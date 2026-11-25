package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.config.FacturationProperties;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.Facture;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.repository.FactureRepository;
import net.checkconsulting.assuranceiard.api.service.facture.CalculFacture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModeleFactureTest {

    private static final FacturationProperties REGLAGES = new FacturationProperties(
            Map.of("HABITATION", new BigDecimal("0.09"), "AUTO", new BigDecimal("0.18")),
            Map.of("protection-juridique", new BigDecimal("0.134")));

    @Mock
    private FactureRepository factures;

    @Mock
    private DonneesContratRepository contrats;

    private ModeleFacture modele;

    @BeforeEach
    void preparer() {
        modele = new ModeleFacture(factures, contrats, new CalculFacture(REGLAGES));
    }

    private static Facture facture() {
        Facture facture = new Facture();
        facture.setId(42L);
        facture.setNumero("FAC-2026-000042");
        facture.setContratId(5L);
        facture.setPeriodeDebut(LocalDate.of(2026, 11, 10));
        facture.setPeriodeFin(LocalDate.of(2026, 12, 9));
        facture.setMontantHt(new BigDecimal("16.86"));
        facture.setTaxe(new BigDecimal("1.64"));
        facture.setMontantTtc(new BigDecimal("18.50"));
        facture.setGenereeLe(Instant.parse("2026-11-25T10:00:00Z"));
        return facture;
    }

    private static DonneesContrat contrat() {
        return new DonneesContrat(5L, "AE-RECETTE-1", "HABITATION", "confort",
                new DonneesContrat.Souscripteur("madame", "Alice", "Martin"),
                new DonneesContrat.Logement("appartement", 45, "1 rue A", "75001", "Paris"),
                new DonneesContrat.Vehicule(null, null, null, null), new DonneesContrat.Conducteur(null, null),
                new DonneesContrat.Tarif(new BigDecimal("300"), new BigDecimal("18.50"), new BigDecimal("222.00"),
                        LocalDate.of(2026, 3, 1), null),
                new DonneesContrat.Paiement("MENSUEL", 10, "•••• 4821", "RUM"), null,
                List.of(new DonneesContrat.Option("protection-juridique", new BigDecimal("4.00"))));
    }

    private static Document document(String reference) {
        Document document = new Document();
        document.setId(9L);
        document.setContratId(5L);
        document.setType(TypeDocument.FACTURE.name());
        document.setReference(reference);
        return document;
    }

    @Test
    void ac3ContenuObligatoireDeLaFacture() {
        when(factures.findByNumero("FAC-2026-000042")).thenReturn(Optional.of(facture()));
        when(contrats.lire(5L)).thenReturn(Optional.of(contrat()));

        ContenuDocument contenu = modele.rediger(document("FAC-2026-000042"));

        assertThat(modele.type()).isEqualTo(TypeDocument.FACTURE);
        assertThat(contenu.titre()).isEqualTo("Facture");
        assertThat(contenu.numero()).isEqualTo("FAC-2026-000042");
        String texte = String.join("\n", contenu.sections().stream()
                .flatMap(section -> java.util.stream.Stream.concat(java.util.stream.Stream.of(section.titre()),
                        section.lignes().stream()))
                .toList());
        assertThat(texte).contains("Facture n° FAC-2026-000042", "Date de la facture : 25/11/2026",
                "Madame Alice Martin", "Contrat n° AE-RECETTE-1", "Assurance habitation, formule Confort",
                "Du 10/11/2026 au 09/12/2026",
                "Formule Confort (garanties de base) : prime HT 13,30 € ; TSCA 9 % 1,20 € ; TTC 14,50 €",
                "Protection juridique : prime HT 3,53 € ; TSCA 13,4 % 0,47 € ; TTC 4,00 €",
                "Prime hors taxe : 16,83 €", "Taxe sur les conventions d'assurance (TSCA) : 1,67 €",
                "Total TTC : 18,50 €", "Exonéré de TVA (article 261 C du CGI)");
    }

    @Test
    void factureDesDonneesDeTestRetrouveeParSonDocument() {
        when(factures.findFirstByDocumentId(9L)).thenReturn(Optional.of(facture()));
        when(contrats.lire(5L)).thenReturn(Optional.of(contrat()));

        assertThat(modele.rediger(document(null)).numero()).isEqualTo("FAC-2026-000042");
    }

    @Test
    void factureOuContratIntrouvable() {
        when(factures.findByNumero("FAC-X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> modele.rediger(document("FAC-X"))).isInstanceOf(IllegalStateException.class);

        when(factures.findByNumero("FAC-2026-000042")).thenReturn(Optional.of(facture()));
        when(contrats.lire(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> modele.rediger(document("FAC-2026-000042")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pourcentageALaFrancaise() {
        assertThat(ModeleFacture.pourcentage(new BigDecimal("0.09"))).isEqualTo("9 %");
        assertThat(ModeleFacture.pourcentage(new BigDecimal("0.134"))).isEqualTo("13,4 %");
        assertThat(ModeleFacture.pourcentage(new BigDecimal("0.18"))).isEqualTo("18 %");
    }
}
