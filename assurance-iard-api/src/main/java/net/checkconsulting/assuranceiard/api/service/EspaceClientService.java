package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.AvenantResponse;
import net.checkconsulting.assuranceiard.api.dto.ContratResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.dto.FicheContratResponse;
import net.checkconsulting.assuranceiard.api.dto.TableauDeBordResponse;
import net.checkconsulting.assuranceiard.api.exception.ContratInconnuException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.DevisEnregistre;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.Droit;
import net.checkconsulting.assuranceiard.api.model.Facture;
import net.checkconsulting.assuranceiard.api.model.Rubrique;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.model.Vehicule;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.FactureRepository;
import net.checkconsulting.assuranceiard.api.repository.PermissionRepository;
import net.checkconsulting.assuranceiard.api.repository.SinistreRepository;
import net.checkconsulting.assuranceiard.api.repository.VehiculeRepository;
import net.checkconsulting.assuranceiard.api.service.avenant.HistoriqueAvenants;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import net.checkconsulting.assuranceiard.api.service.document.GabaritPdf;
import net.checkconsulting.assuranceiard.api.utils.EcheanceAnnuelle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-18 : espace client (tableau de bord, Mes contrats, fiche contrat), en lecture seule. Les contrats sont
 * rattachés au client par contrat.compte_id (habitation et auto). Le contrôle d'accès d'US-04 est fait avant
 * chaque route ; ce service ne lit que les données du compte connecté ou du contrat déjà autorisé.
 */
@Service
@RequiredArgsConstructor
public class EspaceClientService {

    /**
     * AC3 : documents affichés sur la fiche (ceux qui n'existent pas encore en base n'apparaissent pas). US-21 : les
     * conditions particulières n'y sont plus, elles ont leur section « Versions ».
     */
    static final List<TypeDocument> DOCUMENTS_FICHE = List.of(TypeDocument.CONDITIONS_GENERALES, TypeDocument.IPID,
            TypeDocument.FICHE_CONSEIL, TypeDocument.MANDAT_SEPA, TypeDocument.ATTESTATION,
            TypeDocument.MEMO_VEHICULE);
    static final String SINISTRE_DECLARE = "DECLARE";
    private static final String EN_VIGUEUR = "EN_VIGUEUR";
    private static final String AVENANT_ACCEPTE = "ACCEPTE";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final SessionService sessionService;
    private final PermissionRepository permissions;
    private final ContratRepository contrats;
    private final DevisEnregistreRepository devis;
    private final DevisAutoRepository devisAuto;
    private final VehiculeRepository vehicules;
    private final FactureRepository factures;
    private final SinistreRepository sinistres;
    private final DocumentRepository documents;
    private final DonneesContratRepository donneesContrat;
    private final DocumentsGeneresService documentsGeneres;
    private final HistoriqueAvenants historiqueAvenants;
    private final Clock horloge;

    /** US-04, AC1 / US-18 : « Mes contrats », habitation et auto. */
    @LogMetier(champs = {"resultat.taille"})
    @Transactional(readOnly = true)
    public List<ContratResumeResponse> contrats(String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        return contrats.duClient(compte).stream().map(this::resumeAvecAvenants).toList();
    }

    /**
     * AC1, AC6, AC7 : prénom, contrats, devis en cours, 3 dernières factures, sinistres déclarés. Chaque rubrique
     * n'est remplie que si le profil a le droit de la consulter (droits relus à chaque appel, modifiables par
     * l'administrateur depuis US-02).
     */
    @LogMetier
    @Transactional(readOnly = true)
    public TableauDeBordResponse tableauDeBord(String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        List<Contrat> sesContrats = peut(compte, Rubrique.CONTRATS) ? contrats.duClient(compte) : List.of();
        Map<Long, String> numeros = sesContrats.stream()
                .collect(Collectors.toMap(Contrat::getId, Contrat::getNumero));
        boolean devisVisibles = peut(compte, Rubrique.DEVIS);
        boolean sinistresVisibles = peut(compte, Rubrique.SINISTRES);

        return new TableauDeBordResponse(
                compte.getPrenom(),
                compte.getNom(),
                sesContrats.stream().map(this::resume).toList(),
                devisVisibles,
                devisVisibles ? devisEnCours(compte) : List.of(),
                numeros.isEmpty() ? List.of() : dernieresFactures(numeros),
                sinistresVisibles,
                sinistresVisibles && !numeros.isEmpty() ? sinistresDeclares(numeros) : List.of());
    }

