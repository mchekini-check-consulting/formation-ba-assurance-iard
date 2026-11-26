package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.InformationsAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.InformationsAutoResponse;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.exception.VehiculeIntrouvableException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Conducteur;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.SouscripteurAuto;
import net.checkconsulting.assuranceiard.api.repository.ConducteurRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.SouscripteurAutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-31 étape 4 : pré-remplissage et enregistrement des informations du souscripteur auto. */
@Slf4j
@Service
@RequiredArgsConstructor
public class InformationsAutoService {

    private static final int AGE_MAJORITE = 18;
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final SessionService sessionService;
    private final DevisAutoRepository devisAutos;
    private final ConducteurRepository conducteurs;
    private final SouscripteurAutoRepository souscripteurs;
    private final Clock horloge;

    /** Renvoie les données de pré-remplissage : souscripteur déjà enregistré, ou données du compte + date de naissance du conducteur. */
    @Transactional
    public InformationsAutoResponse lire(Long devisAutoId, String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        SouscripteurAuto existant = souscripteurs.findByDevisAutoId(devisAutoId).orElse(null);
        if (existant != null) {
            log.info("layer=service Pré-remplissage souscripteur auto depuis enregistrement existant : devisId={}", devisAutoId);
            return new InformationsAutoResponse(
                    existant.getCivilite(), existant.getNom(), existant.getPrenom(),
                    existant.getDateNaissance(), existant.getTelephone(),
                    existant.getAdresse(), existant.getCodePostal(), existant.getVille());
        }
        DevisAuto devis = chargerDevis(devisAutoId);
        Conducteur conducteur = conducteurs.findById(devis.getConducteurId())
                .orElse(null);
        LocalDate dateNaissance = conducteur != null ? conducteur.getDateNaissance() : null;
        log.info("layer=service Pré-remplissage souscripteur auto depuis compte : devisId={}", devisAutoId);
        return new InformationsAutoResponse(
                compte.getCivilite(), compte.getNom(), compte.getPrenom(),
                dateNaissance, compte.getTelephone(), null, null, null);
    }

    /** Enregistre les informations du souscripteur et rattache le devis au compte. */
    @Transactional
    public void enregistrer(Long devisAutoId, InformationsAutoRequest demande, String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        verifierMajeur(demande.dateNaissance());
        chargerDevis(devisAutoId);
        SouscripteurAuto souscripteur = souscripteurs.findByDevisAutoId(devisAutoId)
                .orElseGet(SouscripteurAuto::new);
        souscripteur.setDevisAutoId(devisAutoId);
        souscripteur.setCompteId(compte.getId());
        souscripteur.setCivilite(demande.civilite());
        souscripteur.setNom(demande.nom().trim());
        souscripteur.setPrenom(demande.prenom().trim());
        souscripteur.setDateNaissance(demande.dateNaissance());
        souscripteur.setTelephone(demande.telephone());
        souscripteur.setAdresse(demande.adresse().trim());
        souscripteur.setCodePostal(demande.codePostal());
        souscripteur.setVille(demande.ville().trim());
        souscripteurs.save(souscripteur);
        log.info("layer=service Souscripteur auto enregistré : devisId={}, compteId={}", devisAutoId, compte.getId());
    }

    private void verifierMajeur(LocalDate dateNaissance) {
        LocalDate aujourdhui = LocalDate.ofInstant(horloge.instant(), FRANCE);
        if (Period.between(dateNaissance, aujourdhui).getYears() < AGE_MAJORITE) {
            throw new InformationRefuseeException("dateNaissance", "Vous devez avoir au moins 18 ans.");
        }
    }

    private DevisAuto chargerDevis(Long id) {
        return devisAutos.findById(id)
                .orElseThrow(() -> new VehiculeIntrouvableException("Devis auto introuvable : id=" + id));
    }
}
