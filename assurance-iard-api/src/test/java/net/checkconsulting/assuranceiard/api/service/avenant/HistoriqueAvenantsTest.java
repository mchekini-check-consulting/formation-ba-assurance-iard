package net.checkconsulting.assuranceiard.api.service.avenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.AvenantResponse;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import net.checkconsulting.assuranceiard.api.model.AvenantLigne;
import net.checkconsulting.assuranceiard.api.repository.AvenantLigneRepository;
import net.checkconsulting.assuranceiard.api.repository.AvenantRepository;
import org.junit.jupiter.api.Test;

/** US-23 : historique des avenants (numéro, date d'effet, garanties changées, statut) et avenant en attente (AC5). */
class HistoriqueAvenantsTest {

    /** 26/11/2026 à 23h30 à Paris (22h30 UTC) : le lendemain est le 27/11. */
    static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-26T22:30:00Z"), ZoneOffset.UTC);

    private final AvenantRepository avenants = mock(AvenantRepository.class);
    private final AvenantLigneRepository lignes = mock(AvenantLigneRepository.class);
    private final HistoriqueAvenants historique = new HistoriqueAvenants(avenants, lignes, HORLOGE);

    static Avenant avenant(long id, int rang, String statut, LocalDate effet, String avant, String apres) {
        Avenant avenant = new Avenant();
        avenant.setId(id);
        avenant.setNumero(rang);
        avenant.setContratId(3L);
        avenant.setStatut(statut);
        avenant.setDateEffet(effet);
        avenant.setPrimeAvantTtc(new BigDecimal(avant));
        avenant.setPrimeApresTtc(new BigDecimal(apres));
        avenant.setDateCreation(Instant.parse("2026-11-20T10:00:00Z"));
        return avenant;
    }

    static AvenantLigne ligne(long avenantId, String code, String action, String prix) {
        AvenantLigne ligne = new AvenantLigne();
        ligne.setAvenantId(avenantId);
        ligne.setCodeOption(code);
        ligne.setAction(action);
        ligne.setPrixMensuelTtc(new BigDecimal(prix));
        return ligne;
    }

    @Test
    void historiqueLePlusRecentDAbordAvecSesGarantiesChangees() {
        Avenant accepte = avenant(5L, 1, "ACCEPTE", LocalDate.of(2026, 10, 2), "45.00", "47.50");
        Avenant enAttente = avenant(9L, 2, "EN_ATTENTE", LocalDate.of(2026, 11, 1), "47.50", "48.50");
        when(avenants.findByContratIdOrderByNumeroDesc(3L)).thenReturn(List.of(enAttente, accepte));
        when(lignes.findByAvenantIdInOrderById(List.of(9L, 5L))).thenReturn(List.of(
                ligne(5L, "PROTECTION_JURIDIQUE", "AJOUT", "2.50"),
                ligne(9L, "ASSISTANCE_0KM", "AJOUT", "3.50"),
                ligne(9L, "PROTECTION_JURIDIQUE", "RETRAIT", "2.50")));

        List<AvenantResponse> reponse = historique.duContrat(3L);

        assertThat(reponse).extracting(AvenantResponse::numero).containsExactly("AV-2026-000009", "AV-2026-000005");
        assertThat(reponse.getFirst()).satisfies(avenant -> {
            assertThat(avenant.rang()).isEqualTo(2);
            assertThat(avenant.statut()).isEqualTo("EN_ATTENTE");
            // En attente : prendra effet le lendemain de son acceptation (aujourd'hui → 27/11).
            assertThat(avenant.dateEffet()).isEqualTo(LocalDate.of(2026, 11, 27));
            assertThat(avenant.ajouts()).extracting(AvenantResponse.Garantie::libelle)
                    .containsExactly("Assistance dès 0 km");
            assertThat(avenant.retraits()).extracting(AvenantResponse.Garantie::code)
                    .containsExactly("PROTECTION_JURIDIQUE");
        });
        // Accepté : sa date d'effet enregistrée.
        assertThat(reponse.get(1).dateEffet()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(reponse.get(1).nouvelleCotisationMensuelle()).isEqualByComparingTo("47.50");
    }

    @Test
    void sansAvenantHistoriqueVideEtRienEnAttente() {
        when(avenants.findByContratIdOrderByNumeroDesc(3L)).thenReturn(List.of());
        when(avenants.findFirstByContratIdAndStatut(3L, "EN_ATTENTE")).thenReturn(Optional.empty());

        assertThat(historique.duContrat(3L)).isEmpty();
        assertThat(historique.enAttente(3L)).isEmpty();
    }

    @Test
    void ac5AvenantEnAttente() {
        Avenant enAttente = avenant(9L, 1, "EN_ATTENTE", LocalDate.of(2026, 8, 1), "18.50", "21.00");
        when(avenants.findFirstByContratIdAndStatut(3L, "EN_ATTENTE")).thenReturn(Optional.of(enAttente));
        when(lignes.findByAvenantIdOrderById(9L)).thenReturn(List.of(ligne(9L, "objets-valeur", "AJOUT", "2.50")));

        assertThat(historique.enAttente(3L)).hasValueSatisfying(avenant -> {
            assertThat(avenant.ancienneCotisationMensuelle()).isEqualByComparingTo("18.50");
            assertThat(avenant.nouvelleCotisationMensuelle()).isEqualByComparingTo("21.00");
            assertThat(avenant.dateEffet()).isEqualTo(historique.lendemain());
            assertThat(avenant.accepteLe()).isNull();
        });
    }
}
