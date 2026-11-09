package net.checkconsulting.moteurtarifaire.api.service;

import static net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees.GRILLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.dto.Besoins;
import net.checkconsulting.moteurtarifaire.api.dto.FormuleConseillee;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire.ReglesRecommandation;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.Priorite;
import net.checkconsulting.moteurtarifaire.api.enums.RaisonRecommandation;
import net.checkconsulting.moteurtarifaire.api.enums.Remboursement;
import net.checkconsulting.moteurtarifaire.api.service.recommandation.RegleConfort;
import net.checkconsulting.moteurtarifaire.api.service.recommandation.RegleEssentielle;
import net.checkconsulting.moteurtarifaire.api.service.recommandation.RegleRecommandation;
import net.checkconsulting.moteurtarifaire.api.service.recommandation.RegleSerenite;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

class RecommandationServiceTest {

    private static final ReglesRecommandation REGLES = GRILLE.recommandation();
    private static final Map<RaisonRecommandation, String> RAISONS = REGLES.raisons();

    private final RecommandationService service = new RecommandationService(reglesDansLOrdreDeSpring());

    private static List<RegleRecommandation> reglesDansLOrdreDeSpring() {
        List<RegleRecommandation> regles = new ArrayList<>(List.of(new RegleConfort(), new RegleEssentielle(), new RegleSerenite()));
        AnnotationAwareOrderComparator.sort(regles);
        return regles;
    }

    private static Besoins besoins() {
        return JeuxDeDonnees.besoinsConfort();
    }

    private static Besoins besoinsEssentielle() {
        return besoins().toBuilder()
                .valeurContenu(REGLES.seuilContenuEssentielle().subtract(BigDecimal.ONE))
                .priorite(Priorite.PRIX)
                .objetsValeur(false)
                .build();
    }

    private FormuleConseillee conseil(Besoins besoins) {
        return service.formuleConseillee(besoins, REGLES);
    }

    @Test
    void ca6UnContenuDePlusDe35000EurosConseilleSereniteAvecUneRaisonLisible() {
        FormuleConseillee conseil = conseil(besoins().toBuilder().valeurContenu(BigDecimal.valueOf(35_001)).build());

        assertThat(conseil.formule()).isEqualTo(Formule.SERENITE);
        assertThat(conseil.raisons()).isNotEmpty().allSatisfy(raison -> assertThat(raison).isNotBlank());
    }

    @Test
    void justeAuDessusDuSeuilSerenite() {
        FormuleConseillee conseil = conseil(besoins().toBuilder().valeurContenu(REGLES.seuilContenuSerenite().add(BigDecimal.ONE)).build());

        assertThat(conseil.formule()).isEqualTo(Formule.SERENITE);
        assertThat(conseil.raisons()).containsExactly(RAISONS.get(RaisonRecommandation.CONTENU_ELEVE));
    }

    @Test
    void auSeuilSereniteExactCEstEncoreConfort() {
        assertThat(conseil(besoins().toBuilder().valeurContenu(REGLES.seuilContenuSerenite()).build()).formule())
                .isEqualTo(Formule.CONFORT);
    }

    @Test
    void unRemboursementANeufConseilleSerenite() {
        FormuleConseillee conseil = conseil(besoins().toBuilder().remboursement(Remboursement.NEUF).build());

        assertThat(conseil.formule()).isEqualTo(Formule.SERENITE);
        assertThat(conseil.raisons()).containsExactly(RAISONS.get(RaisonRecommandation.REMBOURSEMENT_NEUF));
    }

    @Test
    void desAppareilsCouteuxConseillentSerenite() {
        FormuleConseillee conseil = conseil(besoins().toBuilder().appareilsCouteux(true).build());

        assertThat(conseil.formule()).isEqualTo(Formule.SERENITE);
        assertThat(conseil.raisons()).containsExactly(RAISONS.get(RaisonRecommandation.APPAREILS_COUTEUX));
    }

