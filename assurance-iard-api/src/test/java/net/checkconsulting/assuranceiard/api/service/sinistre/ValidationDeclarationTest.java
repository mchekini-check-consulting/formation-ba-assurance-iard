package net.checkconsulting.assuranceiard.api.service.sinistre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.checkconsulting.assuranceiard.api.dto.MiseAJourSinistreRequest;
import net.checkconsulting.assuranceiard.api.exception.DeclarationInvalideException;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import org.junit.jupiter.api.Test;

/** US-24 : messages clairs des réponses invalides (à chaque étape) ou manquantes (à l'envoi). */
class ValidationDeclarationTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 26);
    private static final Set<String> TYPES = Set.of("VOL", "AUTRE");

    static MiseAJourSinistreRequest demande(LocalDate date, Integer nombreBlesses, String declarant,
            Map<String, String> details) {
        return new MiseAJourSinistreRequest("VOL", 2, date, LocalTime.of(8, 30), "Parking", "Vol constaté le matin",
                false, null, nombreBlesses != null, nombreBlesses, declarant, null, null, details, null, null);
    }

    static Sinistre complet() {
        Sinistre sinistre = new Sinistre();
        sinistre.setDateSurvenance(AUJOURDHUI.minusDays(1));
        sinistre.setLieu("Parking");
        sinistre.setDescription("Vol constaté le matin");
        sinistre.setTiersImplique(false);
        sinistre.setBlesses(false);
        sinistre.setDeclarant("SOUSCRIPTEUR");
        return sinistre;
    }

    private static void refuse(Runnable controle, String message) {
        assertThatThrownBy(controle::run).isInstanceOf(DeclarationInvalideException.class).hasMessage(message);
    }

    @Test
    void ac4UneDateDansLeFuturEstRefusee() {
        refuse(() -> ValidationDeclaration.verifierReponses(demande(AUJOURDHUI.plusDays(1), null, null, null), TYPES,
                AUJOURDHUI), ValidationDeclaration.DATE_FUTURE);
        assertThatCode(() -> ValidationDeclaration.verifierReponses(demande(AUJOURDHUI, null, null, null), TYPES,
                AUJOURDHUI)).doesNotThrowAnyException();
    }

    @Test
    void ac8UnBrouillonPeutResterIncomplet() {
        MiseAJourSinistreRequest vide = new MiseAJourSinistreRequest("VOL", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        assertThatCode(() -> ValidationDeclaration.verifierReponses(vide, TYPES, AUJOURDHUI))
                .doesNotThrowAnyException();
    }

    @Test
    void ac2UnTypeQuiNEstPasProposePourCeProduitEstRefuse() {
        MiseAJourSinistreRequest demande = new MiseAJourSinistreRequest("DEGAT_EAUX", 1, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
        refuse(() -> ValidationDeclaration.verifierReponses(demande, TYPES, AUJOURDHUI),
                ValidationDeclaration.TYPE_INCONNU);
    }

    @Test
    void ac11EtAc13ValeursHorsDesPropositions() {
        refuse(() -> ValidationDeclaration.verifierReponses(demande(AUJOURDHUI, 0, null, null), TYPES, AUJOURDHUI),
                ValidationDeclaration.NOMBRE_BLESSES);
        refuse(() -> ValidationDeclaration.verifierReponses(demande(AUJOURDHUI, null, "VOISIN", null), TYPES,
                AUJOURDHUI), ValidationDeclaration.DECLARANT_INCONNU);
    }

    @Test
    void lesLongueursSontLimiteesAvecUnMessageClair() {
        Map<String, String> details = new HashMap<>();
        details.put("Quel vitrage ?", "x".repeat(501));
        refuse(() -> ValidationDeclaration.verifierReponses(demande(AUJOURDHUI, null, null, details), TYPES,
                AUJOURDHUI), "Une réponse ne doit pas dépasser 500 caractères.");
        Map<String, String> tropDeReponses = new HashMap<>();
        for (int i = 0; i < 21; i++) {
            tropDeReponses.put("question " + i, "oui");
        }
        refuse(() -> ValidationDeclaration.verifierReponses(demande(AUJOURDHUI, null, null, tropDeReponses), TYPES,
                AUJOURDHUI), ValidationDeclaration.REPONSES_TROP_NOMBREUSES);
    }

    @Test
    void ac9UneDeclarationCompleteEtSurLHonneurPart() {
        assertThatCode(() -> ValidationDeclaration.verifierComplete(complet(), null, null, false, true))
                .doesNotThrowAnyException();
    }

    @Test
    void ac9LaDeclarationSurLHonneurEstObligatoire() {
        refuse(() -> ValidationDeclaration.verifierComplete(complet(), null, null, false, null),
                ValidationDeclaration.HONNEUR);
        refuse(() -> ValidationDeclaration.verifierComplete(complet(), null, null, false, false),
                ValidationDeclaration.HONNEUR);
    }

    @Test
    void ac4ChaqueReponseObligatoireManquanteASonMessage() {
        manquante(s -> s.setDateSurvenance(null), ValidationDeclaration.DATE_MANQUANTE);
        manquante(s -> s.setLieu(" "), ValidationDeclaration.LIEU_MANQUANT);
        manquante(s -> s.setDescription(null), ValidationDeclaration.DESCRIPTION_MANQUANTE);
        manquante(s -> s.setTiersImplique(null), ValidationDeclaration.TIERS_MANQUANT);
        manquante(s -> s.setBlesses(null), ValidationDeclaration.BLESSES_MANQUANT);
        manquante(s -> s.setBlesses(true), ValidationDeclaration.NOMBRE_BLESSES_MANQUANT);
        manquante(s -> s.setDeclarant(null), ValidationDeclaration.DECLARANT_MANQUANT);
    }

    @Test
    void ac13UnProcheDonneSonNomEtSonLienAvecLAssure() {
        Sinistre proche = complet();
        proche.setDeclarant("PROCHE");
        refuse(() -> ValidationDeclaration.verifierComplete(proche, "Léa Martin", null, false, true),
                ValidationDeclaration.IDENTITE_DECLARANT);
        assertThatCode(() -> ValidationDeclaration.verifierComplete(proche, "Léa Martin", "Fille", false, true))
                .doesNotThrowAnyException();
    }

    @Test
    void ac6EnRetardLExplicationEstObligatoire() {
        refuse(() -> ValidationDeclaration.verifierComplete(complet(), null, null, true, true),
                ValidationDeclaration.EXPLICATION_RETARD);
        Sinistre explique = complet();
        explique.setExplicationRetard("J'étais à l'étranger");
        assertThatCode(() -> ValidationDeclaration.verifierComplete(explique, null, null, true, true))
                .doesNotThrowAnyException();
    }

    @Test
    void unTexteVideDevientVide() {
        assertThat(ValidationDeclaration.nettoyer("  ")).isNull();
        assertThat(ValidationDeclaration.nettoyer(" Parking ")).isEqualTo("Parking");
    }

    private static void manquante(Consumer<Sinistre> retirer, String message) {
        Sinistre sinistre = complet();
        retirer.accept(sinistre);
        refuse(() -> ValidationDeclaration.verifierComplete(sinistre, null, null, false, true), message);
    }
}