    /** AC3 : fiche d'un contrat (le contrôle d'accès a déjà vérifié que le client en est propriétaire). */
    @LogMetier(champs = {"resultat.produit", "resultat.statut"})
    @Transactional(readOnly = true)
    public FicheContratResponse fiche(String numero) {
        Contrat contrat = contrats.parNumero(numero).orElseThrow(ContratInconnuException::new);
        DonneesContrat donnees = donneesContrat.lire(contrat.getId()).orElseThrow(ContratInconnuException::new);
        boolean auto = Contrat.AUTO.equals(contrat.getProduit());
        DonneesContrat.Logement logement = donnees.logement();
        DonneesContrat.Vehicule vehicule = donnees.vehicule();
        DonneesContrat.Paiement paiement = donnees.paiement();
        // US-21, AC2 : toutes les versions des conditions particulières, la plus récente d'abord.
        List<Document> versions = documents.versionsDuContrat(contrat.getId(),
                TypeDocument.CONDITIONS_PARTICULIERES.name());

        return new FicheContratResponse(
                contrat.getNumero(),
                contrat.getDossier() == null ? null : contrat.getDossier().getReferenceDevis(),
                contrat.getProduit(),
                contrat.getStatut(),
                contrat.getFormule(),
                auto ? null : new FicheContratResponse.Logement(logement.type(), logement.surface(),
                        logement.adresse(), logement.codePostal(), logement.ville()),
                auto ? new FicheContratResponse.Vehicule(vehicule.marque(), vehicule.modele(), vehicule.version(),
                        vehicule.immatriculation()) : null,
                contrat.getFranchise(),
                donnees.options().stream()
                        .map(option -> new FicheContratResponse.Garantie(option.code(), option.prixMensuelTtc()))
                        .toList(),
                contrat.getCotisationMensuelle(),
                contrat.getCotisationAnnuelle(),
                contrat.getDateEffet(),
                echeance(contrat),
                contrat.getDateFin(),
                new FicheContratResponse.Paiement(paiement.frequence(), paiement.jourPrelevement(),
                        paiement.ibanMasque(), paiement.rum()),
                documentsDeLaFiche(contrat),
                versions.stream().map(this::version).toList(),
                versions.stream().filter(document -> EN_VIGUEUR.equals(document.getStatut())).findFirst()
                        .map(documentsGeneres::contenuAJour).orElse(false),
                historiqueAvenants.duContrat(contrat.getId()));
    }

    /** US-21, AC2 : une version des conditions particulières, avec la date de production de son PDF. */
    private FicheContratResponse.Version version(Document document) {
        return new FicheContratResponse.Version(document.getId(), document.getVersion(), document.getStatut(),
                document.getDateCreation(), document.getMotif(),
                documentsGeneres.production(document).map(GabaritPdf.Production::produitLe).orElse(null),
                Boolean.TRUE.equals(document.getStocke()));
    }

    private boolean peut(Compte compte, Rubrique rubrique) {
        return permissions.existsByProfilAndRubriqueAndDroitAndValeurTrue(compte.getProfil(), rubrique,
                Droit.CONSULTER);
    }

    /** US-23 : « Mes contrats » et « Mes documents » : le résumé, avec les avenants acceptés et leur PDF. */
    private ContratResumeResponse resumeAvecAvenants(Contrat contrat) {
        ContratResumeResponse resume = resume(contrat);
        List<AvenantResponse> acceptes = historiqueAvenants.duContrat(contrat.getId()).stream()
                .filter(avenant -> AVENANT_ACCEPTE.equals(avenant.statut()) && avenant.documentId() != null)
                .toList();
        return acceptes.isEmpty() ? resume : new ContratResumeResponse(resume.referenceDevis(), resume.numero(),
                resume.produit(), resume.statut(), resume.formule(), resume.bienAssure(), resume.typeLogement(),
                resume.ville(), resume.cotisationMensuelle(), resume.dateEffet(), resume.dateEcheance(),
                resume.dateFin(), resume.ibanMasque(), acceptes);
    }

    private ContratResumeResponse resume(Contrat contrat) {
        return new ContratResumeResponse(
                contrat.getDossier() == null ? null : contrat.getDossier().getReferenceDevis(),
                contrat.getNumero(),
                contrat.getProduit(),
                contrat.getStatut(),
                contrat.getFormule(),
                bienAssure(contrat),
                contrat.getTypeLogement(),
                contrat.getVille(),
                contrat.getCotisationMensuelle(),
                contrat.getDateEffet(),
                echeance(contrat),
                contrat.getDateFin(),
                contrat.getMandatSepa() == null ? null : contrat.getMandatSepa().getIbanMasque());
    }

    private LocalDate echeance(Contrat contrat) {
        return EcheanceAnnuelle.prochaine(contrat.getDateEffet(), contrat.getDateFin(), aujourdhui());
    }

    /** Le logement (adresse) ou le véhicule (marque, modèle, immatriculation), en une ligne. */
    public static String bienAssure(Contrat contrat) {
        if (Contrat.AUTO.equals(contrat.getProduit())) {
            return vehicule(contrat.getVehicule());
        }
        return joindre(", ", contrat.getAdresseLogement(),
                joindre(" ", contrat.getCodePostal(), contrat.getVille()));
    }

