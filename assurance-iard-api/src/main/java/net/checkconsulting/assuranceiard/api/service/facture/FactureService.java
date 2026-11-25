package net.checkconsulting.assuranceiard.api.service.facture;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.DemandeFactureRequest;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.dto.FactureGenereeResponse;
import net.checkconsulting.assuranceiard.api.dto.MesFacturesResponse;
import net.checkconsulting.assuranceiard.api.exception.ContratInconnuException;
import net.checkconsulting.assuranceiard.api.exception.FactureImpossibleException;
import net.checkconsulting.assuranceiard.api.exception.PeriodeFacturationInconnueException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.Facture;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.FactureRepository;
import net.checkconsulting.assuranceiard.api.service.EspaceClientService;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import net.checkconsulting.assuranceiard.api.service.document.DemandeDocument;
import net.checkconsulting.assuranceiard.api.service.document.DocumentsGeneresService;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.Echeancier;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService.LigneEcheance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-22 : « Mes factures » (échéancier et factures de chaque contrat) et génération d'une facture à la demande (RG3 :
 * pas de génération automatique, backlog). Le contrôle d'accès d'US-04 est fait avant chaque route : ce service ne lit
 * que les contrats du compte connecté ou le contrat déjà autorisé.
 * <ul>
 *   <li>RG4 : numéro FAC-AAAA-NNNNNN (année de génération, compteur de la table facture qui ne revient jamais en
 *       arrière).</li>
 *   <li>AC2 : une seule facture par contrat et par période ; si elle existe, elle est renvoyée telle quelle. Pour une
 *       échéance mensuelle, une facture d'un mois qui couvre déjà sa date compte aussi (recette).</li>
 *   <li>Recette : facture d'une échéance seulement si elle est payée ; facture annuelle seulement quand toutes les
 *       échéances de l'année d'assurance sont payées (elle vaut alors la cotisation annuelle).</li>
 *   <li>Pas de facture pour un contrat en attente de signature ; pour un contrat Résilié ou Renoncé, seulement pour
 *       une période qui commence avant sa date de fin.</li>
 * </ul>
 * Journaux sans données personnelles (identifiant technique du contrat, numéro de facture).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactureService {

    static final Set<String> STATUTS_HORS_VIGUEUR = Set.of("RESILIE", "RENONCE");
    static final String MOTIF = "Facture générée à la demande du client";
    /** Recette US-22 : pas de facture pour une échéance À venir (l'avis d'échéance viendra avec IA-245). */
    static final String MESSAGE_ECHEANCE_NON_PAYEE = "La facture d'une échéance est disponible une fois l'échéance payée.";
    static final String MESSAGE_ANNEE_NON_REGLEE = "Facture annuelle disponible quand l'année sera entièrement réglée.";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final SessionService sessionService;
    private final ContratRepository contrats;
    private final DonneesContratRepository donneesContrat;
    private final FactureRepository factures;
    private final DocumentRepository documents;
    private final EcheancierService echeancier;
    private final CalculFacture calcul;
    private final DocumentsGeneresService documentsGeneres;
    private final Clock horloge;

    /** AC1, AC2, AC4 : échéancier et factures de chaque contrat signé du client connecté. */
    @LogMetier(champs = {"resultat.aucuneCotisation"})
    @Transactional(readOnly = true)
    public MesFacturesResponse mesFactures(String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        List<Contrat> signes = contrats.duClient(compte).stream()
                .filter(contrat -> !EcheancierService.EN_ATTENTE_DE_SIGNATURE.equals(contrat.getStatut()))
                .toList();
        Map<Long, List<Facture>> facturesParContrat = signes.isEmpty() ? Map.of()
                : factures.findByContratIdInOrderByPeriodeFinDescIdDesc(signes.stream().map(Contrat::getId).toList())
                        .stream().collect(Collectors.groupingBy(Facture::getContratId));
        Set<Long> stockes = stockes(facturesParContrat.values().stream().flatMap(Collection::stream).toList());
        List<MesFacturesResponse.Contrat> resultat = signes.stream()
                .map(contrat -> contrat(contrat, facturesParContrat.getOrDefault(contrat.getId(), List.of()),
                        stockes))
                .toList();
        boolean aucuneCotisation = resultat.stream()
                .allMatch(contrat -> contrat.echeances().isEmpty() && contrat.factures().isEmpty());
        return new MesFacturesResponse(resultat, aucuneCotisation);
    }

    /** AC2 : génère (ou renvoie) la facture d'une échéance ou d'une année de cotisation du contrat. */
    @LogMetier(champs = {"demande.periode", "resultat.existante", "resultat.disponible"})
    @Transactional
    public FactureGenereeResponse generer(String numero, DemandeFactureRequest demande) {
        Contrat contrat = contrats.parNumero(numero).orElseThrow(ContratInconnuException::new);
        if (EcheancierService.EN_ATTENTE_DE_SIGNATURE.equals(contrat.getStatut())) {
            throw new FactureImpossibleException();
        }
        // Deux clics simultanés : le second attend le premier, puis retrouve sa facture.
        factures.verrouillerContrat(contrat.getId());
        DonneesContrat donnees = donneesContrat.lire(contrat.getId()).orElseThrow(ContratInconnuException::new);
        Periode periode = periode(echeancier.de(contrat.getStatut(), donnees), demande);
        if (!facturable(contrat, periode.debut())) {
            throw new FactureImpossibleException();
        }
        // Recette US-22, point 3 : une facture qui couvre déjà la période (ou le mois de l'échéance) est renvoyée.
        Optional<Facture> existante = existante(
                factures.findByContratIdInOrderByPeriodeFinDescIdDesc(List.of(contrat.getId())), periode);
        if (existante.isPresent()) {
            Facture facture = existante.get();
            boolean disponible = estStocke(facture, stockes(List.of(facture)));
            return new FactureGenereeResponse(reponse(facture, disponible), disponible, true);
        }
        // Recette US-22, point 2 : échéance payée, ou année d'assurance entièrement réglée.
        if (!periode.reglee()) {
            throw new FactureImpossibleException(demande.periode() == DemandeFactureRequest.Periode.ECHEANCE
                    ? MESSAGE_ECHEANCE_NON_PAYEE : MESSAGE_ANNEE_NON_REGLEE);
        }
        RepartitionTaxe.Detail detail = calcul.detail(donnees, periode.montantTtc());
        long id = factures.reserverIdentifiant();
        Instant maintenant = horloge.instant();
        String numeroFacture = numero(LocalDate.ofInstant(maintenant, FRANCE).getYear(), id);
        factures.inserer(id, numeroFacture, contrat.getId(), periode.debut(), periode.fin(), detail.montantHt(),
                detail.taxe(), detail.montantTtc(), maintenant);
        Document document = documentsGeneres.generer(
                new DemandeDocument(TypeDocument.FACTURE, contrat.getId(), numeroFacture, MOTIF));
        Facture facture = factures.findById(id).orElseThrow();
        facture.setDocumentId(document.getId());
        boolean disponible = Boolean.TRUE.equals(document.getStocke());
        log.info("Facture générée : contrat={}, facture={}, periode={}, stocke={}", contrat.getId(), numeroFacture,
                demande.periode(), disponible);
        return new FactureGenereeResponse(reponse(facture, disponible), disponible, false);
    }

    /** RG4 : FAC-AAAA-NNNNNN. */
    static String numero(int annee, long compteur) {
        return "FAC-%d-%06d".formatted(annee, compteur);
    }

    /** Contrat Résilié ou Renoncé : seules les périodes qui commencent avant sa fin (à défaut, aujourd'hui). */
    private boolean facturable(Contrat contrat, LocalDate debut) {
        if (!STATUTS_HORS_VIGUEUR.contains(contrat.getStatut())) {
            return true;
        }
        LocalDate limite = contrat.getDateFin() == null ? LocalDate.ofInstant(horloge.instant(), FRANCE)
                : contrat.getDateFin();
        return debut.isBefore(limite);
    }

    /**
     * Période demandée.
     *
     * @param mensuelle vrai pour l'échéance d'un paiement mensuel (une facture d'un mois qui couvre sa date suffit).
     * @param reglee échéance payée, ou année dont toutes les échéances sont payées.
     */
    private record Periode(LocalDate debut, LocalDate fin, BigDecimal montantTtc, boolean mensuelle,
            boolean reglee) {
    }

    private static Periode periode(Echeancier echeancier, DemandeFactureRequest demande) {
        Optional<Periode> trouvee = switch (demande.periode()) {
            case ECHEANCE -> echeancier.echeances().stream()
                    .filter(ligne -> ligne.date().equals(demande.date()))
                    .findFirst()
                    .map(FactureService::periode);
            case ANNEE -> echeancier.annees().stream()
                    .filter(annee -> annee.debut().equals(demande.date()))
                    .findFirst()
                    .map(annee -> new Periode(annee.debut(), annee.fin(), annee.montantTtc(), false,
                            annee.reglee()));
        };
        return trouvee.orElseThrow(PeriodeFacturationInconnueException::new);
    }

    private static Periode periode(LigneEcheance ligne) {
        return new Periode(ligne.periodeDebut(), ligne.periodeFin(), ligne.montantTtc(),
                estMensuelle(ligne.periodeDebut(), ligne.periodeFin()), EcheancierService.PAYEE.equals(ligne.statut()));
    }

    /**
     * La facture de cette période : même période, ou, pour une échéance mensuelle, une facture d'au plus un mois qui
     * couvre sa date (ex. FAC-RECETTE-0001 du 01/10 au 31/10 pour l'échéance du 10/10). Une facture annuelle ne
     * couvre pas une échéance mensuelle, et inversement (décision BA).
     */
    private static Optional<Facture> existante(List<Facture> sesFactures, Periode periode) {
        Optional<Facture> memePeriode = sesFactures.stream()
                .filter(facture -> facture.getPeriodeDebut().equals(periode.debut())
                        && facture.getPeriodeFin().equals(periode.fin()))
                .min(Comparator.comparing(Facture::getId));
        if (memePeriode.isPresent() || !periode.mensuelle()) {
            return memePeriode;
        }
        return sesFactures.stream()
                .filter(facture -> estMensuelle(facture.getPeriodeDebut(), facture.getPeriodeFin()))
                .filter(facture -> !facture.getPeriodeDebut().isAfter(periode.debut())
                        && !facture.getPeriodeFin().isBefore(periode.debut()))
                .min(Comparator.comparing(Facture::getId));
    }

    /** Une période d'au plus un mois. */
    static boolean estMensuelle(LocalDate debut, LocalDate fin) {
        return !fin.isAfter(debut.plusMonths(1).minusDays(1));
    }

    private MesFacturesResponse.Contrat contrat(Contrat contrat, List<Facture> sesFactures, Set<Long> stockes) {
        Echeancier sonEcheancier = donneesContrat.lire(contrat.getId())
                .map(donnees -> echeancier.de(contrat.getStatut(), donnees))
                .orElse(Echeancier.VIDE);
        return new MesFacturesResponse.Contrat(
                contrat.getNumero(),
                contrat.getProduit(),
                contrat.getStatut(),
                EspaceClientService.bienAssure(contrat),
                sonEcheancier.echeances().stream()
                        .map(ligne -> {
                            Periode periode = periode(ligne);
                            return new MesFacturesResponse.Echeance(ligne.date(), ligne.montantTtc(),
                                    ligne.statut(), ligne.periodeDebut(), ligne.periodeFin(),
                                    periode.reglee() && facturable(contrat, ligne.periodeDebut()),
                                    numeroDe(existante(sesFactures, periode)));
                        })
                        .toList(),
                sonEcheancier.annees().stream()
                        .map(annee -> {
                            Periode periode = new Periode(annee.debut(), annee.fin(), annee.montantTtc(), false,
                                    annee.reglee());
                            return new MesFacturesResponse.Annee(annee.debut(), annee.fin(), annee.montantTtc(),
                                    annee.reglee() && facturable(contrat, annee.debut()), annee.reglee(),
                                    numeroDe(existante(sesFactures, periode)));
                        })
                        .toList(),
                sesFactures.stream()
                        .map(facture -> reponse(facture, estStocke(facture, stockes)))
                        .toList());
    }

    private static String numeroDe(Optional<Facture> facture) {
        return facture.map(Facture::getNumero).orElse(null);
    }

    private static MesFacturesResponse.Facture reponse(Facture facture, boolean stocke) {
        return new MesFacturesResponse.Facture(facture.getNumero(), facture.getPeriodeDebut(),
                facture.getPeriodeFin(), facture.getMontantHt(), facture.getTaxe(), facture.getMontantTtc(),
                facture.getGenereeLe(), stocke ? facture.getDocumentId() : null);
    }

    private static boolean estStocke(Facture facture, Set<Long> stockes) {
        return facture.getDocumentId() != null && stockes.contains(facture.getDocumentId());
    }

    /** Documents des factures dont le PDF est stocké (téléchargeables). */
    private Set<Long> stockes(List<Facture> sesFactures) {
        List<Long> ids = sesFactures.stream().map(Facture::getDocumentId).filter(id -> id != null).toList();
        return ids.isEmpty() ? Set.of() : documents.findAllById(ids).stream()
                .filter(document -> Boolean.TRUE.equals(document.getStocke()))
                .map(Document::getId)
                .collect(Collectors.toSet());
    }
}
