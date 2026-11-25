package net.checkconsulting.assuranceiard.api.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.CriteresEligibiliteResponse;
import net.checkconsulting.assuranceiard.api.dto.DevisAutoCreationRequest;
import net.checkconsulting.assuranceiard.api.dto.DevisAutoCreationResponse;
import net.checkconsulting.assuranceiard.api.exception.ProfilBloqueException;
import net.checkconsulting.assuranceiard.api.exception.VehiculeIntrouvableException;
import net.checkconsulting.assuranceiard.api.model.Conducteur;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.RefVehicule;
import net.checkconsulting.assuranceiard.api.model.Vehicule;
import net.checkconsulting.assuranceiard.api.repository.ConducteurRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.RefVehiculeRepository;
import net.checkconsulting.assuranceiard.api.repository.VehiculeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-28 / US-59 : crée un devis auto (véhicule + conducteur + devis_auto) après validation du profil. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DevisAutoCreationService {

    private static final DateTimeFormatter FORMAT_SAISIE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EligibiliteAutoService eligibiliteAutoService;
    private final RefVehiculeRepository refVehicules;
    private final VehiculeRepository vehicules;
    private final ConducteurRepository conducteurs;
    private final DevisAutoRepository devisAutos;

    @Transactional
    public DevisAutoCreationResponse creer(DevisAutoCreationRequest demande) {
        CriteresEligibiliteResponse criteres = eligibiliteAutoService.criteres();
        verifierEligibilite(demande, criteres);

        RefVehicule ref = resoudreVehiculeRef(demande);

        Vehicule vehicule = new Vehicule();
        vehicule.setMarque(ref.getMarque());
        vehicule.setModele(ref.getModele());
        vehicule.setVersion(ref.getVersion());
        vehicule.setEnergie(ref.getEnergie());
        vehicule.setNonImmatricule(Boolean.TRUE.equals(demande.nonImmatricule()));
        vehicule.setUsage(demande.usage());
        vehicule.setStationnement(demande.stationnement());
        vehicule.setKmAnnuel(demande.kmAnnuels());
        vehicule.setSource(demande.refVehiculeId() != null ? "MARQUE" : "PLAQUE");
        vehicule.setValeurEstimee(ref.getValeurNeuve());
        if (ref.getAnnee() != null) {
            vehicule.setDateMiseCirculation(LocalDate.of(ref.getAnnee(), 1, 1));
        }
        if (demande.titulaireCarteGrise() != null) {
            vehicule.setTitulaireCarteGrise(demande.titulaireCarteGrise());
        }
        if (demande.modeAchat() != null) {
            vehicule.setModeAchat(demande.modeAchat());
        }
        if (demande.dateAcquisition() != null) {
            vehicule.setDateAcquisition(parseDate(demande.dateAcquisition()));
        }
        vehicules.save(vehicule);

        LocalDate dateNaissance = parseDate(demande.dateNaissance());
        LocalDate datePermis = parseDate(demande.datePermis());

        Conducteur conducteur = new Conducteur();
        conducteur.setConducteurPrincipal(demande.conducteurPrincipal());
        conducteur.setDateNaissance(dateNaissance);
        conducteur.setDatePermis(datePermis);
        conducteur.setCoefficientBonusMalus(demande.bonusMalus());
        conducteur.setAnneesAssurance(demande.anneesAssurance());
        conducteur.setConduiteAccompagnee(Boolean.TRUE.equals(demande.conduiteAccompagnee()));
        conducteur.setNbSinistres3Ans(demande.nbSinistresResponsables());
        conducteur.setNbSinistresNonResponsables3Ans(demande.nbSinistresNonResponsables());
        conducteur.setResiliationAssureur(demande.resiliationAssureur());
        conducteur.setSuspensionPermis(Boolean.TRUE.equals(demande.suspensionAnnulation()));
        conducteur.setCondamnationAlcoolStupefiants(Boolean.TRUE.equals(demande.condamnationAlcool()));
        conducteur.setEligibleEnLigne(true);
        conducteurs.save(conducteur);

        DevisAuto devisAuto = new DevisAuto();
        devisAuto.setVehiculeId(vehicule.getId());
        devisAuto.setConducteurId(conducteur.getId());
        devisAuto.setAssureVehicule(demande.assureVehicule());
        devisAuto.setAncienAssureur(demande.ancienAssureur());
        devisAuto.setAncienNumeroContrat(demande.ancienNumeroContrat());
        devisAutos.save(devisAuto);

        log.info("Devis auto créé : id={}, vehiculeId={}, conducteurId={}",
                devisAuto.getId(), vehicule.getId(), conducteur.getId());

        return new DevisAutoCreationResponse(devisAuto.getId(), "EN_COURS", true);
    }

    void verifierEligibilite(DevisAutoCreationRequest d, CriteresEligibiliteResponse criteres) {
        // Bonus-malus dépasse le plafond
        if (d.bonusMalus() != null
                && d.bonusMalus().compareTo(criteres.bonusMalusMaxLigne()) > 0) {
            throw new ProfilBloqueException("BONUS_MALUS",
                    "Coefficient bonus-malus (" + d.bonusMalus() + ") dépasse le plafond accepté ("
                            + criteres.bonusMalusMaxLigne() + ").");
        }
        // Résiliation bloquante
        if (criteres.resiliationBloquante() != null
                && criteres.resiliationBloquante().equals(d.resiliationAssureur())) {
            throw new ProfilBloqueException("RESILIATION",
                    "Une résiliation pour " + d.resiliationAssureur() + " empêche la souscription en ligne.");
        }
        // Suspension ou annulation du permis
        if (Boolean.TRUE.equals(d.suspensionAnnulation())) {
            throw new ProfilBloqueException("SUSPENSION_PERMIS",
                    "Une suspension ou annulation du permis empêche la souscription en ligne.");
        }
        // Condamnation alcool ou stupéfiants
        if (Boolean.TRUE.equals(d.condamnationAlcool())) {
            throw new ProfilBloqueException("CONDAMNATION",
                    "Une condamnation pour alcool ou stupéfiants empêche la souscription en ligne.");
        }
        // Profil novice avec sinistre responsable
        if (isNovice(d) && d.nbSinistresResponsables() != null && d.nbSinistresResponsables() > 0) {
            throw new ProfilBloqueException("NOVICE_SINISTRE",
                    "Un profil novice avec sinistre responsable ne peut pas souscrire en ligne.");
        }
        // Titulaire de la carte grise autre que le souscripteur ou sa famille
        if ("AUTRE_PERSONNE".equals(d.titulaireCarteGrise())) {
            throw new ProfilBloqueException("TITULAIRE_CARTE_GRISE",
                    "La carte grise au nom d'une autre personne empêche la souscription en ligne.");
        }
    }

    private boolean isNovice(DevisAutoCreationRequest d) {
        if (d.anneesAssurance() != null && d.anneesAssurance() == 0) return true;
        LocalDate datePermis = parseDate(d.datePermis());
        if (datePermis != null) {
            return ChronoUnit.YEARS.between(datePermis, LocalDate.now()) < 3;
        }
        return false;
    }

    private RefVehicule resoudreVehiculeRef(DevisAutoCreationRequest d) {
        if (d.refVehiculeId() != null) {
            return refVehicules.findById(d.refVehiculeId())
                    .orElseThrow(() -> new VehiculeIntrouvableException(
                            "Véhicule introuvable (id=" + d.refVehiculeId() + ")."));
        }
        throw new VehiculeIntrouvableException("refVehiculeId obligatoire.");
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s, FORMAT_SAISIE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
