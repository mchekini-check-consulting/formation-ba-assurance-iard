package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;

import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import org.junit.jupiter.api.Test;

class ModeleIPIDautoTest {

    private final ModeleIPIDauto modele = new ModeleIPIDauto();

    @Test
    void typeEstIPID() {
        assertThat(modele.type()).isEqualTo(TypeDocument.IPID);
    }

    @Test
    void convientA_referenceNullRetourneFalse() {
        Document doc = new Document();
        doc.setReference(null);
        assertThat(modele.convientA(doc)).isFalse();
    }

    @Test
    void convientA_referenceHorsFormuleRetourneFalse() {
        Document doc = new Document();
        doc.setReference("HABITATION");
        assertThat(modele.convientA(doc)).isFalse();
    }

    @Test
    void convientA_formuleTIERSRetourneTrue() {
        Document doc = new Document();
        doc.setReference("TIERS");
        assertThat(modele.convientA(doc)).isTrue();
    }

    @Test
    void convientA_formuleINTERRetourneTrue() {
        Document doc = new Document();
        doc.setReference("INTER");
        assertThat(modele.convientA(doc)).isTrue();
    }

    @Test
    void convientA_formuleTRRetourneTrue() {
        Document doc = new Document();
        doc.setReference("TR");
        assertThat(modele.convientA(doc)).isTrue();
    }

    @Test
    void rediger_tiersPasDeDommagesNiVol() {
        Document doc = new Document();
        doc.setReference("TIERS");

        ContenuDocument contenu = modele.rediger(doc);

        assertThat(contenu.titre()).contains("Tiers");
        assertThat(contenu.numero()).isEqualTo("IPID-AUTO-TIERS");
        String garanties = contenu.sections().stream()
                .filter(s -> s.titre().equals("Garanties principales"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining(" "));
        assertThat(garanties).contains("Responsabilité civile");
        assertThat(garanties).doesNotContain("Vol");
        String franchise = contenu.sections().stream()
                .filter(s -> s.titre().equals("Franchise"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining());
        assertThat(franchise).contains("Tiers");
    }

    @Test
    void rediger_interInclutVolEtGlace() {
        Document doc = new Document();
        doc.setReference("INTER");

        ContenuDocument contenu = modele.rediger(doc);

        assertThat(contenu.titre()).contains("Intermédiaire");
        String garanties = contenu.sections().stream()
                .filter(s -> s.titre().equals("Garanties principales"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining(" "));
        assertThat(garanties).contains("Vol");
        assertThat(garanties).doesNotContain("tous accidents");
        String franchise = contenu.sections().stream()
                .filter(s -> s.titre().equals("Franchise"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining());
        assertThat(franchise).contains("400");
    }

    @Test
    void rediger_trInclutDommagesTousAccidents() {
        Document doc = new Document();
        doc.setReference("TR");

        ContenuDocument contenu = modele.rediger(doc);

        assertThat(contenu.titre()).contains("Tous risques");
        String garanties = contenu.sections().stream()
                .filter(s -> s.titre().equals("Garanties principales"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining(" "));
        assertThat(garanties).contains("tous accidents");
        String franchise = contenu.sections().stream()
                .filter(s -> s.titre().equals("Franchise"))
                .flatMap(s -> s.lignes().stream())
                .collect(java.util.stream.Collectors.joining());
        assertThat(franchise).contains("500");
    }
}
