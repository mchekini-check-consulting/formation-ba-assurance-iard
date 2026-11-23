package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.checkconsulting.assuranceiard.api.config.DocumentsProperties;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import org.junit.jupiter.api.Test;

/** T-03 : un test par modèle (conditions particulières habitation et auto, conditions générales provisoires). */
class ModelesDocumentsTest {

    static final DocumentsProperties REGLAGES = new DocumentsProperties(
            new DocumentsProperties.Assureur("Assur'Easy SA", "10 rue de la Recette", "RCS fictif", "ACPR"),
            Map.of("confort", List.of("Dégâts des eaux", "Vol"), "TIERS", List.of("Responsabilité civile"),
                    "INTER", List.of("Bris de glace"), "TR", List.of("Tous accidents")),
            "Exclusions : voir les conditions générales.", null, null);

    private final DonneesContratRepository contrats = mock(DonneesContratRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final ModeleConditionsParticulieresHabitation habitation =
            new ModeleConditionsParticulieresHabitation(contrats, documents, REGLAGES);
    private final ModeleConditionsParticulieresAuto auto =
            new ModeleConditionsParticulieresAuto(contrats, documents, REGLAGES);
    private final ModeleConditionsGenerales generales = new ModeleConditionsGenerales(REGLAGES);

    static DonneesContrat contratHabitation() {
        return new DonneesContrat(1L, "AE-2026-000001", DonneesContrat.HABITATION, "confort",
                new DonneesContrat.Souscripteur("madame", "Alice", "Martin"),
                new DonneesContrat.Logement("appartement", 55, "3 rue de la Recette", "33000", "Bordeaux"),
                new DonneesContrat.Vehicule(null, null, null, null),
                new DonneesContrat.Conducteur(null, null),
                new DonneesContrat.Tarif(new BigDecimal("300.00"), new BigDecimal("18.50"), new BigDecimal("222.00"),
                        LocalDate.of(2026, 3, 9), null),
                new DonneesContrat.Paiement("MENSUEL", 5, "•••• 4821", "AE-RUM-2026-K7M2QX"),
                null,
                List.of(new DonneesContrat.Option("objets-valeur", null),
                        new DonneesContrat.Option("BRIS_GLACE", new BigDecimal("2.50"))));
    }

    static DonneesContrat contratAuto() {
        return new DonneesContrat(2L, "AE-2026-000002", DonneesContrat.AUTO, "TR",
                new DonneesContrat.Souscripteur("monsieur", "David", "Leroy"),
                new DonneesContrat.Logement(null, null, null, null, null),
                new DonneesContrat.Vehicule("AB-123-CD", "Renault", "Clio", "V TCe 90"),
                new DonneesContrat.Conducteur(new BigDecimal("0.68"), true),
                new DonneesContrat.Tarif(new BigDecimal("500.00"), new BigDecimal("54.90"), new BigDecimal("658.80"),
                        LocalDate.of(2026, 12, 1), LocalDate.of(2027, 6, 30)),
                new DonneesContrat.Paiement("ANNUEL", null, null, null),
                new DonneesContrat.AncienContrat("Assureur précédent", "ANC-123456"),
                List.of());
    }

    static Document document(TypeDocument type, Long contratId, String reference, int version) {
        Document document = new Document();
        document.setId(10L);
        document.setType(type.name());
        document.setContratId(contratId);
        document.setReference(reference);
        document.setVersion(version);
        return document;
    }

    /** Le texte de tout le PDF, tel que le client le lira. */
    static String texte(ContenuDocument contenu) throws IOException {
        byte[] pdf = new GabaritPdf().rendre(contenu, new GabaritPdf.Pied("Type", contenu.numero(), 1,
                LocalDate.of(2026, 11, 23)));
        StringBuilder tout = new StringBuilder();
        for (int page = 1; page <= GabaritPdfTest.pages(pdf); page++) {
            tout.append(GabaritPdfTest.page(pdf, page));
        }
        return tout.toString();
    }

    private static String lignes(ContenuDocument contenu) {
        return contenu.sections().stream()
                .map(section -> section.titre() + "\n" + String.join("\n", section.lignes()))
                .collect(Collectors.joining("\n"));
    }

    @Test
    void conditionsParticulieresHabitation() throws IOException {
        when(contrats.lire(1L)).thenReturn(Optional.of(contratHabitation()));
        Document cg = document(TypeDocument.CONDITIONS_GENERALES, null, "HABITATION", 3);
        when(documents.enVigueur(0, "CONDITIONS_GENERALES", "HABITATION")).thenReturn(Optional.of(cg));

        ContenuDocument contenu = habitation.rediger(document(TypeDocument.CONDITIONS_PARTICULIERES, 1L, null, 2));

        assertThat(contenu.titre()).isEqualTo("Conditions particulières - Assurance habitation");
        assertThat(contenu.numero()).isEqualTo("AE-2026-000001");
        assertThat(contenu.provisoire()).isFalse();
        assertThat(lignes(contenu)).contains(
                "Contrat n° AE-2026-000001 - version 2",
                "Date d'effet : 09/03/2026",
                "Échéance principale : le 09/03 de chaque année",
                "Madame Alice Martin",
                "Appartement de 55 m²", "3 rue de la Recette, 33000 Bordeaux",
                "Formule Confort", "• Dégâts des eaux", "• Vol",
                "• Objets de valeur", "• Bris de glace : 2,50 € par mois",
                "300,00 € par sinistre",
                "18,50 € par mois", "222,00 € par an",
                "Prélèvement mensuel, le 5 de chaque mois", "IBAN •••• 4821", "AE-RUM-2026-K7M2QX",
                "réf. CG-HABITATION, version 3", "Exclusions : voir les conditions générales.", "L112-2 et L112-4");
        assertThat(texte(contenu)).contains("IBAN •••• 4821").contains("Madame Alice Martin");
    }

    @Test
    void conditionsParticulieresAuto() {
        when(contrats.lire(2L)).thenReturn(Optional.of(contratAuto()));
        when(documents.enVigueur(0, "CONDITIONS_GENERALES", "AUTO")).thenReturn(Optional.empty());

        ContenuDocument contenu = auto.rediger(document(TypeDocument.CONDITIONS_PARTICULIERES, 2L, null, 1));

        assertThat(contenu.titre()).isEqualTo("Conditions particulières - Assurance auto");
        assertThat(lignes(contenu)).contains(
                "Monsieur David Leroy",
                "Renault Clio V TCe 90", "Immatriculation : AB-123-CD",
                "Le souscripteur", "bonus-malus) : 0,68",
                "contrat n° ANC-123456 souscrit auprès de Assureur précédent (loi Hamon)",
                "Formule Tous risques", "• Tous accidents",
                "Aucune option souscrite",
                "Fin du contrat : 30/06/2027",
                "Prélèvement annuel", "Mandat de prélèvement SEPA pas encore signé",
                "réf. CG-AUTO, dernière version");
    }

    @Test
    void unVehiculeSansImmatriculationNiBonusMalus() {
        DonneesContrat base = contratAuto();
        DonneesContrat contrat = new DonneesContrat(base.id(), base.numero(), base.produit(), "INCONNUE",
                base.souscripteur(), base.logement(), new DonneesContrat.Vehicule(null, "Peugeot", "208", null),
                new DonneesContrat.Conducteur(null, false), base.tarif(),
                new DonneesContrat.Paiement("MENSUEL", null, null, null), null, base.options());
        when(contrats.lire(2L)).thenReturn(Optional.of(contrat));

        String texte = lignes(auto.rediger(document(TypeDocument.CONDITIONS_PARTICULIERES, 2L, null, 1)));

        assertThat(texte).contains("Peugeot 208\nVéhicule pas encore immatriculé", "Conducteur désigné au contrat",
                "bonus-malus) : non renseigné", "• voir les conditions générales", "Prélèvement mensuel\n")
                .doesNotContain("Ancien contrat résilié");
    }

    @Test
    void chaqueModeleDeConditionsParticulieresNeConvientQuASonProduit() {
        when(contrats.produit(1L)).thenReturn(Optional.of(DonneesContrat.HABITATION));
        when(contrats.produit(2L)).thenReturn(Optional.of(DonneesContrat.AUTO));
        Document cpHabitation = document(TypeDocument.CONDITIONS_PARTICULIERES, 1L, null, 1);
        Document cpAuto = document(TypeDocument.CONDITIONS_PARTICULIERES, 2L, null, 1);
        Document sansContrat = document(TypeDocument.CONDITIONS_PARTICULIERES, null, "X", 1);

        assertThat(habitation.convientA(cpHabitation)).isTrue();
        assertThat(habitation.convientA(cpAuto)).isFalse();
        assertThat(auto.convientA(cpAuto)).isTrue();
        assertThat(auto.convientA(sansContrat)).isFalse();
        assertThat(habitation.type()).isEqualTo(TypeDocument.CONDITIONS_PARTICULIERES);
    }

    @Test
    void unContratIntrouvableEmpecheLaRedaction() {
        when(contrats.lire(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitation.rediger(document(TypeDocument.CONDITIONS_PARTICULIERES, 9L, null, 1)))
                .isInstanceOf(IllegalStateException.class).hasMessageNotContaining("Alice");
    }

    @Test
    void conditionsGeneralesProvisoiresParProduit() throws IOException {
        ContenuDocument contenu = generales.rediger(document(TypeDocument.CONDITIONS_GENERALES, null, "AUTO", 1));

        assertThat(contenu.provisoire()).isTrue();
        assertThat(contenu.numero()).isEqualTo("CG-AUTO");
        assertThat(contenu.titre()).isEqualTo("Conditions générales - Assurance auto");
        assertThat(lignes(contenu)).contains("Formule Tiers : Responsabilité civile",
                "Formule Intermédiaire : Bris de glace", "Formule Tous risques : Tous accidents");
        assertThat(texte(contenu)).contains(GabaritPdf.MENTION_PROVISOIRE);
        assertThat(generales.rediger(document(TypeDocument.CONDITIONS_GENERALES, null, "HABITATION", 1)).titre())
                .isEqualTo("Conditions générales - Assurance habitation");

        assertThat(generales.convientA(document(TypeDocument.CONDITIONS_GENERALES, null, "AUTO", 1))).isTrue();
        assertThat(generales.convientA(document(TypeDocument.CONDITIONS_GENERALES, null, "MOTO", 1))).isFalse();
        assertThat(generales.convientA(document(TypeDocument.CONDITIONS_GENERALES, 1L, null, 1))).isFalse();
    }

    @Test
    void lesReglagesOntDesValeursParDefaut() {
        assertThat(REGLAGES.tailleMax().toMegabytes()).isEqualTo(5);
        assertThat(REGLAGES.reprise().active()).isTrue();
        assertThat(REGLAGES.reprise().lot()).isEqualTo(50);
        assertThat(REGLAGES.garantiesDe(null)).isEmpty();
        assertThat(REGLAGES.garantiesDe("inconnue")).isEmpty();
        assertThat(new DocumentsProperties(null, null, null, null, null).garanties()).isEmpty();
    }
}
