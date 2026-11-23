package net.checkconsulting.moteurtarifaire.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** T-05 : référentiels de tarification auto, rattachés à une version de grille. */
class ReferentielsAutoTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-23T10:00:00Z");

    @Test
    void uneFormuleGardeSesValeurs() {
        RefFormule formule = new RefFormule();
        formule.setId(1L);
        formule.setGrilleId(2L);
        formule.setCode("INTER");
        formule.setLibelle("Intermédiaire");
        formule.setFranchise(new BigDecimal("400.00"));
        formule.setPrixMensuelBase(new BigDecimal("32.50"));
        formule.setOrdre(2);
        formule.setDateCreation(MAINTENANT);
        formule.setDateModification(MAINTENANT);

        assertThat(List.of(formule.getId(), formule.getGrilleId(), formule.getCode(), formule.getLibelle(),
                formule.getFranchise(), formule.getPrixMensuelBase(), formule.getOrdre(), formule.getDateCreation(),
                formule.getDateModification()))
                .containsExactly(1L, 2L, "INTER", "Intermédiaire", new BigDecimal("400.00"),
                        new BigDecimal("32.50"), 2, MAINTENANT, MAINTENANT);
    }

    @Test
    void uneFormuleADesListesVidesParDefaut() {
        RefFormule formule = new RefFormule();

        assertThat(formule.getGarantiesIncluses()).isEmpty();
        assertThat(formule.getNonCouvert()).isEmpty();
        assertThat(formule.getSoumisBonus()).isFalse();
        assertThat(formule.getPartRc()).isNull();
    }

    @Test
    void uneFormuleGardeSesChampsTechniquesAuto() {
        RefFormule formule = new RefFormule();
        formule.setGarantiesIncluses(List.of("RC", "INCENDIE"));
        formule.setNonCouvert(List.of("BRIS_GLACE"));
        formule.setPartRc(new BigDecimal("0.6500"));
        formule.setSoumisBonus(true);

        assertThat(formule.getGarantiesIncluses()).containsExactly("RC", "INCENDIE");
        assertThat(formule.getNonCouvert()).containsExactly("BRIS_GLACE");
        assertThat(formule.getPartRc()).isEqualByComparingTo("0.65");
        assertThat(formule.getSoumisBonus()).isTrue();
    }

    @Test
    void uneOptionEstFacultativeEtSansFormuleCompatibleParDefaut() {
        RefOption option = new RefOption();

        assertThat(option.getObligatoire()).isFalse();
        assertThat(option.getFormulesCompatibles()).isEmpty();
        assertThat(option.getSoumisBonus()).isFalse();
        assertThat(option.getCategorieTaxe()).isNull();

        option.setId(3L);
        option.setGrilleId(2L);
        option.setCode("RC");
        option.setLibelle("Responsabilité civile");
        option.setPrixMensuel(new BigDecimal("12.00"));
        option.setProduit("AUTO");
        option.setObligatoire(true);
        option.setFormulesCompatibles(List.of("TIERS", "INTER", "TR"));
        option.setCategorieTaxe("ASSURANCE");
        option.setSoumisBonus(true);
        option.setDateCreation(MAINTENANT);
        option.setDateModification(MAINTENANT);

        assertThat(List.of(option.getId(), option.getGrilleId(), option.getCode(), option.getLibelle(),
                option.getPrixMensuel(), option.getProduit(), option.getObligatoire(), option.getFormulesCompatibles(),
                option.getCategorieTaxe(), option.getSoumisBonus(),
                option.getDateCreation(), option.getDateModification()))
                .containsExactly(3L, 2L, "RC", "Responsabilité civile", new BigDecimal("12.00"), "AUTO", true,
                        List.of("TIERS", "INTER", "TR"), "ASSURANCE", true, MAINTENANT, MAINTENANT);
    }

    @Test
    void unCoefficientDecritSaTrancheOuSaValeur() {
        RefTarif tarif = new RefTarif();
        tarif.setId(4L);
        tarif.setGrilleId(2L);
        tarif.setCritere("BONUS_MALUS");
        tarif.setBorneMin(new BigDecimal("0.50"));
        tarif.setBorneMax(new BigDecimal("0.79"));
        tarif.setValeur(null);
        tarif.setCoefficient(new BigDecimal("0.850"));
        tarif.setDateCreation(MAINTENANT);
        tarif.setDateModification(MAINTENANT);

        assertThat(tarif.getId()).isEqualTo(4L);
        assertThat(tarif.getGrilleId()).isEqualTo(2L);
        assertThat(tarif.getCritere()).isEqualTo("BONUS_MALUS");
        assertThat(tarif.getBorneMin()).isEqualByComparingTo("0.50");
        assertThat(tarif.getBorneMax()).isEqualByComparingTo("0.79");
        assertThat(tarif.getValeur()).isNull();
        assertThat(tarif.getCoefficient()).isEqualByComparingTo("0.85");
        assertThat(tarif.getDateCreation()).isEqualTo(MAINTENANT);
        assertThat(tarif.getDateModification()).isEqualTo(MAINTENANT);
    }
}
