package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.InformationsAutoRequest;
import net.checkconsulting.assuranceiard.api.dto.InformationsAutoResponse;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.exception.VehiculeIntrouvableException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Conducteur;
import net.checkconsulting.assuranceiard.api.model.DevisAuto;
import net.checkconsulting.assuranceiard.api.model.SouscripteurAuto;
import net.checkconsulting.assuranceiard.api.repository.ConducteurRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisAutoRepository;
import net.checkconsulting.assuranceiard.api.repository.SouscripteurAutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InformationsAutoServiceTest {

    private static final String JETON = "Bearer jeton";
    private static final long DEVIS_ID = 42L;
    private static final Long COMPTE_ID = 7L;
    /** 5 novembre 2026, 23 h 30 UTC : déjà le 6 novembre en France. */
    private static final Instant MAINTENANT = Instant.parse("2026-11-05T23:30:00Z");

    private final SessionService sessionService = mock(SessionService.class);
    private final DevisAutoRepository devisAutos = mock(DevisAutoRepository.class);
    private final ConducteurRepository conducteurs = mock(ConducteurRepository.class);
    private final SouscripteurAutoRepository souscripteurs = mock(SouscripteurAutoRepository.class);

    private final InformationsAutoService service = new InformationsAutoService(
            sessionService, devisAutos, conducteurs, souscripteurs,
            Clock.fixed(MAINTENANT, ZoneOffset.UTC));

    private final Compte compte = new Compte();
    private final DevisAuto devis = new DevisAuto();

    @BeforeEach
    void preparer() {
        compte.setId(COMPTE_ID);
        compte.setCivilite("madame");
        compte.setNom("Durand");
        compte.setPrenom("Marie");
        compte.setTelephone("0612345678");
        devis.setId(DEVIS_ID);
        devis.setConducteurId(10L);
        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(devisAutos.findById(DEVIS_ID)).thenReturn(Optional.of(devis));
        when(souscripteurs.findByDevisAutoId(DEVIS_ID)).thenReturn(Optional.empty());
        when(conducteurs.findById(10L)).thenReturn(Optional.empty());
        when(souscripteurs.save(any())).thenAnswer(a -> a.getArgument(0));
    }

    // ── lire ──────────────────────────────────────────────────────────────────

    @Test
    void lirePreremplitAvecLesInformationsDuCompte() {
        InformationsAutoResponse rep = service.lire(DEVIS_ID, JETON);

        assertThat(rep.civilite()).isEqualTo("madame");
        assertThat(rep.nom()).isEqualTo("Durand");
        assertThat(rep.prenom()).isEqualTo("Marie");
        assertThat(rep.telephone()).isEqualTo("0612345678");
        assertThat(rep.dateNaissance()).isNull();
    }

    @Test
    void lireInclutLaDateDeNaissanceDuConducteur() {
        Conducteur conducteur = new Conducteur();
        conducteur.setDateNaissance(LocalDate.of(1990, 3, 15));
        when(conducteurs.findById(10L)).thenReturn(Optional.of(conducteur));

        InformationsAutoResponse rep = service.lire(DEVIS_ID, JETON);

        assertThat(rep.dateNaissance()).isEqualTo(LocalDate.of(1990, 3, 15));
    }

    @Test
    void lirePreremplitDepuisLesSouscripteurEnregistreSiPresent() {
        SouscripteurAuto existant = new SouscripteurAuto();
        existant.setCivilite("monsieur");
        existant.setNom("Martin");
        existant.setPrenom("Paul");
        existant.setDateNaissance(LocalDate.of(1985, 6, 20));
        existant.setTelephone("0712345678");
        existant.setAdresse("10 rue de la Paix");
        existant.setCodePostal("75001");
        existant.setVille("Paris");
        when(souscripteurs.findByDevisAutoId(DEVIS_ID)).thenReturn(Optional.of(existant));

        InformationsAutoResponse rep = service.lire(DEVIS_ID, JETON);

        assertThat(rep.nom()).isEqualTo("Martin");
        assertThat(rep.adresse()).isEqualTo("10 rue de la Paix");
        assertThat(rep.codePostal()).isEqualTo("75001");
    }

    // ── enregistrer : majeur ───────────────────────────────────────────────────

    /**
     * MAINTENANT = 6 nov 2026 en France.
     * naissance = 7 nov 2008 → 17 ans → refusé.
     * naissance = 6 nov 2008 → 18 ans le jour même → accepté.
     * naissance = 5 nov 2008 → 18 ans depuis hier → accepté.
     */
    @ParameterizedTest
    @CsvSource({"2008-11-07, false", "2008-11-06, true", "2008-11-05, true", "2020-01-01, false"})
    void b3MajeurLeJourDeSes18AnsEnFrance(LocalDate naissance, boolean accepte) {
        InformationsAutoRequest req = demandeValide(naissance);
        if (accepte) {
            assertThatCode(() -> service.enregistrer(DEVIS_ID, req, JETON)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> service.enregistrer(DEVIS_ID, req, JETON))
                    .isInstanceOfSatisfying(InformationRefuseeException.class, e ->
                            assertThat(e.getChamp()).isEqualTo("dateNaissance"));
        }
    }

    @Test
    void enregistreSauvegardeLeSouscripteurAvecLeCompteId() {
        service.enregistrer(DEVIS_ID, demandeValide(LocalDate.of(1990, 1, 1)), JETON);

        verify(souscripteurs).save(any(SouscripteurAuto.class));
    }

    @Test
    void enregistreTrimmeLesChaines() {
        InformationsAutoRequest req = new InformationsAutoRequest(
                "madame", " Durand ", " Marie ",
                LocalDate.of(1990, 1, 1), "0612345678",
                " 12 rue de la Paix ", "75001", " Paris ");
        service.enregistrer(DEVIS_ID, req, JETON);

        verify(souscripteurs).save(org.mockito.ArgumentMatchers.argThat((SouscripteurAuto s) ->
                "Durand".equals(s.getNom())
                        && "Marie".equals(s.getPrenom())
                        && "12 rue de la Paix".equals(s.getAdresse())
                        && "Paris".equals(s.getVille())
                        && COMPTE_ID.equals(s.getCompteId())));
    }

    @Test
    void enregistreRefuseUnDevisInconnu() {
        when(devisAutos.findById(DEVIS_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enregistrer(DEVIS_ID, demandeValide(LocalDate.of(1990, 1, 1)), JETON))
                .isInstanceOf(VehiculeIntrouvableException.class);
    }

    private InformationsAutoRequest demandeValide(LocalDate naissance) {
        return new InformationsAutoRequest(
                "madame", "Durand", "Marie", naissance,
                "0612345678", "12 rue de la Paix", "75001", "Paris");
    }
}
