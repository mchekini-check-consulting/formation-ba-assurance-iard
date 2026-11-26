package net.checkconsulting.assuranceiard.api.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.BesoinsDevis;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.exception.MoteurIndisponibleException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.dto.DemandeTarif;
import net.checkconsulting.assuranceiard.api.dto.ErreurChampMoteur;
import net.checkconsulting.assuranceiard.api.dto.GenerationContratRequest;
import net.checkconsulting.assuranceiard.api.dto.LogementDevis;
import net.checkconsulting.assuranceiard.api.dto.TarifRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.MoteurDonneesInvalidesException;
import net.checkconsulting.assuranceiard.api.exception.MoteurTarifInconnuException;
import net.checkconsulting.assuranceiard.api.exception.TarifDevisIncoherentException;
import net.checkconsulting.assuranceiard.api.mapper.DevisEnregistreMapper;
import net.checkconsulting.assuranceiard.api.mapper.TarifMoteurMapper;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.model.TarifDevis;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import net.checkconsulting.assuranceiard.api.repository.TarifDevisRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TarifDevisService {

    private static final String CHAMP_FORMULE = "formule";
    private static final String CHAMP_OBLIGATOIRE = "Ce champ est obligatoire.";
    /** US-23 : les options habitation de la grille du sprint 1 (codes de l'écran). */
    static final List<String> OPTIONS_HABITATION = List.of("objets-valeur", "assistance-depannage",
            "protection-juridique");
    private static final String PROFIL_STATUT = "locataire";
    private static final String PROFIL_RESIDENCE = "principale";
    private static final String PROFIL_PIECES = "2";
    private static final String PROFIL_SINISTRES = "0";
    private static final String PROFIL_ETAGE = "intermediaire";
    private static final String PROFIL_DEPENDANCES = "aucune";

    private final MoteurTarifaireService moteur;
    private final TarifDevisRepository tarifs;
    private final RecommandationDevisRepository recommandations;
    private final TarifMoteurMapper traduction;
    private final DevisEnregistreMapper devisMapper;
    private final Clock horloge;

    @LogMetier(champs = {"demande.formule", "demande.franchise"})
    public TarifResponse estimer(DemandeTarif demande) {
        log.info("Estimation de tarif demandée au moteur");
        TarifRequest requete = traduction.versMoteur(
                demande.logement(), demande.besoins(), demande.formule(), demande.options(), demande.franchise());
        return traduction.versEcran(moteur.calculer(requete));
    }

    @LogMetier(champs = {"demande.formule", "demande.franchise"})
    public TarifResponse tarifer(UUID referenceDevis, DemandeTarif demande) {
        RecommandationDevis recommandation = recommandations.findByReference(referenceDevis)
                .orElseThrow(DevisInconnuException::new);
        if (demande.formule() == null) {
            throw new MoteurDonneesInvalidesException(List.of(new ErreurChampMoteur(CHAMP_FORMULE, CHAMP_OBLIGATOIRE)));
        }

        BesoinsDevis besoins = devisMapper.versBesoins(recommandation);
        TarifResponse tarif = moteur.calculer(traduction.versMoteur(
                demande.logement(), besoins, demande.formule(), demande.options(), demande.franchise()));
        Optional<TarifDevis> existant = tarifs.findByReferenceDevis(referenceDevis);
        if (tarif.tarifable()) {
            retenir(existant.orElseGet(TarifDevis::new), referenceDevis, demande, tarif);
        } else {
            log.info("Tarif refusé par le moteur : idTarif={}", tarif.idTarif());
            existant.ifPresent(tarifs::delete);
        }
        return traduction.versEcran(tarif);
    }

    public TarifResponse tarifRetenu(UUID referenceDevis) {
        log.info("Relecture du tarif retenu : referenceDevis={}", referenceDevis);
        TarifDevis tarif = tarifs.findByReferenceDevis(referenceDevis).orElseThrow(MoteurTarifInconnuException::new);
        return traduction.versEcran(moteur.reprendre(tarif.getIdTarif()));
    }

    public BigDecimal prixMensuel(
            LogementDevis logement, RecommandationDevis recommandation, String formule, List<String> options, String franchise) {
        log.info("Prix d'un devis demandé au moteur : referenceDevis={}", recommandation.getReference());
        TarifResponse tarif = moteur.calculer(traduction.versMoteur(
                logement, devisMapper.versBesoins(recommandation), formule, options, franchise));
        if (!tarif.tarifable() || tarif.detail() == null) {
            throw new TarifDevisIncoherentException();
        }
        return tarif.detail().totalMensuel();
    }

    /**
     * US-23 : prix mensuel TTC de chaque option habitation (codes de l'écran : objets-valeur...) pour la formule et
     * le logement du contrat, avec la grille du sprint 1. Le contrat ne garde pas tout le profil du devis (statut,
     * pièces, sinistres) : le moteur reçoit un profil neutre (locataire, résidence principale, 2 pièces, aucun
     * sinistre). Sans effet sur le prix des options, fixe dans la grille 2026.1 (hypothèse à valider PO).
     */
    public Map<String, BigDecimal> prixDesOptions(DonneesContrat contrat) {
        DonneesContrat.Logement logement = contrat.logement();
        // Le moteur exige l'étage d'un appartement et les dépendances d'une maison : valeurs neutres (coefficient 1).
        LogementDevis profil = new LogementDevis(PROFIL_STATUT, PROFIL_RESIDENCE, logement.type(), logement.adresse(),
                logement.codePostal(), logement.ville(), String.valueOf(logement.surface()), PROFIL_PIECES, null,
                null, null, PROFIL_ETAGE, PROFIL_DEPENDANCES, null, null, PROFIL_SINISTRES);
        String franchise = contrat.tarif().franchise() == null ? null
                : String.valueOf(contrat.tarif().franchise().intValue());
        TarifResponse tarif;
        try {
            tarif = traduction.versEcran(moteur.calculer(traduction.versMoteur(
                    profil, null, contrat.formule(), OPTIONS_HABITATION, franchise)));
        } catch (MoteurDonneesInvalidesException refus) {
            // Jamais « champs invalides » au client : le contrat est en base, il n'a rien saisi.
            log.warn("Prix des options habitation refusés par le moteur : champs={}",
                    refus.getErreurs().stream().map(ErreurChampMoteur::champ).toList());
            throw new MoteurIndisponibleException();
        }
        if (!tarif.tarifable() || tarif.detail() == null || tarif.detail().options() == null) {
            log.warn("Prix des options habitation indisponibles : idTarif={}", tarif.idTarif());
            throw new MoteurIndisponibleException();
        }
        Map<String, BigDecimal> prix = new LinkedHashMap<>();
        tarif.detail().options().forEach(option -> prix.put(option.option(), option.prixMensuel()));
        return prix;
    }

    public TarifResponse.Detail cotisationDuContrat(GenerationContratRequest contrat) {
        TarifDevis tarif = tarifs.findByReferenceDevis(contrat.referenceDevis())
                .orElseThrow(TarifDevisIncoherentException::new);
        verifierMemeLogement(tarif, contrat);
        log.info("Cotisation du contrat lue auprès du moteur : idTarif={}", tarif.getIdTarif());
        return moteur.reprendre(tarif.getIdTarif()).detail();
    }

    private void retenir(TarifDevis ligne, UUID referenceDevis, DemandeTarif demande, TarifResponse tarif) {
        ligne.setReferenceDevis(referenceDevis);
        ligne.setIdTarif(tarif.idTarif());
        ligne.setCodePostal(demande.logement().codePostal());
        ligne.setTypeLogement(demande.logement().typeLogement());
        ligne.setSurface(new BigDecimal(demande.logement().surface()));
        ligne.setFranchise(demande.franchise() == null ? null : Integer.valueOf(demande.franchise()));
        ligne.setFormule(demande.formule());
        ligne.setOptions(demande.options() == null ? List.of() : demande.options());
        ligne.setDateCreation(horloge.instant());
        tarifs.save(ligne);
        log.info("Tarif retenu pour un devis : idTarif={}", tarif.idTarif());
    }

    private void verifierMemeLogement(TarifDevis tarif, GenerationContratRequest contrat) {
        boolean identique = tarif.getCodePostal().equals(contrat.codePostal())
                && tarif.getTypeLogement().equalsIgnoreCase(contrat.typeLogement())
                && tarif.getSurface().compareTo(BigDecimal.valueOf(contrat.surface())) == 0
                && Objects.equals(tarif.getFranchise(), contrat.franchise());
        if (!identique) {
            log.warn("Contrat refusé : le logement ne correspond pas au tarif idTarif={}", tarif.getIdTarif());
            throw new TarifDevisIncoherentException();
        }
    }
}
