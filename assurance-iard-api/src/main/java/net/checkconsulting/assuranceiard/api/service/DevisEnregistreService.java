package net.checkconsulting.assuranceiard.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DevisEnregistreResponse;
import net.checkconsulting.assuranceiard.api.dto.DevisRequest;
import net.checkconsulting.assuranceiard.api.dto.DonneesDevisPdf;
import net.checkconsulting.assuranceiard.api.dto.RepriseDevisResponse;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.mapper.DevisEnregistreMapper;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DevisEnregistre;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-12. RG1 : 30 jours de validité depuis la création du devis (fin de l'étape 2), prix garanti
 * pendant cette période. Le jour 30 est encore valide. Un devis expiré repris repart pour 30 jours.
 */
@Service
@RequiredArgsConstructor
public class DevisEnregistreService {

    static final int DUREE_VALIDITE_JOURS = 30;
    private static final BigDecimal MOIS_PAR_AN = BigDecimal.valueOf(12);
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final DevisEnregistreRepository repository;
    private final RecommandationDevisRepository recommandations;
    private final SessionService sessionService;
    private final TarifDevisService tarifDevis;
    private final DevisEnregistreMapper mapper;
    private final DevisPdfService pdf;
    private final Clock horloge;

    /** Réenregistrer le même devis met sa ligne à jour, sans doublon ni validité prolongée. */
    @Transactional
    public DevisEnregistreResponse enregistrer(String authorization, DevisRequest demande) {
        Compte compte = sessionService.compteConnecte(authorization);
        RecommandationDevis recommandation = recommandation(demande.reference());
        DevisEnregistre devis = repository.findByCompteAndReference(compte, demande.reference())
                .map(existant -> estExpire(existant) ? renouveler(existant) : existant)
                .orElseGet(() -> nouveau(compte, recommandation));

        BigDecimal prix = garantiPour(devis, demande).orElseGet(() -> baremeDe(demande, recommandation));
        mapper.copierLogement(demande.logement(), devis);
        devis.setFormule(demande.formule());
        devis.setOptions(demande.options().stream().distinct().toList());
        devis.setFranchise(demande.franchise());
        devis.setPrixMensuel(prix);
        devis.setDateDernierContact(horloge.instant());
        return versReponse(repository.save(devis));
    }

    @Transactional
    public List<DevisEnregistreResponse> lister(String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        return repository.findByCompteOrderByDateCreationDesc(compte).stream().map(this::versReponse).toList();
    }

    @Transactional
    public RepriseDevisResponse reprendre(String authorization, UUID reference) {
        DevisEnregistre devis = duClient(authorization, reference);
        RecommandationDevis recommandation = recommandation(reference);

        BigDecimal ancienPrix = null;
        if (estExpire(devis)) {
            ancienPrix = devis.getPrixMensuel();
            renouveler(devis);
            devis.setPrixMensuel(tarifDevis.prixMensuel(mapper.versLogement(devis), recommandation,
                    devis.getFormule(), devis.getOptions(), devis.getFranchise()));
        }
        devis.setDateDernierContact(horloge.instant());
        return new RepriseDevisResponse(reference, mapper.versLogement(devis), mapper.versBesoins(recommandation),
                devis.getFormule(), devis.getOptions(), devis.getFranchise(), devis.getPrixMensuel(), ancienPrix,
                devis.getDateFinValidite());
    }

    /** RGPD : le client peut effacer son devis à tout moment. */
    @Transactional
    public void supprimer(String authorization, UUID reference) {
        repository.delete(duClient(authorization, reference));
    }

    /** RG2 : sans compte. La date et le prix ne viennent jamais du navigateur. */
    @Transactional(readOnly = true)
    public byte[] telecharger(DevisRequest demande) {
        RecommandationDevis recommandation = recommandation(demande.reference());
        Optional<DevisEnregistre> enregistre = repository
                .findFirstByReferenceOrderByDateCreationDesc(demande.reference())
                .filter(devis -> !estExpire(devis));
        BigDecimal prix = enregistre.flatMap(devis -> garantiPour(devis, demande)).orElseGet(() -> baremeDe(demande, recommandation));
        LocalDate finValidite = enregistre.map(DevisEnregistre::getDateFinValidite)
                .orElseGet(() -> finValidite(debutValidite(recommandation)));
        return pdf.generer(new DonneesDevisPdf(demande.logement(), demande.formule(),
                demande.options().stream().distinct().toList(), demande.franchise(), prix, annuel(prix),
                finValidite));
    }

    private DevisEnregistre duClient(String authorization, UUID reference) {
        Compte compte = sessionService.compteConnecte(authorization);
        return repository.findByCompteAndReference(compte, reference).orElseThrow(DevisInconnuException::new);
    }

    private RecommandationDevis recommandation(UUID reference) {
        return recommandations.findByReference(reference).orElseThrow(DevisInconnuException::new);
    }

    private DevisEnregistre nouveau(Compte compte, RecommandationDevis recommandation) {
        DevisEnregistre devis = new DevisEnregistre();
        devis.setCompte(compte);
        devis.setReference(recommandation.getReference());
        demarrerValidite(devis, debutValidite(recommandation));
        return devis;
    }

    private DevisEnregistre renouveler(DevisEnregistre devis) {
        demarrerValidite(devis, horloge.instant());
        devis.setPrixMensuel(null);
        return devis;
    }

    private void demarrerValidite(DevisEnregistre devis, Instant debut) {
        devis.setDateCreation(debut);
        devis.setDateFinValidite(finValidite(debut));
    }

    /** Une recommandation déjà trop ancienne ne doit pas produire un devis né expiré. */
    private Instant debutValidite(RecommandationDevis recommandation) {
        Instant creation = recommandation.getDateCreation();
        return aujourdhui().isAfter(finValidite(creation)) ? horloge.instant() : creation;
    }

    private LocalDate finValidite(Instant debut) {
        return LocalDate.ofInstant(debut, FRANCE).plusDays(DUREE_VALIDITE_JOURS);
    }

    private boolean estExpire(DevisEnregistre devis) {
        return aujourdhui().isAfter(devis.getDateFinValidite());
    }

    private LocalDate aujourdhui() {
        return LocalDate.ofInstant(horloge.instant(), FRANCE);
    }

    /** Le prix enregistré ne vaut que pour les mêmes choix. */
    private Optional<BigDecimal> garantiPour(DevisEnregistre devis, DevisRequest demande) {
        boolean memesChoix = devis.getPrixMensuel() != null
                && String.valueOf(devis.getSurface()).equals(demande.logement().surface())
                && demande.logement().typeLogement().equals(devis.getTypeLogement())
                && demande.formule().equals(devis.getFormule())
                && demande.franchise().equals(devis.getFranchise())
                && new HashSet<>(demande.options()).equals(new HashSet<>(devis.getOptions()));
        return memesChoix ? Optional.of(devis.getPrixMensuel()) : Optional.empty();
    }

    private BigDecimal baremeDe(DevisRequest demande, RecommandationDevis recommandation) {
        return tarifDevis.prixMensuel(demande.logement(), recommandation,
                demande.formule(), demande.options(), demande.franchise());
    }

    private BigDecimal annuel(BigDecimal mensuel) {
        return mensuel.multiply(MOIS_PAR_AN).setScale(2, RoundingMode.HALF_UP);
    }

    private DevisEnregistreResponse versReponse(DevisEnregistre devis) {
        return new DevisEnregistreResponse(devis.getReference(), devis.getFormule(), devis.getTypeLogement(),
                devis.getSurface(), devis.getAdresse(), devis.getCodePostal(), devis.getVille(),
                devis.getPrixMensuel(), annuel(devis.getPrixMensuel()), devis.getDateFinValidite(),
                estExpire(devis));
    }
}
