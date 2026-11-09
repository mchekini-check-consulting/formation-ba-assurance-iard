package net.checkconsulting.assuranceiard.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import net.checkconsulting.assuranceiard.api.dto.BesoinsDevis;
import net.checkconsulting.assuranceiard.api.dto.LogementDevis;
import net.checkconsulting.assuranceiard.api.dto.TarifRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TarifMoteurMapperTest {

    private final TarifMoteurMapper mapper = new TarifMoteurMapper();

    private static LogementDevis logement(
            String statut, String pieces, String etage, String dependances, String sinistres, String antecedent) {
        return new LogementDevis(statut, "principale", "appartement", "12 rue de la Paix", "69003", "Lyon",
                "45", pieces, antecedent, "", "", etage, dependances, "", "", sinistres);
    }

    private static LogementDevis logementComplet() {
        return logement("locataire", "2", "intermediaire", null, "0", "non-assure");
    }

    private TarifRequest versMoteur(LogementDevis logement) {
        return mapper.versMoteur(logement, null, null, null, null);
    }

    @Test
    void traduitLeLogementSansDonneePersonnelle() {
        TarifRequest requete = versMoteur(logementComplet());

        assertThat(requete.statut()).isEqualTo("LOCATAIRE");
        assertThat(requete.residence()).isEqualTo("PRINCIPALE");
        assertThat(requete.typeLogement()).isEqualTo("APPARTEMENT");
        assertThat(requete.codePostal()).isEqualTo("69003");
        assertThat(requete.surface()).isEqualByComparingTo("45");
        assertThat(requete.pieces()).isEqualTo(2);
        assertThat(requete.etage()).isEqualTo("INTERMEDIAIRE");
        assertThat(requete.sinistres3ans()).isZero();
        assertThat(requete.resiliationAnterieure()).isEqualTo("NON");
        assertThat(requete.toString()).doesNotContain("Paix", "Lyon");
    }

    @ParameterizedTest
    @CsvSource({
        "locataire, LOCATAIRE",
        "colocation, LOCATAIRE",
        "proprietaire-occupant, PROPRIETAIRE_OCCUPANT",
        "proprietaire-non-occupant, PROPRIETAIRE_NON_OCCUPANT"
    })
    void traduitLeStatut(String ecran, String moteur) {
        assertThat(versMoteur(logement(ecran, "2", "rdc", null, "0", null)).statut()).isEqualTo(moteur);
    }

    @ParameterizedTest
    @CsvSource({"1, 1", "5, 5", "6, 5", "10, 5"})
    void plafonneLesPieces(String ecran, int moteur) {
        assertThat(versMoteur(logement("locataire", ecran, "rdc", null, "0", null)).pieces()).isEqualTo(moteur);
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "2, 2", "3+, 3"})
    void traduitLesSinistres(String ecran, int moteur) {
        assertThat(versMoteur(logement("locataire", "2", "rdc", null, ecran, null)).sinistres3ans()).isEqualTo(moteur);
    }

    @ParameterizedTest
    @CsvSource({"rdc, RDC", "dernier, DERNIER", "0, RDC", "3, INTERMEDIAIRE"})
    void traduitLEtageMemeSurUnAncienDevis(String ecran, String moteur) {
        assertThat(versMoteur(logement("locataire", "2", ecran, null, "0", null)).etage()).isEqualTo(moteur);
    }

    @ParameterizedTest
    @CsvSource({"resiliation-non-paiement, NON_PAIEMENT", "resiliation-autre, AUTRE", "non-assure, NON", "assure-plus-un-an, NON"})
    void traduitLaResiliation(String ecran, String moteur) {
        assertThat(versMoteur(logement("locataire", "2", "rdc", null, "0", ecran)).resiliationAnterieure()).isEqualTo(moteur);
    }

    @Test
    void uneValeurAbsenteResteAbsentePourQueLeMoteurLaRefuse() {
        TarifRequest requete = versMoteur(logement("locataire", null, null, null, null, null));

        assertThat(requete.pieces()).isNull();
        assertThat(requete.etage()).isNull();
        assertThat(requete.sinistres3ans()).isNull();
    }

    @Test
    void traduitLesBesoinsEtLesChoix() {
        BesoinsDevis besoins = new BesoinsDevis(15_000, true, false, "neuf", true, false, "equilibre");

        TarifRequest requete = mapper.versMoteur(logementComplet(), besoins, "serenite", List.of("assistance-depannage"), "300");

        assertThat(requete.besoins().valeurContenu()).isEqualByComparingTo("15000");
        assertThat(requete.besoins().remboursement()).isEqualTo("NEUF");
        assertThat(requete.besoins().priorite()).isEqualTo("EQUILIBRE");
        assertThat(requete.formule()).isEqualTo("SERENITE");
        assertThat(requete.options()).containsExactly("ASSISTANCE");
        assertThat(requete.franchise()).isEqualTo(300);
    }

    @Test
    void traduitLaReponseDansLeVocabulaireDeLEcran() {
        TarifResponse moteur = new TarifResponse(null, "v", true, null, null,
                List.of(new TarifResponse.PrixFormule("CONFORT", BigDecimal.ONE, BigDecimal.TEN)),
                new TarifResponse.Detail("CONFORT", BigDecimal.ONE,
                        List.of(new TarifResponse.PrixOption("OBJETS_VALEUR", BigDecimal.ONE)),
                        BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN),
                "SERENITE", List.of("raison"), List.of("PROTECTION_JURIDIQUE"), null,
                List.of(new TarifResponse.PrixOption("ASSISTANCE", BigDecimal.ONE)),
                List.of(new TarifResponse.AjustementFranchise(300, BigDecimal.ZERO)));

        TarifResponse ecran = mapper.versEcran(moteur);

        assertThat(ecran.prix().getFirst().formule()).isEqualTo("confort");
        assertThat(ecran.detail().formule()).isEqualTo("confort");
        assertThat(ecran.detail().options().getFirst().option()).isEqualTo("objets-valeur");
        assertThat(ecran.formuleConseillee()).isEqualTo("serenite");
        assertThat(ecran.optionsConseillees()).containsExactly("protection-juridique");
        assertThat(ecran.prixOptions().getFirst().option()).isEqualTo("assistance-depannage");
        assertThat(ecran.ajustementsFranchise().getFirst().franchise()).isEqualTo(300);
    }

    @Test
    void uneReponseDeRefusResteSansPrix() {
        TarifResponse refus = new TarifResponse(null, "v", false, "motif", "CONSEILLER",
                null, null, null, null, null, null, null, null);

        TarifResponse ecran = mapper.versEcran(refus);

        assertThat(ecran.tarifable()).isFalse();
        assertThat(ecran.motif()).isEqualTo("motif");
        assertThat(ecran.prix()).isNull();
        assertThat(ecran.detail()).isNull();
    }
}
