package net.checkconsulting.moteurtarifaire.api.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleInvalideException;
import net.checkconsulting.moteurtarifaire.api.mapper.GrilleTarifaireAssembleur;
import net.checkconsulting.moteurtarifaire.api.model.ParametreTarifaire;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ValidateurGrilleTest {

    private final ValidateurGrille validateur = JeuxDeDonnees.validateurDeGrille();

    private GrilleTarifaire assembler(List<ParametreTarifaire> parametres) {
        return new GrilleTarifaireAssembleur().assembler(JeuxDeDonnees.versionDeReference(), parametres);
    }

    @Test
    void acceptLaGrilleDeReference() {
        assertThatCode(() -> validateur.verifier(assembler(JeuxDeDonnees.parametresDeReference())))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({
        "general, duree-validite-jours",
        "arrondi, decimales",
        "arrondi, mode",
        "prix-base, CONFORT",
        "coefficient-depart, surface-plafond",
        "coefficient-depart, pente-surface",
        "coefficient-depart, pieces-reference",
        "statut, LOCATAIRE",
        "type-logement, MAISON",
        "residence, SECONDAIRE",
        "sinistres, 2",
        "etage, RDC",
        "dependances, PISCINE",
        "prix-option, ASSISTANCE",
        "recommandation, seuil-contenu-serenite",
        "raison, PRIORITE_PRIX"
    })
    void uneLigneManquanteEstRefusee(String categorie, String cle) {
        GrilleTarifaire grille = assembler(JeuxDeDonnees.parametresDeReference().stream()
                .filter(parametre -> !(parametre.getCategorie().equals(categorie) && parametre.getCle().equals(cle)))
                .toList());

        assertThatThrownBy(() -> validateur.verifier(grille))
                .isInstanceOf(GrilleInvalideException.class)
                .hasMessageContaining(grille.version());
    }

    @ParameterizedTest
    @CsvSource({"prix-base, CONFORT", "statut, LOCATAIRE", "prix-option, ASSISTANCE", "general, duree-validite-jours"})
    void uneValeurNegativeEstRefusee(String categorie, String cle) {
        List<ParametreTarifaire> parametres = JeuxDeDonnees.parametresDeReference();
        parametres.stream()
                .filter(parametre -> parametre.getCategorie().equals(categorie) && parametre.getCle().equals(cle))
                .forEach(parametre -> parametre.setValeur("-1"));
        GrilleTarifaire grille = assembler(parametres);

        assertThatThrownBy(() -> validateur.verifier(grille)).isInstanceOf(GrilleInvalideException.class);
    }

    @Test
    void unStatutSansTarifEstAccepteSeulementSIlEstRefuse() {
        GrilleTarifaire grille = assembler(JeuxDeDonnees.parametresDeReference().stream()
                .filter(parametre -> !parametre.getCategorie().equals("refus-statut"))
                .toList());

        assertThatThrownBy(() -> validateur.verifier(grille))
                .isInstanceOf(GrilleInvalideException.class)
                .hasMessageContaining("statut");
    }
}