    @Test
    void unePrioriteProtectionConseilleSerenite() {
        FormuleConseillee conseil = conseil(besoins().toBuilder().priorite(Priorite.PROTECTION).build());

        assertThat(conseil.formule()).isEqualTo(Formule.SERENITE);
        assertThat(conseil.raisons()).containsExactly(RAISONS.get(RaisonRecommandation.PRIORITE_PROTECTION));
    }

    @Test
    void toutesLesRaisonsSereniteRempliesSontCitees() {
        FormuleConseillee conseil = conseil(besoins().toBuilder()
                .valeurContenu(REGLES.seuilContenuSerenite().add(BigDecimal.ONE))
                .remboursement(Remboursement.NEUF)
                .appareilsCouteux(true)
                .priorite(Priorite.PROTECTION)
                .build());

        assertThat(conseil.raisons()).hasSize(4);
    }

    @Test
    void sereniteLEmporteSurEssentielle() {
        assertThat(conseil(besoinsEssentielle().toBuilder().appareilsCouteux(true).build()).formule())
                .isEqualTo(Formule.SERENITE);
    }

    @Test
    void essentielleQuandLesTroisConditionsSontReunies() {
        FormuleConseillee conseil = conseil(besoinsEssentielle());

        assertThat(conseil.formule()).isEqualTo(Formule.ESSENTIELLE);
        assertThat(conseil.raisons()).containsExactly(
                RAISONS.get(RaisonRecommandation.CONTENU_FAIBLE),
                RAISONS.get(RaisonRecommandation.AUCUN_OBJET_VALEUR),
                RAISONS.get(RaisonRecommandation.PRIORITE_PRIX));
    }

    @Test
    void confortSiLeContenuAtteintLeSeuilEssentielle() {
        FormuleConseillee conseil = conseil(besoinsEssentielle().toBuilder().valeurContenu(REGLES.seuilContenuEssentielle()).build());

        assertThat(conseil.formule()).isEqualTo(Formule.CONFORT);
        assertThat(conseil.raisons()).containsExactly(RAISONS.get(RaisonRecommandation.CONTENU_MOYEN));
    }

    @Test
    void confortSiLeClientADesObjetsDeValeur() {
        FormuleConseillee conseil = conseil(besoinsEssentielle().toBuilder().objetsValeur(true).build());

        assertThat(conseil.formule()).isEqualTo(Formule.CONFORT);
        assertThat(conseil.raisons()).containsExactly(RAISONS.get(RaisonRecommandation.OBJETS_VALEUR));
    }

    @Test
    void confortSiLaPrioriteEstLEquilibre() {
        FormuleConseillee conseil = conseil(besoinsEssentielle().toBuilder().priorite(Priorite.EQUILIBRE).build());

        assertThat(conseil.formule()).isEqualTo(Formule.CONFORT);
        assertThat(conseil.raisons()).containsExactly(RAISONS.get(RaisonRecommandation.PRIORITE_EQUILIBRE));
    }

    @Test
    void uneChaineSansReponseEstUneErreurDeConfiguration() {
        RecommandationService chaineVide = new RecommandationService(List.of());

        assertThatThrownBy(() -> chaineVide.formuleConseillee(besoins(), REGLES)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void chaqueBesoinConseilleSonOption() {
        assertThat(service.optionsConseillees(besoins())).isEmpty();
        assertThat(service.optionsConseillees(besoins().toBuilder().objetsValeur(true).build()))
                .containsExactly(OptionTarifaire.OBJETS_VALEUR);
        assertThat(service.optionsConseillees(besoins().toBuilder().assistance(true).build()))
                .containsExactly(OptionTarifaire.ASSISTANCE);
        assertThat(service.optionsConseillees(besoins().toBuilder().litiges(true).build()))
                .containsExactly(OptionTarifaire.PROTECTION_JURIDIQUE);
    }
}
