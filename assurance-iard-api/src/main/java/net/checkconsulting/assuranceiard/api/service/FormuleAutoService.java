package net.checkconsulting.assuranceiard.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.FormuleAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.FormuleAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.PrixFormuleAutoResponse;
import net.checkconsulting.assuranceiard.api.dto.PrixFormuleAutoResponse.PrixFormule;
import net.checkconsulting.assuranceiard.api.dto.PrixFormuleAutoResponse.PrixOption;
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

/** US-29 étape 3 : prix des 3 formules et enregistrement de la formule choisie (deployed). */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormuleAutoService {

    private static final int VALIDITE_JOURS = 30;

    /** Franchise par code moteur (TIERS absent → getOrDefault retourne null = « sans objet »). */
    private static final Map<String, BigDecimal> FRANCHISE_PAR_FORMULE = Map.of(
            "INTER", new BigDecimal("400"),
            "TR", new BigDecimal("500"));

    private final DevisAutoRepository devisAutos;
    private final VehiculeRepository vehicules;
    private final ConducteurRepository conducteurs;
    private final MoteurTarifaireClient moteurTarifaire;

    /** Renvoie les prix des 3 formules (sans option) et les prix de chaque option disponible. */
    public PrixFormuleAutoResponse obtenirPrix(Long devisAutoId) {
        DevisAuto devisAuto = chargerDevis(devisAutoId);
        Vehicule vehicule = chargerVehicule(devisAuto);
        Conducteur conducteur = chargerConducteur(devisAuto);

        TarifAutoResponse tarif = appelerMoteur(vehicule, conducteur, null, null);

        List<PrixFormule> prix = tarif.prix() == null ? List.of() : tarif.prix().stream()
                .map(p -> new PrixFormule(p.formule(), p.mensuel(), p.annuel(),
                        FRANCHISE_PAR_FORMULE.getOrDefault(p.formule(), null)))
                .collect(Collectors.toList());

        List<PrixOption> options = tarif.prixOptions() == null ? List.of() : tarif.prixOptions().stream()
                .map(o -> new PrixOption(o.option(), o.prixMensuel()))
                .collect(Collectors.toList());

        log.info("layer=service Prix formules auto obtenus : devisId={}, nbFormules={}, nbOptions={}",
                devisAutoId, prix.size(), options.size());
        return new PrixFormuleAutoResponse(tarif.idTarif(), tarif.versionGrille(), tarif.dateValidite(), prix, options);
    }

    /** Enregistre la formule et les options choisies, recalcule le prix final via le moteur. */
    @Transactional
    public FormuleAutoResponse choisirFormule(Long devisAutoId, FormuleAutoRequest requete) {
        DevisAuto devisAuto = chargerDevis(devisAutoId);
        Vehicule vehicule = chargerVehicule(devisAuto);
        Conducteur conducteur = chargerConducteur(devisAuto);

        BigDecimal prixMensuel = requete.prixMensuelTtc();
        BigDecimal prixAnnuel = requete.prixAnnuelTtc();

        // Recalcul via moteur si formule + options fournis
        List<String> options = requete.optionsChoisies() == null ? List.of() : requete.optionsChoisies();
        try {
            TarifAutoResponse tarif = appelerMoteur(vehicule, conducteur, requete.formuleChoisie(), options);
            if (tarif.tarifable() && tarif.detail() != null) {
                prixMensuel = tarif.detail().totalMensuel();
                prixAnnuel = tarif.detail().totalAnnuel();
            }
        } catch (Exception e) {
            log.warn("layer=service Recalcul moteur indisponible, utilisation du prix fourni : {}", e.getMessage());
        }

        devisAuto.setFormuleChoisie(requete.formuleChoisie());
        devisAuto.setOptionsChoisies(options.isEmpty() ? null : String.join(",", options));
        devisAuto.setChoixDifferentDuConseil(Boolean.TRUE.equals(requete.choixDifferentDuConseil()));
        if (Boolean.TRUE.equals(requete.choixDifferentDuConseil())) {
            devisAuto.setDateConfirmationChoixDifferent(java.time.Instant.now());
        }
        devisAuto.setPrixMensuelTtc(prixMensuel);
        devisAuto.setPrixAnnuelTtc(prixAnnuel);
        devisAuto.setVersionGrille(requete.versionGrille());
        devisAuto.setDateFinValidite(LocalDate.now().plusDays(VALIDITE_JOURS));
        devisAutos.save(devisAuto);

        log.info("layer=service Formule auto enregistrée : devisId={}, formule={}, options={}",
                devisAutoId, requete.formuleChoisie(), options);
        return new FormuleAutoResponse(devisAuto.getFormuleChoisie(),
                options.isEmpty() ? List.of() : options,
                devisAuto.getPrixMensuelTtc(), devisAuto.getPrixAnnuelTtc(), devisAuto.getDateFinValidite());
    }

    private TarifAutoResponse appelerMoteur(Vehicule vehicule, Conducteur conducteur,
            String formule, List<String> options) {
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
        return moteurTarifaire.calculerAuto(new MoteurTarifaireClient.DemandeMoteurAuto(
                anneePermis, bm, km, vehicule.getUsage(), vehicule.getStationnement(), valeur, formule, options));
    }

    private DevisAuto chargerDevis(Long id) {
        return devisAutos.findById(id)
                .orElseThrow(() -> new VehiculeIntrouvableException("Devis auto introuvable : id=" + id));
    }

    private Vehicule chargerVehicule(DevisAuto d) {
        return vehicules.findById(d.getVehiculeId())
                .orElseThrow(() -> new VehiculeIntrouvableException("Véhicule introuvable pour le devis " + d.getId()));
    }

    private Conducteur chargerConducteur(DevisAuto d) {
        return conducteurs.findById(d.getConducteurId())
                .orElseThrow(() -> new VehiculeIntrouvableException("Conducteur introuvable pour le devis " + d.getId()));
    }
}
