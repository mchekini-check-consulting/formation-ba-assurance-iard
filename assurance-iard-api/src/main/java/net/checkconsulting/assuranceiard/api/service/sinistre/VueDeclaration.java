package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.config.SinistresProperties;
import net.checkconsulting.assuranceiard.api.dto.ContexteDeclarationResponse.ContratDeclarable;
import net.checkconsulting.assuranceiard.api.dto.DeclarationSinistreResponse;
import net.checkconsulting.assuranceiard.api.dto.DeclarationSinistreResponse.Controles;
import net.checkconsulting.assuranceiard.api.dto.DeclarationSinistreResponse.PieceJointeResponse;
import net.checkconsulting.assuranceiard.api.dto.TypesSinistreResponse;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.PieceJointe;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import net.checkconsulting.assuranceiard.api.repository.PieceJointeRepository;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository.PieceAttendue;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository.TypeSinistre;
import net.checkconsulting.assuranceiard.api.service.EspaceClientService;
import org.springframework.stereotype.Component;

/**
 * US-24 : une déclaration telle que l'écran l'affiche, contrôles recalculés à chaque lecture : couverture à la date du
 * sinistre (AC3, RG4), période du contrat (AC5), délai (AC6), pièces manquantes (AC7).
 */
@Component
@RequiredArgsConstructor
public class VueDeclaration {

    static final String STOCKEE = "STOCKEE";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final ReferentielSinistresRepository referentiel;
    private final PieceJointeRepository pieces;
    private final CouvertureContrat couverture;
    private final JsonSinistre json;
    private final SinistresProperties proprietes;
    private final Clock horloge;

    public LocalDate aujourdhui() {
        return LocalDate.now(horloge.withZone(FRANCE));
    }

    public Instant instant() {
        return horloge.instant();
    }

    public static ContratDeclarable contrat(Contrat contrat) {
        return new ContratDeclarable(contrat.getNumero(), contrat.getProduit(), contrat.getStatut(),
                contrat.getFormule(), EspaceClientService.bienAssure(contrat), contrat.getDateEffet(),
                contrat.getDateFin());
    }

    /** RG2 et AC6 : déclaration en retard aujourd'hui (jamais bloquant). */
    public boolean horsDelai(Sinistre sinistre, TypeSinistre type) {
        return ReglesDeclaration.horsDelai(type, sinistre.getDateSurvenance(), sinistre.getDateArreteCatnat(),
                aujourdhui());
    }

    /** AC7 : une pièce obligatoire attendue n'a encore aucun fichier déposé. */
    public boolean piecesManquantes(Sinistre sinistre, List<PieceAttendue> attendues) {
        Set<String> deposees = pieces.findBySinistreIdAndStatutOrderByIdAsc(sinistre.getId(), STOCKEE).stream()
                .map(PieceJointe::getTypePiece).collect(Collectors.toSet());
        return attendues.stream().anyMatch(piece -> piece.obligatoire() && !deposees.contains(piece.code()));
    }

    public DeclarationSinistreResponse de(Sinistre sinistre, Contrat contrat, String prenom) {
        TypeSinistre type = referentiel.type(contrat.getProduit(), sinistre.getType()).orElse(null);
        List<PieceAttendue> attendues = referentiel.pieces(contrat.getProduit(), sinistre.getType());
        List<PieceJointe> deposees = pieces.findBySinistreIdAndStatutOrderByIdAsc(sinistre.getId(), STOCKEE);
        JsonSinistre.Declarant declarant = json.declarant(sinistre.getDeclarantInfos());
        return new DeclarationSinistreResponse(
                sinistre.getId(), sinistre.getNumero(), sinistre.getStatut(), sinistre.getEtape(), prenom,
                proprietes.numeroAssistance(), contrat(contrat), sinistre.getType(),
                type == null ? sinistre.getType() : type.libelle(),
                type == null ? List.of() : type.gestesUrgence(),
                sinistre.getDateSurvenance(), sinistre.getHeureSurvenance(), sinistre.getLieu(),
                sinistre.getDescription(), sinistre.getTiersImplique(), json.tiers(sinistre.getTiersInfos()),
                sinistre.getBlesses(),
                sinistre.getNombreBlesses() == null ? null : sinistre.getNombreBlesses().intValue(),
                sinistre.getDeclarant(), declarant == null ? null : declarant.nom(),
                declarant == null ? null : declarant.lien(), json.textes(sinistre.getDetails()),
                sinistre.getDateArreteCatnat(), sinistre.getExplicationRetard(),
                controles(sinistre, contrat, type),
                attendues.stream().map(VueDeclaration::piece).toList(),
                deposees.stream().map(VueDeclaration::piece).toList(),
                piecesManquantes(sinistre, attendues), sinistre.getBrouillonExpireLe(), sinistre.getDeclareLe());
    }

    private Controles controles(Sinistre sinistre, Contrat contrat, TypeSinistre type) {
        if (type == null) {
            return new Controles(null, sinistre.getHorsPeriodeContrat(), sinistre.getHorsDelai(), null, null);
        }
        LocalDate date = sinistre.getDateSurvenance() == null ? aujourdhui() : sinistre.getDateSurvenance();
        return new Controles(couverture.couvert(contrat, type, date).orElse(null),
                ReglesDeclaration.horsPeriode(contrat, sinistre.getDateSurvenance()), horsDelai(sinistre, type),
                ReglesDeclaration.delaiPrevu(type),
                ReglesDeclaration.dateLimite(type, sinistre.getDateSurvenance(), sinistre.getDateArreteCatnat())
                        .orElse(null));
    }

    static TypesSinistreResponse.PieceAttendue piece(PieceAttendue piece) {
        return new TypesSinistreResponse.PieceAttendue(piece.code(), piece.libelle(), piece.obligatoire());
    }

    static PieceJointeResponse piece(PieceJointe piece) {
        return new PieceJointeResponse(piece.getId(), piece.getTypePiece(), piece.getNomFichier(), piece.getFormat(),
                piece.getTailleOctets(), piece.getDateCreation());
    }
}
