package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import org.junit.jupiter.api.Test;

class ModeleMemoVehiculeAssureTest {

    private final DonneesContratRepository contrats = mock(DonneesContratRepository.class);
    private final ModeleMemoVehiculeAssure modele = new ModeleMemoVehiculeAssure(contrats);

    private static DonneesContrat contratAuto(String immatriculation) {
        return new DonneesContrat(1L, "AUTO-2024-00001", "AUTO", "TIERS",
                new DonneesContrat.Souscripteur("M.", "Jean", "Dupont"),
                null,
                new DonneesContrat.Vehicule(immatriculation, "Renault", "Clio", "1.0 TCe 90"),
                new DonneesContrat.Conducteur(new BigDecimal("0.80"), null),
                new DonneesContrat.Tarif(new BigDecimal("300.00"), new BigDecimal("42.50"),
                        new BigDecimal("510.00"), LocalDate.of(2024, 2, 1), LocalDate.of(2025, 1, 31)),
                new DonneesContrat.Paiement("MENSUEL", 5, "FR76 •••• 1234", "RUM-001"),
                null,
                List.of());
    }

    @Test
    void typeEstMEMO_VEHICULE() {
        assertThat(modele.type()).isEqualTo(TypeDocument.MEMO_VEHICULE);
    }

    @Test
    void nepasConvenirSiContratIdNull() {
        Document doc = new Document();
        doc.setContratId(null);
        assertThat(modele.convientA(doc)).isFalse();
    }

    @Test
    void nepasConvenirSiContratHabitation() {
        Document doc = new Document();
        doc.setContratId(42L);
        when(contrats.produit(42L)).thenReturn(Optional.of("HABITATION"));
        assertThat(modele.convientA(doc)).isFalse();
    }

    @Test
    void convenirSiContratAuto() {
        Document doc = new Document();
        doc.setContratId(99L);
        when(contrats.produit(99L)).thenReturn(Optional.of("AUTO"));
        assertThat(modele.convientA(doc)).isTrue();
    }

    @Test
    void redigerContientLeVehiculeEtLeContrat() {
        Document doc = new Document();
        doc.setContratId(1L);
        when(contrats.lire(1L)).thenReturn(Optional.of(contratAuto("AB-123-CD")));

        ContenuDocument contenu = modele.rediger(doc);

        assertThat(contenu.titre()).isEqualTo("Mémo véhicule assuré");
        assertThat(contenu.numero()).isEqualTo("AUTO-2024-00001");
        String texte = contenu.sections().stream()
                .flatMap(s -> s.lignes().stream())
                .reduce("", (a, b) -> a + " " + b);
        assertThat(texte).contains("Renault", "Clio", "AB-123-CD", "Tiers", "01/02/2024");
    }

    @Test
    void redigerSansImmatriculationAffichePasEncore() {
        Document doc = new Document();
        doc.setContratId(1L);
        when(contrats.lire(1L)).thenReturn(Optional.of(contratAuto(null)));

        ContenuDocument contenu = modele.rediger(doc);

        String texte = contenu.sections().stream()
                .flatMap(s -> s.lignes().stream())
                .reduce("", (a, b) -> a + " " + b);
        assertThat(texte).contains("Pas encore immatriculé");
    }

    @Test
    void redigerInclutLaPeriodeDeValidite() {
        Document doc = new Document();
        doc.setContratId(1L);
        when(contrats.lire(1L)).thenReturn(Optional.of(contratAuto("AB-123-CD")));

        ContenuDocument contenu = modele.rediger(doc);

        assertThat(contenu.sections()).anyMatch(s -> s.titre().equals("Période de validité"));
        String periodeTexte = contenu.sections().stream()
                .filter(s -> s.titre().equals("Période de validité"))
                .flatMap(s -> s.lignes().stream())
                .findFirst().orElse("");
        assertThat(periodeTexte).contains("01/02/2024", "31/01/2025");
    }

    @Test
    void redigerAvecFranchise() {
        Document doc = new Document();
        doc.setContratId(1L);
        when(contrats.lire(1L)).thenReturn(Optional.of(contratAuto("AB-123-CD")));

        ContenuDocument contenu = modele.rediger(doc);

        assertThat(contenu.sections()).anyMatch(s -> s.titre().equals("Franchise"));
        String franchise = contenu.sections().stream()
                .filter(s -> s.titre().equals("Franchise"))
                .flatMap(s -> s.lignes().stream())
                .findFirst().orElse("");
        assertThat(franchise).contains("300");
    }
}
