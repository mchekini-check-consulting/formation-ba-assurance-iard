package net.checkconsulting.assuranceiard.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

/** T-05 : entités des nouvelles tables (valeurs par défaut alignées sur les migrations, horodatages). */
class EntitesT05Test {

    static Stream<Class<? extends EntiteHorodatee>> entites() {
        return Stream.of(RefVehicule.class, RefPlaqueTest.class, Vehicule.class, Conducteur.class, DevisAuto.class,
                DevisOption.class, ContratOption.class, HistoriqueStatutContrat.class, Document.class,
                Echeance.class, Facture.class, Avenant.class, AvenantLigne.class, PreuveSignature.class,
                DeclarationFva.class, ResiliationHamon.class, Sinistre.class, PieceJointe.class);
    }

    @ParameterizedTest
    @MethodSource("entites")
    void chaqueChampSeLitCommeIlAEteEcrit(Class<? extends EntiteHorodatee> type) throws Exception {
        EntiteHorodatee entite = type.getDeclaredConstructor().newInstance();
        for (PropertyDescriptor propriete : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
            if (propriete.getWriteMethod() == null) {
                continue;
            }
            Object valeur = exemple(propriete.getPropertyType());
            propriete.getWriteMethod().invoke(entite, valeur);
            assertThat(propriete.getReadMethod().invoke(entite)).as(propriete.getName()).isEqualTo(valeur);
        }
    }

    @ParameterizedTest
    @MethodSource("entites")
    void lesDatesSontRenseigneesALaCreationPuisALaModification(Class<? extends EntiteHorodatee> type)
            throws ReflectiveOperationException {
        EntiteHorodatee entite = type.getDeclaredConstructor().newInstance();
        Instant avant = Instant.now();

        entite.avantCreation();

        assertThat(entite.getDateCreation()).isAfterOrEqualTo(avant);
        assertThat(entite.getDateModification()).isEqualTo(entite.getDateCreation());

        Instant creation = Instant.parse("2026-01-01T00:00:00Z");
        entite.setDateCreation(creation);
        entite.avantModification();

        assertThat(entite.getDateCreation()).isEqualTo(creation);
        assertThat(entite.getDateModification()).isAfter(creation);
    }

    @Test
    void uneDateDeCreationDejaFixeeEstConservee() {
        Echeance echeance = new Echeance();
        Instant fixee = Instant.parse("2026-02-01T10:00:00Z");
        echeance.setDateCreation(fixee);

        echeance.avantCreation();

        assertThat(echeance.getDateCreation()).isEqualTo(fixee);
    }

    @Test
    void lesValeursParDefautSontCellesDesMigrations() {
        assertThat(new Document().getStatut()).isEqualTo("EN_VIGUEUR");
        assertThat(new Document().getVersion()).isEqualTo(1);
        assertThat(new Document().getStocke()).isFalse();
        assertThat(new Echeance().getStatut()).isEqualTo("A_VENIR");
        assertThat(new Avenant().getStatut()).isEqualTo("EN_ATTENTE");
        assertThat(new DeclarationFva().getStatut()).isEqualTo("A_ENVOYER");
        assertThat(new ResiliationHamon().getStatut()).isEqualTo("A_ENVOYER");
        assertThat(new ResiliationHamon().getNbEssais()).isZero();
        assertThat(new PieceJointe().getStatut()).isEqualTo("STOCKEE");
        assertThat(new Conducteur().getResiliationAssureur()).isEqualTo("AUCUNE");
        assertThat(new Conducteur().getEstSouscripteur()).isTrue();
        assertThat(new Vehicule().getNonImmatricule()).isFalse();
        assertThat(new ContratOption().getObligatoire()).isFalse();
        assertThat(new DevisAuto().getAssurePlusUnAn()).isFalse();
        assertThat(new Sinistre().getStatut()).isEqualTo("BROUILLON");
        assertThat(new Sinistre().getDeclarationHonneur()).isFalse();
    }

    @Test
    void unBrouillonDeSinistreExpireTrenteJoursApresSaCreation() {
        Sinistre sinistre = new Sinistre();

        sinistre.avantCreation();
        sinistre.expirationDuBrouillon();

        assertThat(sinistre.getBrouillonExpireLe())
                .isEqualTo(sinistre.getDateCreation().plus(Duration.ofDays(30)));
    }

    @Test
    void uneExpirationDejaFixeeEstConservee() {
        Sinistre sinistre = new Sinistre();
        Instant fixee = Instant.parse("2026-03-01T00:00:00Z");
        sinistre.setBrouillonExpireLe(fixee);

        sinistre.avantCreation();
        sinistre.expirationDuBrouillon();

        assertThat(sinistre.getBrouillonExpireLe()).isEqualTo(fixee);
    }

    @Test
    void lesNouveauxChampsDuDevisEtDuMandatOntLesValeursParDefautDesMigrations() {
        DevisEnregistre devis = new DevisEnregistre();
        assertThat(devis.getProduit()).isEqualTo(DevisEnregistre.PRODUIT_HABITATION);
        assertThat(devis.getStatutDevis()).isEqualTo("EN_COURS");

        MandatSepa mandat = new MandatSepa();
        assertThat(mandat.getStatut()).isEqualTo("ACTIF");
        mandat.setCompteId(7L);
        mandat.setBic("AGRIFRPPXXX");
        mandat.setBanque("Banque");
        mandat.setIbanMasque("•••• 4821");
        mandat.setDateModification(Instant.EPOCH);
        devis.setFormuleConseillee("confort");
        devis.setChoixDifferentConseil(true);
        devis.setReponsesBesoins("{}");
        devis.setDateModification(Instant.EPOCH);
        assertThat(List.of(mandat.getCompteId(), mandat.getBic(), mandat.getBanque(), mandat.getIbanMasque(),
                mandat.getDateModification(), devis.getFormuleConseillee(), devis.getChoixDifferentConseil(),
                devis.getReponsesBesoins(), devis.getDateModification()))
                .containsExactly(7L, "AGRIFRPPXXX", "Banque", "•••• 4821", Instant.EPOCH, "confort", true, "{}",
                        Instant.EPOCH);
    }

    private static Object exemple(Class<?> type) throws InvocationTargetException {
        if (type == Long.class) {
            return 42L;
        }
        if (type == Integer.class) {
            return 3;
        }
        if (type == Boolean.class) {
            return Boolean.TRUE;
        }
        if (type == BigDecimal.class) {
            return new BigDecimal("12.34");
        }
        if (type == LocalDate.class) {
            return LocalDate.of(2026, 5, 10);
        }
        if (type == LocalTime.class) {
            return LocalTime.of(8, 30);
        }
        if (type == Instant.class) {
            return Instant.parse("2026-05-10T08:30:00Z");
        }
        if (type == String.class) {
            return "valeur";
        }
        throw new InvocationTargetException(new IllegalStateException("Type sans exemple : " + type));
    }
}
