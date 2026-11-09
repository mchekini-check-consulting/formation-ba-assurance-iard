package net.checkconsulting.moteurtarifaire.api.service;

import static net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees.GRILLE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.dto.DetailFormuleResponse;
import net.checkconsulting.moteurtarifaire.api.dto.PrixFormuleResponse;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.enums.Dependances;
import net.checkconsulting.moteurtarifaire.api.enums.Etage;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.Residence;
import net.checkconsulting.moteurtarifaire.api.enums.StatutOccupant;
import net.checkconsulting.moteurtarifaire.api.enums.TypeLogement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CalculateurPrixTest {

    private final CalculateurPrix calculateur = new CalculateurPrix(new CalculateurCoefficient(JeuxDeDonnees.criteres()));

    @Test
    void ca1LesTroisFormulesDuProfilNominal() {
        List<PrixFormuleResponse> prix = calculateur.prixDesFormules(JeuxDeDonnees.profilCa1(), GRILLE);

        assertThat(prix).extracting(PrixFormuleResponse::formule).containsExactly(Formule.values());
        assertThat(prix).extracting(PrixFormuleResponse::mensuel)
                .containsExactly(new BigDecimal("8.69"), new BigDecimal("11.99"), new BigDecimal("17.49"));
    }

    @Test
    void ca2ConfortAvecOptionEtFranchise() {
        DetailFormuleResponse detail = calculateur.detail(
                JeuxDeDonnees.profilCa1(), Formule.CONFORT, List.of(OptionTarifaire.OBJETS_VALEUR), 300, GRILLE);

        assertThat(detail.prixFormule()).isEqualByComparingTo("11.99");
        assertThat(detail.ajustementFranchise()).isEqualByComparingTo("0");
        assertThat(detail.totalMensuel()).isEqualByComparingTo("14.49");
        assertThat(detail.totalAnnuel()).isEqualByComparingTo("173.88");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("coefficients")
    void chaqueCoefficientDeLUs(String cas, ProfilRisque profil, String prixEssentielle) {
        PrixFormuleResponse essentielle = calculateur.prixDesFormules(profil, GRILLE).getFirst();

        assertThat(essentielle.mensuel()).isEqualByComparingTo(prixEssentielle);
    }

    static Stream<Arguments> coefficients() {
        ProfilRisque neutre = JeuxDeDonnees.profilNeutre();
        ProfilRisque maison = neutre.toBuilder().typeLogement(TypeLogement.MAISON).etage(null).build();
        return Stream.of(
                Arguments.of("neutre", neutre, "7.90"),
                Arguments.of("surface 9 m², plancher 0,8", neutre.toBuilder().surface(BigDecimal.valueOf(9)).build(), "6.32"),
                Arguments.of("surface 90 m²", neutre.toBuilder().surface(BigDecimal.valueOf(90)).build(), "10.67"),
                Arguments.of("surface 400 m²", neutre.toBuilder().surface(BigDecimal.valueOf(400)).build(), "29.71"),
                Arguments.of("surface 1000 m², plafonnée à 400", neutre.toBuilder().surface(BigDecimal.valueOf(1000)).build(), "29.71"),
                Arguments.of("1 pièce", neutre.toBuilder().pieces(1).build(), "7.43"),
                Arguments.of("3 pièces", neutre.toBuilder().pieces(3).build(), "8.37"),
                Arguments.of("5 pièces", neutre.toBuilder().pieces(5).build(), "9.32"),
                Arguments.of("propriétaire occupant", neutre.toBuilder().statut(StatutOccupant.PROPRIETAIRE_OCCUPANT).build(), "12.64"),
                Arguments.of("résidence secondaire", neutre.toBuilder().residence(Residence.SECONDAIRE).build(), "9.88"),
                Arguments.of("1 sinistre", neutre.toBuilder().sinistres3ans(1).build(), "9.09"),
                Arguments.of("2 sinistres", neutre.toBuilder().sinistres3ans(2).build(), "10.27"),
                Arguments.of("3 sinistres ou plus", neutre.toBuilder().sinistres3ans(3).build(), "11.85"),
                Arguments.of("Paris", neutre.toBuilder().codePostal("75011").build(), "9.48"),
                Arguments.of("Hauts-de-Seine", neutre.toBuilder().codePostal("92100").build(), "9.48"),
                Arguments.of("Seine-Saint-Denis", neutre.toBuilder().codePostal("93100").build(), "9.48"),
                Arguments.of("Val-de-Marne", neutre.toBuilder().codePostal("94100").build(), "9.48"),
                Arguments.of("Bouches-du-Rhône", neutre.toBuilder().codePostal("13001").build(), "8.69"),
                Arguments.of("Alpes-Maritimes", neutre.toBuilder().codePostal("06000").build(), "8.69"),
                Arguments.of("Gironde", neutre.toBuilder().codePostal("33000").build(), "8.69"),
                Arguments.of("Haute-Garonne", neutre.toBuilder().codePostal("31000").build(), "8.69"),
                Arguments.of("Nord", neutre.toBuilder().codePostal("59000").build(), "8.69"),
                Arguments.of("rez-de-chaussée", neutre.toBuilder().etage(Etage.RDC).build(), "8.30"),
                Arguments.of("dernier étage", neutre.toBuilder().etage(Etage.DERNIER).build(), "7.90"),
                Arguments.of("maison sans dépendance", maison.toBuilder().dependances(Dependances.AUCUNE).build(), "10.67"),
                Arguments.of("maison avec jardin", maison.toBuilder().dependances(Dependances.JARDIN).build(), "11.20"),
                Arguments.of("maison avec garage", maison.toBuilder().dependances(Dependances.GARAGE).build(), "11.20"),
                Arguments.of("maison avec piscine", maison.toBuilder().dependances(Dependances.PISCINE).build(), "12.26"),
                Arguments.of("maison avec plusieurs dépendances", maison.toBuilder().dependances(Dependances.PLUSIEURS).build(), "12.80"));
    }

    @Test
    void lesMultiplicateursSeCumulent() {
        ProfilRisque cumul = JeuxDeDonnees.profilCa1().toBuilder()
                .statut(StatutOccupant.PROPRIETAIRE_OCCUPANT)
                .residence(Residence.SECONDAIRE)
                .sinistres3ans(1)
                .build();

        assertThat(calculateur.prixDesFormules(cumul, GRILLE).getFirst().mensuel()).isEqualByComparingTo("19.99");
    }

    @Test
    void lAnnuelEstDouzeFoisLeMensuelArrondi() {
        calculateur.prixDesFormules(JeuxDeDonnees.profilCa1(), GRILLE).forEach(prix ->
                assertThat(prix.annuel()).isEqualByComparingTo(prix.mensuel().multiply(BigDecimal.valueOf(12))));
    }

    @Test
    void lesOptionsSAdditionnentAuPrixDeLaGrille() {
        List<OptionTarifaire> options = List.of(OptionTarifaire.values());
        BigDecimal totalOptions = options.stream().map(GRILLE.prixOptions()::get).reduce(BigDecimal.ZERO, BigDecimal::add);

        DetailFormuleResponse detail = calculateur.detail(JeuxDeDonnees.profilCa1(), Formule.CONFORT, options, null, GRILLE);

        assertThat(detail.options()).hasSameSizeAs(options);
        assertThat(detail.totalMensuel()).isEqualByComparingTo(detail.prixFormule().add(totalOptions));
    }

    @Test
    void uneOptionEnDoubleNEstFactureeQuUneFois() {
        OptionTarifaire option = OptionTarifaire.ASSISTANCE;

        DetailFormuleResponse detail = calculateur.detail(
                JeuxDeDonnees.profilCa1(), Formule.CONFORT, List.of(option, option), null, GRILLE);

        assertThat(detail.options()).hasSize(1);
        assertThat(detail.totalMensuel()).isEqualByComparingTo(detail.prixFormule().add(GRILLE.prixOptions().get(option)));
    }

    @Test
    void chaqueFranchiseAjusteLeTotal() {
        GRILLE.ajustementsFranchise().forEach((franchise, ajustement) -> {
            DetailFormuleResponse detail = calculateur.detail(
                    JeuxDeDonnees.profilCa1(), Formule.ESSENTIELLE, List.of(), franchise, GRILLE);

            assertThat(detail.ajustementFranchise()).isEqualByComparingTo(ajustement);
            assertThat(detail.totalMensuel()).isEqualByComparingTo(detail.prixFormule().add(ajustement));
        });
    }

    @Test
    void sansFranchiseIlNYaPasDAjustement() {
        DetailFormuleResponse detail = calculateur.detail(JeuxDeDonnees.profilCa1(), Formule.ESSENTIELLE, List.of(), null, GRILLE);

        assertThat(detail.ajustementFranchise()).isZero();
        assertThat(detail.totalMensuel()).isEqualByComparingTo(detail.prixFormule());
    }

    @Test
    void leCatalogueDonneChaqueOptionAuPrixDeLaGrille() {
        assertThat(calculateur.prixDesOptions(GRILLE)).hasSize(OptionTarifaire.values().length)
                .allSatisfy(ligne -> assertThat(ligne.prixMensuel()).isEqualByComparingTo(GRILLE.prixOptions().get(ligne.option())));
    }

    @Test
    void leCatalogueDonneChaqueFranchiseDansLOrdre() {
        assertThat(calculateur.ajustementsDesFranchises(GRILLE)).hasSameSizeAs(GRILLE.ajustementsFranchise().keySet())
                .isSortedAccordingTo(java.util.Comparator.comparingInt(net.checkconsulting.moteurtarifaire.api.dto.AjustementFranchiseResponse::franchise))
                .allSatisfy(ligne -> assertThat(ligne.ajustement()).isEqualByComparingTo(GRILLE.ajustementsFranchise().get(ligne.franchise())));
    }
}
