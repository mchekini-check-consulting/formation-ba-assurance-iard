package net.checkconsulting.assuranceiard.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.ContratResponse;
import net.checkconsulting.assuranceiard.api.dto.GenerationContratRequest;
import net.checkconsulting.assuranceiard.api.dto.SignatureContratRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import net.checkconsulting.assuranceiard.api.exception.CodeSignatureIncorrectException;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.MandatNonSigneException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.model.TarifDevis;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.MandatSepaRepository;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import net.checkconsulting.assuranceiard.api.repository.TarifDevisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-15 : génère le contrat à l'issue de la souscription et permet de le relire. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContratService {

    static final String CODE_SIGNATURE_DEV = "123456";
    private static final String STATUT_EN_ATTENTE = "EN_ATTENTE_DE_SIGNATURE";
    private static final String STATUT_SIGNE = "SIGNE";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final SessionService sessionService;
    private final DossierSouscriptionRepository dossierRepository;
    private final ContratRepository contratRepository;
    private final MandatSepaRepository mandatRepository;
    private final RecommandationDevisRepository recommandationRepository;
    private final TarifDevisService tarifDevisService;
    private final TarifDevisRepository tarifDevisRepository;
    private final Clock horloge;

    /** Idempotent : renvoie le contrat existant si déjà généré pour ce dossier. */
    @Transactional
    public ContratResponse generer(String authorization, GenerationContratRequest demande) {
        DossierSouscription dossier = dossierDuClient(authorization, demande.referenceDevis());

        return contratRepository.findByDossier(dossier)
                .map(existant -> construireReponse(existant, dossier))
                .orElseGet(() -> creerEtRepondre(dossier, demande));
    }

    @Transactional(readOnly = true)
    public ContratResponse obtenir(String authorization, UUID referenceDevis) {
        DossierSouscription dossier = dossierDuClient(authorization, referenceDevis);
        Contrat contrat = contratRepository.findByDossier(dossier)
                .orElseThrow(DevisInconnuException::new);
        return construireReponse(contrat, dossier);
    }

    /** Prototype : code 123456 accepté, tout autre code est refusé. */
    @Transactional
    public ContratResponse signer(String authorization, UUID referenceDevis, SignatureContratRequest demande) {
        DossierSouscription dossier = dossierDuClient(authorization, referenceDevis);
        if (!CODE_SIGNATURE_DEV.equals(demande.code())) {
            throw new CodeSignatureIncorrectException();
        }
        Contrat contrat = contratRepository.findByDossier(dossier)
                .orElseThrow(DevisInconnuException::new);
        contrat.setStatut(STATUT_SIGNE);
        contrat = contratRepository.save(contrat);
        return construireReponse(contrat, dossier);
    }

    private ContratResponse creerEtRepondre(DossierSouscription dossier, GenerationContratRequest demande) {
        MandatSepa mandat = mandatRepository.findByDossier(dossier)
                .filter(m -> m.getDateSignature() != null)
                .orElseThrow(MandatNonSigneException::new);

        TarifResponse.Detail tarif = tarifDevisService.cotisationDuContrat(demande);
        BigDecimal mensuelle = tarif.totalMensuel().setScale(2, RoundingMode.HALF_UP);
        BigDecimal annuelle = tarif.totalAnnuel().setScale(2, RoundingMode.HALF_UP);

        Contrat contrat = new Contrat();
        contrat.setDossier(dossier);
        contrat.setNumero(genererNumero());
        contrat.setStatut(STATUT_EN_ATTENTE);
        contrat.setAdresseLogement(demande.adresseLogement());
        contrat.setCodePostal(demande.codePostal());
        contrat.setVille(demande.ville());
        contrat.setTypeLogement(demande.typeLogement());
        contrat.setSurface(demande.surface());
        contrat.setFranchise(demande.franchise());
        contrat.setCotisationMensuelle(mensuelle);
        contrat.setCotisationAnnuelle(annuelle);
        contrat.setResiliationHamon(demande.resiliationHamon());
        contrat.setDateEffet(mandat.getDateDebut());
        contrat.setDateCreation(horloge.instant());

        contrat = contratRepository.save(contrat);
        log.info("Contrat généré au prix du moteur : numero={}", contrat.getNumero());
        return construireReponse(contrat, dossier);
    }

    private ContratResponse construireReponse(Contrat contrat, DossierSouscription dossier) {
        Compte compte = dossier.getCompte();
        RecommandationDevis recommandation = recommandationRepository
                .findByReference(dossier.getReferenceDevis())
                .orElseThrow(DevisInconnuException::new);
        MandatSepa mandat = mandatRepository.findByDossier(dossier)
                .orElseThrow(DevisInconnuException::new);
        Optional<TarifDevis> tarif = tarifDevisRepository.findByReferenceDevis(dossier.getReferenceDevis());
        String formule = tarif.map(TarifDevis::getFormule).orElse(recommandation.getFormule());
        List<String> options = tarif.map(TarifDevis::getOptions).orElse(recommandation.getOptions());

        return new ContratResponse(
                contrat.getNumero(),
                contrat.getStatut(),
                compte.getCivilite(),
                compte.getPrenom(),
                compte.getNom(),
                compte.getTelephone(),
                contrat.getAdresseLogement(),
                contrat.getCodePostal(),
                contrat.getVille(),
                contrat.getTypeLogement(),
                contrat.getSurface(),
                formule,
                options,
                contrat.getFranchise(),
                contrat.getCotisationMensuelle(),
                contrat.getCotisationAnnuelle(),
                mandat.getJourPrelevement(),
                mandat.getIbanFin(),
                mandat.getIbanDebut(),
                mandat.getRum(),
                mandat.getDatePremierPrelevement(),
                contrat.getDateEffet(),
                contrat.getResiliationHamon(),
                LocalDate.ofInstant(contrat.getDateCreation(), FRANCE));
    }

    private DossierSouscription dossierDuClient(String authorization, UUID referenceDevis) {
        Compte compte = sessionService.compteConnecte(authorization);
        return dossierRepository.findByCompteAndReferenceDevis(compte, referenceDevis)
                .orElseThrow(DevisInconnuException::new);
    }

    // RG1 : AE-année-6chiffres, ex. AE-2026-000001
    private String genererNumero() {
        int annee = LocalDate.ofInstant(horloge.instant(), FRANCE).getYear();
        String prefixe = "AE-" + annee;
        long numero = contratRepository.prochainNumeroAnnee(prefixe);
        return prefixe + "-" + String.format("%06d", numero);
    }
}
