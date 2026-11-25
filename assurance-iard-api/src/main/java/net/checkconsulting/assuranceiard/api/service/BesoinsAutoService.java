package net.checkconsulting.assuranceiard.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.BesoinsAutoReponse;
import net.checkconsulting.assuranceiard.api.dto.BesoinsAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifAutoResponse;
import net.checkconsulting.assuranceiard.api.exception.VehiculeIntrouvableException;
import net.checkconsulting.assuranceiard.api.model.Conducteur;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.Vehicule;
import net.checkconsulting.assuranceiard.api.repository.ConducteurRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.VehiculeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-29 : enregistre les besoins, applique RG1 et retourne la formule conseillee. */
@Slf4j
@Service
@RequiredArgsConstructor
public class BesoinsAutoService {

    private static final int SEUIL_RECENTS_ANS = 5;
    private static final int SEUIL_ANCIENS_ANS = 10;
    private static final int SEUIL_VALEUR_HAUTE = 15_000;
    private static final int SEUIL_VALEUR_BASSE = 3_000;

    private final DevisAutoRepository devisAutos;
    private final VehiculeRepository vehicules;
    private final ConducteurRepository conducteurs;
    private final MoteurTarifaireClient moteurTarifaire;

    @Transactional
    public BesoinsAutoReponse enregistrerBesoins(Long devisAutoId, BesoinsAutoRequest requete) {
        DevisAuto devisAuto = devisAutos.findById(devisAutoId)
                .orElseThrow(() -> new VehiculeIntrouvableException("Devis auto introuvable : id=" + devisAutoId));
        Vehicule vehicule = vehicules.findById(devisAuto.getVehiculeId())
                .orElseThrow(() -> new VehiculeIntrouvableException("Vehicule introuvable pour le devis " + devisAutoId));
        Conducteur conducteur = conducteurs.findById(devisAuto.getConducteurId())
                .orElseThrow(() -> new VehiculeIntrouvableException("Conducteur introuvable pour le devis " + devisAutoId));

        String formule = determinerFormule(requete, vehicule);
        List<String> raisons = genererRaisons(formule, requete, vehicule);
        BigDecimal prix = calculerPrix(vehicule, conducteur, formule);

        devisAuto.setValeurVehicule(requete.valeurVehicule());
        devisAuto.setVehiculeIndispensable(requete.vehiculeIndispensable());
        devisAuto.setAssistance0km(requete.assistance0km());
        devisAuto.setProtectionConducteur(requete.protectionConducteur());
        devisAuto.setBrisGlace(requete.brisGlace());
        devisAuto.setAccompagnementLitige(requete.accompagnementLitige());
        devisAuto.setPriorite(requete.priorite());
        devisAuto.setFormuleConseillee(formule);
        devisAuto.setRaisonsFormule(String.join("|", raisons));
        devisAuto.setPrixMensuelEstime(prix);
        devisAutos.save(devisAuto);

        log.info("layer=service Besoins auto enregistres : devisId={}, formule={}", devisAutoId, formule);
        return new BesoinsAutoReponse(formule, raisons, prix);
    }

    /**
     * RG1 – determine la formule conseillee :
     * a. TOUS_RISQUES : vehicule recent (< 5 ans), valeur > 15 000 EUR, LOA/LLD, ou priorite PROTECTION_LARGE.
     * b. TIERS        : priorite PRIX ET (vehicule ancien > 10 ans OU valeur < 3 000 EUR).
     * c. INTERMEDIAIRE : tous les autres cas.
     */
    String determinerFormule(BesoinsAutoRequest r, Vehicule vehicule) {
        int age = ageVehicule(vehicule);
        boolean loaOuLld = "LOA".equals(vehicule.getModeAchat()) || "LLD".equals(vehicule.getModeAchat());
        // a. TOUS_RISQUES
        if (age < SEUIL_RECENTS_ANS
                || (r.valeurVehicule() != null && r.valeurVehicule() > SEUIL_VALEUR_HAUTE)
                || loaOuLld
                || "PROTECTION_LARGE".equals(r.priorite())) {
            return "TOUS_RISQUES";
        }
        // b. TIERS
        if ("PRIX".equals(r.priorite())
                && (age > SEUIL_ANCIENS_ANS
                        || (r.valeurVehicule() != null && r.valeurVehicule() < SEUIL_VALEUR_BASSE))) {
            return "TIERS";
        }
        // c. INTERMEDIAIRE
        return "INTERMEDIAIRE";
    }

    List<String> genererRaisons(String formule, BesoinsAutoRequest r, Vehicule vehicule) {
        List<String> raisons = new ArrayList<>();
        int age = ageVehicule(vehicule);
        boolean loaOuLld = "LOA".equals(vehicule.getModeAchat()) || "LLD".equals(vehicule.getModeAchat());
        switch (formule) {
            case "TOUS_RISQUES" -> {
                if (age < SEUIL_RECENTS_ANS) raisons.add("Votre vehicule a moins de 5 ans");
                if (r.valeurVehicule() != null && r.valeurVehicule() > SEUIL_VALEUR_HAUTE) raisons.add("Votre vehicule vaut plus de 15 000 €");
                if (loaOuLld) raisons.add("Votre vehicule est en LOA ou LLD");
                if ("PROTECTION_LARGE".equals(r.priorite())) raisons.add("Vous recherchez la protection la plus large");
            }
            case "TIERS" -> {
                raisons.add("Vous privilegiez le prix");
                if (age > SEUIL_ANCIENS_ANS) raisons.add("Votre vehicule a plus de 10 ans");
                if (r.valeurVehicule() != null && r.valeurVehicule() < SEUIL_VALEUR_BASSE) raisons.add("Votre vehicule vaut moins de 3 000 €");
            }
            default -> raisons.add("Vous recherchez l'équilibre prix / protection");
        }
        return raisons;
    }

    private int ageVehicule(Vehicule vehicule) {
        if (vehicule.getDateMiseCirculation() == null) return Integer.MAX_VALUE;
        return (int) ChronoUnit.YEARS.between(vehicule.getDateMiseCirculation(), LocalDate.now());
    }

    private BigDecimal calculerPrix(Vehicule vehicule, Conducteur conducteur, String formule) {
        int anneePermis = conducteur.getDatePermis() != null
                ? conducteur.getDatePermis().getYear()
                : (LocalDate.now().getYear() - 10);
        BigDecimal bm = conducteur.getCoefficientBonusMalus() != null
                ? conducteur.getCoefficientBonusMalus()
                : BigDecimal.ONE;
        int km = vehicule.getKmAnnuel() != null ? vehicule.getKmAnnuel() : 10000;
        BigDecimal valeur = vehicule.getValeurEstimee() != null
                ? vehicule.getValeurEstimee()
                : BigDecimal.valueOf(10000);
        try {
            TarifAutoResponse tarif = moteurTarifaire.calculerAuto(
                    new MoteurTarifaireClient.DemandeMoteurAuto(
                            anneePermis, bm, km,
                            vehicule.getUsage(), vehicule.getStationnement(),
                            valeur, formule, null));
            if (!tarif.tarifable() || tarif.prix() == null || tarif.prix().isEmpty()) {
                return null;
            }
            return tarif.prix().stream()
                    .filter(p -> formule.equals(p.formule()))
                    .findFirst()
                    .map(TarifAutoResponse.PrixFormule::mensuel)
                    .orElse(tarif.prix().get(0).mensuel());
        } catch (Exception e) {
            log.warn("Tarif indisponible pour devis auto : {}", e.getMessage());
            return null;
        }
    }
}