    static String vehicule(Vehicule vehicule) {
        if (vehicule == null) {
            return null;
        }
        return joindre(" · ", joindre(" ", vehicule.getMarque(), vehicule.getModele()),
                vehicule.getImmatriculation());
    }

    private static String joindre(String separateur, String... parties) {
        String resultat = Stream.of(parties).filter(partie -> partie != null && !partie.isBlank())
                .collect(Collectors.joining(separateur));
        return resultat.isEmpty() ? null : resultat;
    }

    private List<TableauDeBordResponse.Devis> devisEnCours(Compte compte) {
        List<DevisEnregistre> enCours = devis.enCours(compte, aujourdhui());
        Set<Long> auto = enCours.stream().filter(un -> Contrat.AUTO.equals(un.getProduit()))
                .map(DevisEnregistre::getId).collect(Collectors.toSet());
        Map<Long, String> vehiculesDesDevis = auto.isEmpty() ? Map.of() : vehiculesDesDevis(auto);
        return enCours.stream()
                .map(un -> new TableauDeBordResponse.Devis(un.getProduit(),
                        Contrat.AUTO.equals(un.getProduit()) ? vehiculesDesDevis.get(un.getId())
                                : joindre(", ", un.getAdresse(), joindre(" ", un.getCodePostal(), un.getVille())),
                        un.getFormule(), un.getPrixMensuel(), un.getDateFinValidite()))
                .toList();
    }

    private Map<Long, String> vehiculesDesDevis(Set<Long> devisIds) {
        List<DevisAuto> parties = devisAuto.findByDevisIdIn(devisIds);
        Map<Long, Vehicule> parId = vehicules.findAllById(parties.stream().map(DevisAuto::getVehiculeId)
                        .filter(id -> id != null).toList())
                .stream().collect(Collectors.toMap(Vehicule::getId, Function.identity()));
        return parties.stream().filter(partie -> parId.containsKey(partie.getVehiculeId()))
                .collect(Collectors.toMap(DevisAuto::getDevisId,
                        partie -> vehicule(parId.get(partie.getVehiculeId())), (premier, second) -> premier));
    }

    private List<TableauDeBordResponse.Facture> dernieresFactures(Map<Long, String> numeros) {
        List<Facture> dernieres = factures.findTop3ByContratIdInOrderByPeriodeFinDescIdDesc(numeros.keySet());
        Set<Long> stockes = documents.findAllById(dernieres.stream().map(Facture::getDocumentId)
                        .filter(id -> id != null).toList())
                .stream().filter(Document::getStocke).map(Document::getId).collect(Collectors.toSet());
        return dernieres.stream()
                .map(facture -> new TableauDeBordResponse.Facture(facture.getNumero(),
                        numeros.get(facture.getContratId()), facture.getPeriodeDebut(), facture.getPeriodeFin(),
                        facture.getMontantTtc(),
                        stockes.contains(facture.getDocumentId()) ? facture.getDocumentId() : null))
                .toList();
    }

    private List<TableauDeBordResponse.Sinistre> sinistresDeclares(Map<Long, String> numeros) {
        List<Sinistre> declares = sinistres.findByContratIdInAndStatutOrderByDateSurvenanceDescIdDesc(
                numeros.keySet(), SINISTRE_DECLARE);
        return declares.stream()
                .map(sinistre -> new TableauDeBordResponse.Sinistre(sinistre.getNumero(),
                        numeros.get(sinistre.getContratId()), sinistre.getType(), sinistre.getDateSurvenance(),
                        sinistre.getDeclareLe()))
                .toList();
    }

    /**
     * Documents en vigueur du contrat, puis les conditions générales en vigueur de son produit. US-23 : relus par
     * l'écran « Relire et accepter l'avenant » ; les avenants ont leur propre section (historique des avenants).
     */
    public List<FicheContratResponse.Document> documentsDeLaFiche(Contrat contrat) {
        List<String> types = DOCUMENTS_FICHE.stream().filter(type -> type != TypeDocument.CONDITIONS_GENERALES)
                .map(Enum::name).toList();
        Stream<Document> duContrat = documents.enVigueurDuContrat(contrat.getId(), types).stream();
        Stream<Document> generales = documents.enVigueur(0, TypeDocument.CONDITIONS_GENERALES.name(),
                contrat.getProduit()).stream();
        return Stream.concat(duContrat, generales)
                .map(document -> new FicheContratResponse.Document(document.getId(), document.getType(),
                        TypeDocument.valueOf(document.getType()).libelle(), document.getVersion(),
                        document.getDateCreation()))
                .toList();
    }

    private LocalDate aujourdhui() {
        return LocalDate.ofInstant(horloge.instant(), FRANCE);
    }
}
