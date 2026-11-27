package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.config.SinistresProperties;
import net.checkconsulting.assuranceiard.api.dto.ContexteDeclarationResponse;
import net.checkconsulting.assuranceiard.api.dto.CreationSinistreRequest;
import net.checkconsulting.assuranceiard.api.dto.DeclarationSinistreResponse;
import net.checkconsulting.assuranceiard.api.dto.MiseAJourSinistreRequest;
import net.checkconsulting.assuranceiard.api.dto.SinistreResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.TypesSinistreResponse;
import net.checkconsulting.assuranceiard.api.exception.ContratInconnuException;
import net.checkconsulting.assuranceiard.api.exception.DeclarationDejaEnvoyeeException;
import net.checkconsulting.assuranceiard.api.exception.DeclarationImpossibleException;
import net.checkconsulting.assuranceiard.api.exception.DeclarationInvalideException;
import net.checkconsulting.assuranceiard.api.exception.SinistreInconnuException;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository.TypeSinistre;
import net.checkconsulting.assuranceiard.api.repository.SinistreRepository;
import net.checkconsulting.assuranceiard.api.service.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-24 : parcours de déclaration (contrat, type, circonstances, brouillon) et « Mes sinistres ». Le contrôle d'accès
 * d'US-04 a déjà vérifié, avant chaque route, que le client est propriétaire du contrat ou du sinistre.
 * <ul>
 *   <li>AC1 : seuls les contrats ouverts à la déclaration ({@link ReglesDeclaration}) ; refus aussi en appel direct.</li>
 *   <li>AC8, RG6 : le brouillon est enregistré à chaque étape ; il n'a aucune valeur de déclaration et expire
 *       30 jours après sa création (invisible dès son expiration, supprimé par la purge).</li>
 *   <li>RG9, RG11 : corbeille du produit, remplie à la création.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class DeclarationSinistreService {

    public static final String BROUILLON = "BROUILLON";
    public static final String DECLARE = "DECLARE";
    private static final short ETAPE_CIRCONSTANCES = 2;

    private final SessionService sessionService;
    private final ContratRepository contrats;
    private final SinistreRepository sinistres;
    private final ReferentielSinistresRepository referentiel;
    private final VueDeclaration vue;
    private final JsonSinistre json;
    private final SinistresProperties proprietes;

    /** AC1, AC10 : contrats ouverts à la déclaration (aucun présélectionné) et numéro d'assistance. */
    @LogMetier(champs = {"resultat.contrats.taille"})
    @Transactional(readOnly = true)
    public ContexteDeclarationResponse contexte(String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        List<ContexteDeclarationResponse.ContratDeclarable> declarables = contrats.duClient(compte).stream()
                .filter(contrat -> ReglesDeclaration.declarable(contrat, vue.aujourdhui()))
                .map(VueDeclaration::contrat).toList();
        return new ContexteDeclarationResponse(compte.getPrenom(), proprietes.numeroAssistance(), declarables);
    }

    /** AC2, AC7, AC10 : types du produit du contrat (« Autre » en dernier) et pièces attendues. */
    @LogMetier(champs = {"resultat.produit"})
    @Transactional(readOnly = true)
    public TypesSinistreResponse types(String numeroContrat) {
        Contrat contrat = declarable(numeroContrat);
        List<TypesSinistreResponse.Type> types = referentiel.types(contrat.getProduit()).stream()
                .map(type -> new TypesSinistreResponse.Type(type.code(), type.libelle(), type.aide(),
                        ReglesDeclaration.delaiPrevu(type), type.delaiJoursArrete() != null, type.gestesUrgence(),
                        referentiel.pieces(contrat.getProduit(), type.code()).stream().map(VueDeclaration::piece)
                                .toList()))
                .toList();
        return new TypesSinistreResponse(contrat.getNumero(), contrat.getProduit(), proprietes.numeroAssistance(),
                types);
    }

    /** AC1, AC2, AC8 : crée le brouillon avec le contrat et le type choisis à l'étape 1. */
    @LogMetier(champs = {"resultat.type", "resultat.etape"})
    @Transactional
    public DeclarationSinistreResponse creer(CreationSinistreRequest demande) {
        Contrat contrat = declarable(demande.numeroContrat());
        TypeSinistre type = typeDuProduit(contrat, demande.type());
        Sinistre sinistre = new Sinistre();
        sinistre.setContratId(contrat.getId());
        sinistre.setType(type.code());
        sinistre.setCorbeille(contrat.getProduit());
        sinistre.setRequalifier(type.autre());
        sinistre.setEtape(ETAPE_CIRCONSTANCES);
        return vue.de(sinistres.save(sinistre), contrat, prenom(contrat));
    }

    /** AC3 à AC6 : une déclaration (brouillon non expiré ou dossier envoyé), contrôles recalculés. */
    @LogMetier(champs = {"resultat.statut", "resultat.etape"})
    @Transactional(readOnly = true)
    public DeclarationSinistreResponse lire(Long id) {
        Sinistre sinistre = visible(id);
        Contrat contrat = contratDe(sinistre);
        return vue.de(sinistre, contrat, prenom(contrat));
    }

    /** AC4 à AC13 : enregistre les réponses d'une étape du brouillon (AC8) et recalcule ses contrôles. */
    @LogMetier(champs = {"resultat.etape", "resultat.controles.horsDelai", "resultat.controles.horsPeriodeContrat"})
    @Transactional
    public DeclarationSinistreResponse mettreAJour(Long id, MiseAJourSinistreRequest demande) {
        Sinistre sinistre = brouillon(id);
        Contrat contrat = contratDe(sinistre);
        Set<String> codes = referentiel.types(contrat.getProduit()).stream().map(TypeSinistre::code)
                .collect(Collectors.toSet());
        ValidationDeclaration.verifierReponses(demande, codes, vue.aujourdhui());
        TypeSinistre type = typeDuProduit(contrat, demande.type());
        appliquer(sinistre, demande);
        sinistre.setRequalifier(type.autre());
        sinistre.setHorsPeriodeContrat(ReglesDeclaration.horsPeriode(contrat, sinistre.getDateSurvenance()));
        sinistre.setHorsDelai(vue.horsDelai(sinistre, type));
        return vue.de(sinistres.save(sinistre), contrat, prenom(contrat));
    }

    /** AC8 : « Mes sinistres » : dossiers déclarés et brouillons non expirés, les plus récents d'abord. */
    @LogMetier(champs = {"resultat.taille"})
    @Transactional(readOnly = true)
    public List<SinistreResumeResponse> mesSinistres(String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        Map<Long, Contrat> sesContrats = contrats.duClient(compte).stream()
                .collect(Collectors.toMap(Contrat::getId, Function.identity()));
        if (sesContrats.isEmpty()) {
            return List.of();
        }
        return sinistres.visibles(sesContrats.keySet(), vue.instant()).stream()
                .map(sinistre -> resume(sinistre, sesContrats.get(sinistre.getContratId()))).toList();
    }

    /** Brouillon modifiable : ni envoyé, ni expiré. */
    Sinistre brouillon(Long id) {
        Sinistre sinistre = visible(id);
        if (!BROUILLON.equals(sinistre.getStatut())) {
            throw new DeclarationDejaEnvoyeeException();
        }
        return sinistre;
    }

    /** Dossier envoyé, ou brouillon pas encore expiré (RG6). */
    Sinistre visible(Long id) {
        Sinistre sinistre = sinistres.findById(id).orElseThrow(SinistreInconnuException::new);
        if (BROUILLON.equals(sinistre.getStatut()) && !sinistre.getBrouillonExpireLe().isAfter(vue.instant())) {
            throw new SinistreInconnuException();
        }
        return sinistre;
    }

    Contrat contratDe(Sinistre sinistre) {
        return contrats.findById(sinistre.getContratId()).orElseThrow(ContratInconnuException::new);
    }

    TypeSinistre typeDuProduit(Contrat contrat, String code) {
        return referentiel.type(contrat.getProduit(), code)
                .orElseThrow(() -> new DeclarationInvalideException(ValidationDeclaration.TYPE_INCONNU));
    }

    static String prenom(Contrat contrat) {
        return contrat.getCompte() == null ? null : contrat.getCompte().getPrenom();
    }

    private Contrat declarable(String numeroContrat) {
        Contrat contrat = contrats.parNumero(numeroContrat).orElseThrow(ContratInconnuException::new);
        if (!ReglesDeclaration.declarable(contrat, vue.aujourdhui())) {
            throw new DeclarationImpossibleException();
        }
        return contrat;
    }

    private void appliquer(Sinistre sinistre, MiseAJourSinistreRequest demande) {
        sinistre.setType(demande.type());
        if (demande.etape() != null) {
            sinistre.setEtape(demande.etape().shortValue());
        }
        sinistre.setDateSurvenance(demande.dateSurvenance());
        sinistre.setHeureSurvenance(demande.heureSurvenance());
        sinistre.setLieu(ValidationDeclaration.nettoyer(demande.lieu()));
        sinistre.setDescription(ValidationDeclaration.nettoyer(demande.description()));
        sinistre.setTiersImplique(demande.tiersImplique());
        sinistre.setTiersInfos(Boolean.TRUE.equals(demande.tiersImplique()) ? json.ecrire(demande.tiers()) : null);
        sinistre.setBlesses(demande.blesses());
        sinistre.setNombreBlesses(Boolean.TRUE.equals(demande.blesses()) && demande.nombreBlesses() != null
                ? demande.nombreBlesses().shortValue() : null);
        sinistre.setDeclarant(demande.declarant());
        boolean pourUnAutre = demande.declarant() != null
                && !ValidationDeclaration.SOUSCRIPTEUR.equals(demande.declarant());
        sinistre.setDeclarantInfos(pourUnAutre ? json.ecrireDeclarant(ValidationDeclaration.nettoyer(
                demande.declarantNom()), ValidationDeclaration.nettoyer(demande.declarantLien())) : null);
        sinistre.setDetails(demande.details() == null || demande.details().isEmpty() ? null
                : json.ecrire(demande.details()));
        sinistre.setDateArreteCatnat(demande.dateArreteCatnat());
        sinistre.setExplicationRetard(ValidationDeclaration.nettoyer(demande.explicationRetard()));
    }

    private SinistreResumeResponse resume(Sinistre sinistre, Contrat contrat) {
        String libelle = referentiel.type(contrat.getProduit(), sinistre.getType()).map(TypeSinistre::libelle)
                .orElse(sinistre.getType());
        return new SinistreResumeResponse(sinistre.getId(), sinistre.getNumero(), sinistre.getStatut(),
                sinistre.getType(), libelle, contrat.getNumero(), contrat.getProduit(), sinistre.getDateSurvenance(),
                sinistre.getEtape(), Boolean.TRUE.equals(sinistre.getPiecesManquantes()), sinistre.getDeclareLe(),
                BROUILLON.equals(sinistre.getStatut()) ? sinistre.getBrouillonExpireLe() : null);
    }
}
