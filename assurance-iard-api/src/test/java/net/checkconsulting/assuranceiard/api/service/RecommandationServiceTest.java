package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.RecommandationRequest;
import net.checkconsulting.assuranceiard.api.mapper.RecommandationMapperImpl;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecommandationServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-04T10:00:00Z");

    @Test
    void conserveLesReponsesEtLaRecommandation() {
        RecommandationDevisRepository repository = mock(RecommandationDevisRepository.class);
        when(repository.save(any())).thenAnswer(appel -> appel.getArgument(0));
        RecommandationService service = new RecommandationService(
                repository, new RecommandationMapperImpl(), Clock.fixed(MAINTENANT, ZoneOffset.UTC));
        RecommandationRequest demande = new RecommandationRequest(
                25_000, true, false, "neuf", true, false, "protection", "serenite",
                List.of("Votre contenu dépasse 20 000 €"), List.of("objets-valeur", "assistance-depannage"));

        UUID reference = service.enregistrer(demande);

        ArgumentCaptor<RecommandationDevis> capture = ArgumentCaptor.forClass(RecommandationDevis.class);
        verify(repository).save(capture.capture());
        RecommandationDevis enregistree = capture.getValue();
        assertThat(enregistree.getReference()).isEqualTo(reference).isNotNull();
        assertThat(enregistree.getDateCreation()).isEqualTo(MAINTENANT);
        assertThat(enregistree.getValeurContenu()).isEqualTo(25_000);
        assertThat(enregistree.isObjetsValeur()).isTrue();
        assertThat(enregistree.isAppareilsCouteux()).isFalse();
        assertThat(enregistree.getRemboursement()).isEqualTo("neuf");
        assertThat(enregistree.isAssistance()).isTrue();
        assertThat(enregistree.isLitiges()).isFalse();
        assertThat(enregistree.getPriorite()).isEqualTo("protection");
        assertThat(enregistree.getFormule()).isEqualTo("serenite");
        assertThat(enregistree.getRaisons()).containsExactly("Votre contenu dépasse 20 000 €");
        assertThat(enregistree.getOptions()).containsExactly("objets-valeur", "assistance-depannage");
    }
}
