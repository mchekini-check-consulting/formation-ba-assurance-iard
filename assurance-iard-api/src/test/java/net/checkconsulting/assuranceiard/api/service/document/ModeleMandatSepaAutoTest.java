package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ModeleMandatSepaAutoTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final DonneesContratRepository contrats = mock(DonneesContratRepository.class);
    private final ModeleMandatSepaAuto modele = new ModeleMandatSepaAuto(jdbc, contrats);

    @Test
    void typeEstMANDAT_SEPA() {
        assertThat(modele.type()).isEqualTo(TypeDocument.MANDAT_SEPA);
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
}
