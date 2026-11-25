package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModeleFicheConseilAutoTest {

    private final DevisAutoRepository devisAutos = mock(DevisAutoRepository.class);
    private final ModeleFicheConseilAuto modele = new ModeleFicheConseilAuto(devisAutos);

    private DevisAuto devisAuto;

    @BeforeEach
    void setUp() {
        devisAuto = new DevisAuto();
        devisAuto.setId(7L);
        devisAuto.setFormuleConseillee("TR");
        devisAuto.setFormuleChoisie("TR");
        devisAuto.setChoixDifferentDuConseil(false);
        devisAuto.setPrixMensuelTtc(new BigDecimal("55.00"));
        devisAuto.setPrixAnnuelTtc(new BigDecimal("660.00"));
        devisAuto.setDateFinValidite(LocalDate.of(2027, 1, 1));
    }

    @Test
    void typeEstFICHE_CONSEIL() {
        assertThat(modele.type()).isEqualTo(TypeDocument.FICHE_CONSEIL);
    }

    @Test
    void convientA_referenceNullRetourneFalse() {
        Document doc = new Document();
        doc.setReference(null);
        assertThat(modele.convientA(doc)).isFalse();
    }

    @Test
    void convientA_referenceNonNumeriqueRetourneFalse() {
        Document doc = new Document();
        doc.setReference("DEVIS-ABC");
        assertThat(modele.convientA(doc)).isFalse();
    }

    @Test
    void convientA_referenceNumeriqueRetourneTrue() {
        Document doc = new Document();
        doc.setReference("7");
        assertThat(modele.convientA(doc)).isTrue();
    }

    @Test
    void rediger_produireUneRubriqueDevis() {
        when(devisAutos.findById(7L)).thenReturn(Optional.of(devisAuto));
        Document doc = new Document();
        doc.setReference("7");

        ContenuDocument contenu = modele.rediger(doc);

        assertThat(contenu.titre()).contains("Fiche conseil");
        assertThat(contenu.numero()).isEqualTo("FC-AUTO-7");
        assertThat(contenu.sections()).isNotEmpty();
    }

    @Test
    void rediger_leveExceptionSiDevisIntrouvable() {
        when(devisAutos.findById(99L)).thenReturn(Optional.empty());
        Document doc = new Document();
        doc.setReference("99");

        assertThatThrownBy(() -> modele.rediger(doc))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("99");
    }

    @Test
    void rediger_choixDifferentDuConseilAvecDate() {
        devisAuto.setFormuleChoisie("TIERS");
        devisAuto.setChoixDifferentDuConseil(true);
        devisAuto.setDateConfirmationChoixDifferent(Instant.parse("2026-11-01T10:00:00Z"));
        when(devisAutos.findById(7L)).thenReturn(Optional.of(devisAuto));
        Document doc = new Document();
        doc.setReference("7");

        ContenuDocument contenu = modele.rediger(doc);

        String texteFormule = contenu.sections().stream()
                .filter(s -> s.titre().equals("Formule choisie"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining(" "));
        assertThat(texteFormule).contains("différent");
    }

    @Test
    void rediger_choixDifferentSansDate() {
        devisAuto.setChoixDifferentDuConseil(true);
        devisAuto.setDateConfirmationChoixDifferent(null);
        when(devisAutos.findById(7L)).thenReturn(Optional.of(devisAuto));
        Document doc = new Document();
        doc.setReference("7");

        ContenuDocument contenu = modele.rediger(doc);

        String texteFormule = contenu.sections().stream()
                .filter(s -> s.titre().equals("Formule choisie"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining(" "));
        assertThat(texteFormule).contains("–");
    }

    @Test
    void rediger_avecOptions() {
        devisAuto.setOptionsChoisies("ASSISTANCE_0KM,VEHICULE_REMPLACEMENT");
        when(devisAutos.findById(7L)).thenReturn(Optional.of(devisAuto));
        Document doc = new Document();
        doc.setReference("7");

        ContenuDocument contenu = modele.rediger(doc);

        String texteOptions = contenu.sections().stream()
                .filter(s -> s.titre().equals("Options choisies"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining(" "));
        assertThat(texteOptions).contains("Assistance");
    }

    @Test
    void rediger_sansOptions() {
        devisAuto.setOptionsChoisies(null);
        when(devisAutos.findById(7L)).thenReturn(Optional.of(devisAuto));
        Document doc = new Document();
        doc.setReference("7");

        ContenuDocument contenu = modele.rediger(doc);

        String texteOptions = contenu.sections().stream()
                .filter(s -> s.titre().equals("Options choisies"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining());
        assertThat(texteOptions).contains("Aucune option");
    }

    @Test
    void rediger_avecValeurVehicule() {
        devisAuto.setValeurVehicule(18500);
        when(devisAutos.findById(7L)).thenReturn(Optional.of(devisAuto));
        Document doc = new Document();
        doc.setReference("7");

        ContenuDocument contenu = modele.rediger(doc);

        String texteBesoins = contenu.sections().stream()
                .filter(s -> s.titre().equals("Vos besoins (étape 2)"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining(" "));
        assertThat(texteBesoins).contains("Valeur véhicule");
    }

    @Test
    void rediger_sansDateFinValidite() {
        devisAuto.setDateFinValidite(null);
        when(devisAutos.findById(7L)).thenReturn(Optional.of(devisAuto));
        Document doc = new Document();
        doc.setReference("7");

        ContenuDocument contenu = modele.rediger(doc);

        String texteDevis = contenu.sections().stream()
                .filter(s -> s.titre().equals("Devis"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining(" "));
        assertThat(texteDevis).contains("30 jours");
    }

    @Test
    void rediger_avecPrixNuls() {
        devisAuto.setPrixMensuelTtc(null);
        devisAuto.setPrixAnnuelTtc(null);
        when(devisAutos.findById(7L)).thenReturn(Optional.of(devisAuto));
        Document doc = new Document();
        doc.setReference("7");

        ContenuDocument contenu = modele.rediger(doc);

        String texteTarif = contenu.sections().stream()
                .filter(s -> s.titre().equals("Tarif"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining(" "));
        assertThat(texteTarif).contains("Prix non calculé");
    }

    @Test
    void rediger_avecPriorite() {
        devisAuto.setPriorite("PRIX");
        when(devisAutos.findById(7L)).thenReturn(Optional.of(devisAuto));
        Document doc = new Document();
        doc.setReference("7");

        ContenuDocument contenu = modele.rediger(doc);

        String texteBesoins = contenu.sections().stream()
                .filter(s -> s.titre().equals("Vos besoins (étape 2)"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining(" "));
        assertThat(texteBesoins).contains("PRIX");
    }

    @Test
    void rediger_avecRaisonsFormule() {
        devisAuto.setRaisonsFormule("Usage professionnel|Véhicule récent");
        when(devisAutos.findById(7L)).thenReturn(Optional.of(devisAuto));
        Document doc = new Document();
        doc.setReference("7");

        ContenuDocument contenu = modele.rediger(doc);

        String texteConseil = contenu.sections().stream()
                .filter(s -> s.titre().equals("Formule conseillée"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining(" "));
        assertThat(texteConseil).contains("professionnel");
    }
}
