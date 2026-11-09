package net.checkconsulting.assuranceiard.api.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.checkconsulting.assuranceiard.api.dto.BesoinsDevis;
import net.checkconsulting.assuranceiard.api.dto.LogementDevis;
import net.checkconsulting.assuranceiard.api.dto.TarifRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import org.springframework.stereotype.Component;

@Component
public class TarifMoteurMapper {

    private static final int PIECES_MAX = 5;
    private static final int SINISTRES_MAX = 3;
    private static final String ETAGE_RDC = "RDC";
    private static final String ETAGE_INTERMEDIAIRE = "INTERMEDIAIRE";
    private static final String ZERO = "0";

    private static final Map<String, String> STATUTS = Map.of(
            "locataire", "LOCATAIRE",
            "colocation", "LOCATAIRE",
            "proprietaire-occupant", "PROPRIETAIRE_OCCUPANT",
            "proprietaire-non-occupant", "PROPRIETAIRE_NON_OCCUPANT");
    private static final Map<String, String> RESILIATIONS = Map.of(
            "resiliation-non-paiement", "NON_PAIEMENT",
            "resiliation-autre", "AUTRE");
    private static final String SANS_RESILIATION = "NON";
    private static final Map<String, String> OPTIONS_VERS_MOTEUR = Map.of(
            "objets-valeur", "OBJETS_VALEUR",
            "assistance-depannage", "ASSISTANCE",
            "protection-juridique", "PROTECTION_JURIDIQUE");
    private static final Map<String, String> OPTIONS_VERS_ECRAN = Map.of(
            "OBJETS_VALEUR", "objets-valeur",
            "ASSISTANCE", "assistance-depannage",
            "PROTECTION_JURIDIQUE", "protection-juridique");

    public TarifRequest versMoteur(
            LogementDevis logement, BesoinsDevis besoins, String formule, List<String> options, String franchise) {
        return new TarifRequest(
                logement.statut() == null ? null : STATUTS.get(logement.statut()),
                majuscules(logement.residence()),
                majuscules(logement.typeLogement()),
                logement.codePostal(),
                logement.surface() == null ? null : new BigDecimal(logement.surface()),
                pieces(logement.nombrePieces()),
                etage(logement.etage()),
                majuscules(logement.dependances()),
                sinistres(logement.nombreSinistres()),
                logement.antecedent() == null ? SANS_RESILIATION : RESILIATIONS.getOrDefault(logement.antecedent(), SANS_RESILIATION),
                besoins == null ? null : besoins(besoins),
                majuscules(formule),
                options == null ? null : options.stream().map(OPTIONS_VERS_MOTEUR::get).toList(),
                franchise == null ? null : Integer.valueOf(franchise));
    }

    public TarifResponse versEcran(TarifResponse tarif) {
        return new TarifResponse(
                tarif.idTarif(),
                tarif.versionGrille(),
                tarif.tarifable(),
                tarif.motif(),
                tarif.orientation(),
                tarif.prix() == null ? null : tarif.prix().stream()
                        .map(p -> new TarifResponse.PrixFormule(minuscules(p.formule()), p.mensuel(), p.annuel()))
                        .toList(),
                tarif.detail() == null ? null : detail(tarif.detail()),
                minuscules(tarif.formuleConseillee()),
                tarif.raisons(),
                tarif.optionsConseillees() == null ? null
                        : tarif.optionsConseillees().stream().map(OPTIONS_VERS_ECRAN::get).toList(),
                tarif.dateValidite(),
                tarif.prixOptions() == null ? null : options(tarif.prixOptions()),
                tarif.ajustementsFranchise());
    }

    private TarifResponse.Detail detail(TarifResponse.Detail detail) {
        return new TarifResponse.Detail(
                minuscules(detail.formule()),
                detail.prixFormule(),
                options(detail.options()),
                detail.ajustementFranchise(),
                detail.totalMensuel(),
                detail.totalAnnuel());
    }

    private List<TarifResponse.PrixOption> options(List<TarifResponse.PrixOption> options) {
        return options.stream()
                .map(o -> new TarifResponse.PrixOption(OPTIONS_VERS_ECRAN.get(o.option()), o.prixMensuel()))
                .toList();
    }

    private TarifRequest.Besoins besoins(BesoinsDevis besoins) {
        return new TarifRequest.Besoins(
                BigDecimal.valueOf(besoins.valeurContenu()),
                besoins.objetsValeur(),
                besoins.appareilsCouteux(),
                majuscules(besoins.remboursement()),
                besoins.assistance(),
                besoins.litiges(),
                majuscules(besoins.priorite()));
    }

    private Integer pieces(String pieces) {
        return pieces == null || pieces.isBlank() ? null : Math.min(Integer.parseInt(pieces), PIECES_MAX);
    }

    private String etage(String etage) {
        if (etage == null || etage.isBlank()) {
            return null;
        }
        if (etage.chars().allMatch(Character::isDigit)) {
            return ZERO.equals(etage) ? ETAGE_RDC : ETAGE_INTERMEDIAIRE;
        }
        return majuscules(etage);
    }

    private Integer sinistres(String sinistres) {
        if (sinistres == null || sinistres.isBlank()) {
            return null;
        }
        return sinistres.endsWith("+") ? Integer.valueOf(SINISTRES_MAX) : Integer.valueOf(sinistres);
    }

    private String majuscules(String valeur) {
        return valeur == null ? null : valeur.toUpperCase(Locale.ROOT);
    }

    private String minuscules(String valeur) {
        return valeur == null ? null : valeur.toLowerCase(Locale.ROOT);
    }
}
