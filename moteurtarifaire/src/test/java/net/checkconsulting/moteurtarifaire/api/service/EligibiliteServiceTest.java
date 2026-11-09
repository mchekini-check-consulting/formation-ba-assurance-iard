package net.checkconsulting.moteurtarifaire.api.service;

import static net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees.GRILLE;
import static org.assertj.core.api.Assertions.assertThat;

import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.enums.ResiliationAnterieure;
import net.checkconsulting.moteurtarifaire.api.enums.StatutOccupant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EligibiliteServiceTest {

    private final EligibiliteService service = new EligibiliteService();

    @Test
    void unProprietaireNonOccupantEstOrienteVersUnConseiller() {
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder().statut(StatutOccupant.PROPRIETAIRE_NON_OCCUPANT).build();

        assertThat(service.motifDeRefus(profil, GRILLE.refus()))
                .contains(GRILLE.refus().statuts().get(StatutOccupant.PROPRIETAIRE_NON_OCCUPANT));
    }

    @Test
    void uneResiliationPourNonPaiementEstOrienteeVersUnConseiller() {
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder().resiliationAnterieure(ResiliationAnterieure.NON_PAIEMENT).build();

        assertThat(service.motifDeRefus(profil, GRILLE.refus()))
                .contains(GRILLE.refus().resiliations().get(ResiliationAnterieure.NON_PAIEMENT));
    }

    @ParameterizedTest
    @EnumSource(value = ResiliationAnterieure.class, names = "NON_PAIEMENT", mode = EnumSource.Mode.EXCLUDE)
    void lesAutresResiliationsRestentTarifables(ResiliationAnterieure resiliation) {
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder().resiliationAnterieure(resiliation).build();

        assertThat(service.motifDeRefus(profil, GRILLE.refus())).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = StatutOccupant.class, names = "PROPRIETAIRE_NON_OCCUPANT", mode = EnumSource.Mode.EXCLUDE)
    void lesAutresStatutsRestentTarifables(StatutOccupant statut) {
        ProfilRisque profil = JeuxDeDonnees.profilCa1().toBuilder().statut(statut).build();

        assertThat(service.motifDeRefus(profil, GRILLE.refus())).isEmpty();
    }
}
