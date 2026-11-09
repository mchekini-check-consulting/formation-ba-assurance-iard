package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.DevisEnregistreResponse;
import net.checkconsulting.assuranceiard.api.dto.DevisRequest;
import net.checkconsulting.assuranceiard.api.dto.DonneesDevisPdf;
import net.checkconsulting.assuranceiard.api.dto.LogementDevis;
import net.checkconsulting.assuranceiard.api.dto.RepriseDevisResponse;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.SessionInvalideException;
import net.checkconsulting.assuranceiard.api.mapper.DevisEnregistreMapperImpl;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DevisEnregistre;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DevisEnregistreServiceTest {

    private static final String JETON = "Bearer jeton";
    private static final UUID REFERENCE = UUID.randomUUID();
    private static final Instant MAINTENANT = Instant.parse("2026-11-05T10:00:00Z");
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 5);
    private static final BigDecimal PRIX_BAREME = new BigDecimal("12.22");
    private static final BigDecimal PRIX_GARANTI = new BigDecimal("10.00");

    private final DevisEnregistreRepository repository = mock(DevisEnregistreRepository.class);
    private final RecommandationDevisRepository recommandations = mock(RecommandationDevisRepository.class);
    private final SessionService sessionService = mock(SessionService.class);
    private final DevisPdfService pdf = mock(DevisPdfService.class);
    private final TarifDevisService tarifDevis = mock(TarifDevisService.class);
    private final DevisEnregistreService service = new DevisEnregistreService(repository, recommandations,
            sessionService, tarifDevis, new DevisEnregistreMapperImpl(), pdf,
            Clock.fixed(MAINTENANT, ZoneOffset.UTC));

    private final Compte compte = new Compte();
    private final RecommandationDevis recommandation = new RecommandationDevis();

    @BeforeEach
    void preparer() {
        recommandation.setReference(REFERENCE);
        recommandation.setDateCreation(MAINTENANT.minus(2, ChronoUnit.DAYS));
        recommandation.setValeurContenu(15_000);
        recommandation.setObjetsValeur(true);
        recommandation.setRemboursement("neuf");
        recommandation.setPriorite("prix");
        when(sessionService.compteConnecte(JETON)).thenReturn(compte);
        when(tarifDevis.prixMensuel(any(), any(), any(), any(), any())).thenAnswer(appel -> prixDuMoteur(appel.getArgument(3), appel.getArgument(4)));
        when(recommandations.findByReference(REFERENCE)).thenReturn(Optional.of(recommandation));
        when(repository.save(any(DevisEnregistre.class))).thenAnswer(appel -> appel.getArgument(0));
        when(repository.findByCompteAndReference(compte, REFERENCE)).thenReturn(Optional.empty());
        when(repository.findFirstByReferenceOrderByDateCreationDesc(REFERENCE)).thenReturn(Optional.empty());
    }

    private static BigDecimal prixDuMoteur(List<String> options, String franchise) {
        BigDecimal avecOptions = options.isEmpty() ? PRIX_BAREME : PRIX_BAREME.add(new BigDecimal("2.50"));
        return "500".equals(franchise) ? avecOptions.subtract(BigDecimal.ONE) : avecOptions;
    }

    private static LogementDevis logement() {
        return new LogementDevis("locataire", "principale", "appartement", "12 rue des Lilas", "75001", "Paris",
                "60", "3", "non-assure", "", "", "2", "jardin", "", "", "0");
    }

    private static DevisRequest demande(String formule, String franchise, List<String> options) {
        return new DevisRequest(REFERENCE, logement(), formule, options, franchise);
    }

    private static DevisRequest demande() {
        return demande("confort", "300", List.of());
    }

    private DevisEnregistre existant(LocalDate finValidite) {
        DevisEnregistre devis = new DevisEnregistre();
        devis.setCompte(compte);
        devis.setReference(REFERENCE);
        devis.setTypeLogement("appartement");
        devis.setSurface(60);
        devis.setFormule("confort");
        devis.setOptions(List.of());
        devis.setFranchise("300");
        devis.setPrixMensuel(PRIX_GARANTI);
        devis.setDateCreation(MAINTENANT.minus(2, ChronoUnit.DAYS));
        devis.setDateFinValidite(finValidite);
        devis.setDateDernierContact(MAINTENANT.minus(2, ChronoUnit.DAYS));
        return devis;
    }

    private void enBase(DevisEnregistre devis) {
        when(repository.findByCompteAndReference(compte, REFERENCE)).thenReturn(Optional.of(devis));
    }

    @Test
    void rg1LaValiditeCourtTrenteJoursDepuisLaCreationDuDevis() {
        DevisEnregistreResponse reponse = service.enregistrer(JETON, demande());

        assertThat(reponse.dateFinValidite()).isEqualTo(AUJOURDHUI.minusDays(2).plusDays(30));
        assertThat(reponse.expire()).isFalse();
    }

    @Test
    void ac3ConserveLogementFormuleOptionsFranchiseEtPrixCalcule() {
        service.enregistrer(JETON, demande("confort", "300", List.of("objets-valeur", "objets-valeur")));

        ArgumentCaptor<DevisEnregistre> capture = forClass(DevisEnregistre.class);
        verify(repository).save(capture.capture());
        DevisEnregistre devis = capture.getValue();
        assertThat(devis.getCompte()).isSameAs(compte);
        assertThat(devis.getAdresse()).isEqualTo("12 rue des Lilas");
        assertThat(devis.getSurface()).isEqualTo(60);
        assertThat(devis.getDependances()).isEqualTo("jardin");
        assertThat(devis.getFormule()).isEqualTo("confort");
        assertThat(devis.getOptions()).containsExactly("objets-valeur");
        assertThat(devis.getFranchise()).isEqualTo("300");
        assertThat(devis.getPrixMensuel()).isEqualByComparingTo("14.72");
        assertThat(devis.getDateDernierContact()).isEqualTo(MAINTENANT);
    }

    @Test
    void uneRecommandationTropAncienneNeDonnePasUnDevisNeExpire() {
        recommandation.setDateCreation(MAINTENANT.minus(40, ChronoUnit.DAYS));

        assertThat(service.enregistrer(JETON, demande()).dateFinValidite()).isEqualTo(AUJOURDHUI.plusDays(30));
    }

    @Test
    void refuseSansSession() {
        when(sessionService.compteConnecte(null)).thenThrow(new SessionInvalideException());

        assertThatThrownBy(() -> service.enregistrer(null, demande())).isInstanceOf(SessionInvalideException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void refuseUnDevisInconnu() {
        when(recommandations.findByReference(REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enregistrer(JETON, demande())).isInstanceOf(DevisInconnuException.class);
    }

    @Test
    void reenregistrerSansChangementGardeLePrixEtLaValidite() {
        LocalDate fin = AUJOURDHUI.plusDays(10);
        enBase(existant(fin));

        DevisEnregistreResponse reponse = service.enregistrer(JETON, demande());

        assertThat(reponse.prixMensuel()).isEqualByComparingTo(PRIX_GARANTI);
        assertThat(reponse.dateFinValidite()).isEqualTo(fin);
    }

    @Test
    void changerUnChoixAppliqueLeBaremeSansProlongerLaValidite() {
        LocalDate fin = AUJOURDHUI.plusDays(10);
        enBase(existant(fin));

        DevisEnregistreResponse reponse = service.enregistrer(JETON, demande("confort", "500", List.of()));

        assertThat(reponse.prixMensuel()).isEqualByComparingTo("11.22");
        assertThat(reponse.dateFinValidite()).isEqualTo(fin);
    }

    @Test
    void reenregistrerUnDevisExpireRepartPourTrenteJoursAuBareme() {
        enBase(existant(AUJOURDHUI.minusDays(1)));

        DevisEnregistreResponse reponse = service.enregistrer(JETON, demande());

        assertThat(reponse.prixMensuel()).isEqualByComparingTo(PRIX_BAREME);
        assertThat(reponse.dateFinValidite()).isEqualTo(AUJOURDHUI.plusDays(30));
    }

    @Test
    void ac1ListeLesDevisDuClient() {
        when(repository.findByCompteOrderByDateCreationDesc(compte)).thenReturn(
                List.of(existant(AUJOURDHUI), existant(AUJOURDHUI.minusDays(1))));

        List<DevisEnregistreResponse> liste = service.lister(JETON);

        assertThat(liste).extracting(DevisEnregistreResponse::expire).containsExactly(false, true);
        assertThat(liste.get(0).prixAnnuel()).isEqualByComparingTo("120.00");
    }

    @Test
    void ac5UnDevisValideRepritGardeSonPrix() {
        enBase(existant(AUJOURDHUI));

        RepriseDevisResponse reprise = service.reprendre(JETON, REFERENCE);

        assertThat(reprise.prixMensuel()).isEqualByComparingTo(PRIX_GARANTI);
        assertThat(reprise.ancienPrixMensuel()).isNull();
        assertThat(reprise.dateFinValidite()).isEqualTo(AUJOURDHUI);
        assertThat(reprise.besoins().valeurContenu()).isEqualTo(15_000);
        assertThat(reprise.besoins().objetsValeur()).isTrue();
    }

    @Test
    void ac6UnDevisExpireRepritEstRecalculeEtRepartPourTrenteJours() {
        DevisEnregistre devis = existant(AUJOURDHUI.minusDays(1));
        enBase(devis);

        RepriseDevisResponse reprise = service.reprendre(JETON, REFERENCE);

        assertThat(reprise.ancienPrixMensuel()).isEqualByComparingTo(PRIX_GARANTI);
        assertThat(reprise.prixMensuel()).isEqualByComparingTo(PRIX_BAREME);
        assertThat(reprise.dateFinValidite()).isEqualTo(AUJOURDHUI.plusDays(30));
        assertThat(devis.getDateCreation()).isEqualTo(MAINTENANT);
        assertThat(devis.getDateDernierContact()).isEqualTo(MAINTENANT);
    }

    @Test
    void neReprendPasLeDevisDUnAutreClient() {
        assertThatThrownBy(() -> service.reprendre(JETON, REFERENCE)).isInstanceOf(DevisInconnuException.class);
    }

    @Test
    void rgpdSupprimeLeDevisDuClient() {
        DevisEnregistre devis = existant(AUJOURDHUI);
        enBase(devis);

        service.supprimer(JETON, REFERENCE);

        verify(repository).delete(devis);
    }

    @Test
    void rgpdNeSupprimePasLeDevisDUnAutreClient() {
        assertThatThrownBy(() -> service.supprimer(JETON, REFERENCE)).isInstanceOf(DevisInconnuException.class);
        verify(repository, never()).delete(any());
    }

    private DonneesDevisPdf donneesPdf(DevisRequest demande) {
        when(pdf.generer(any())).thenReturn(new byte[] {1});
        service.telecharger(demande);
        ArgumentCaptor<DonneesDevisPdf> capture = forClass(DonneesDevisPdf.class);
        verify(pdf).generer(capture.capture());
        return capture.getValue();
    }

    @Test
    void rg2SansDevisEnregistreLePdfPrendLeBaremeEtLaDateDeLaCreation() {
        DonneesDevisPdf donnees = donneesPdf(demande());

        assertThat(donnees.prixMensuel()).isEqualByComparingTo(PRIX_BAREME);
        assertThat(donnees.prixAnnuel()).isEqualByComparingTo("146.64");
        assertThat(donnees.dateFinValidite()).isEqualTo(AUJOURDHUI.minusDays(2).plusDays(30));
    }

    @Test
    void lePdfDUnDevisEnregistreReprendSonPrixGarantiEtSaDate() {
        LocalDate fin = AUJOURDHUI.plusDays(10);
        when(repository.findFirstByReferenceOrderByDateCreationDesc(REFERENCE))
                .thenReturn(Optional.of(existant(fin)));

        DonneesDevisPdf donnees = donneesPdf(demande());

        assertThat(donnees.prixMensuel()).isEqualByComparingTo(PRIX_GARANTI);
        assertThat(donnees.dateFinValidite()).isEqualTo(fin);
    }

    @Test
    void lePdfIgnoreUnPrixGarantiPourDAutresChoix() {
        when(repository.findFirstByReferenceOrderByDateCreationDesc(REFERENCE))
                .thenReturn(Optional.of(existant(AUJOURDHUI)));

        assertThat(donneesPdf(demande("confort", "500", List.of())).prixMensuel()).isEqualByComparingTo("11.22");
    }

    @Test
    void lePdfNeReprendPasUnDevisExpire() {
        when(repository.findFirstByReferenceOrderByDateCreationDesc(REFERENCE))
                .thenReturn(Optional.of(existant(AUJOURDHUI.minusDays(1))));

        assertThat(donneesPdf(demande()).prixMensuel()).isEqualByComparingTo(PRIX_BAREME);
    }

    @Test
    void lePdfRefuseUnDevisInconnu() {
        when(recommandations.findByReference(REFERENCE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.telecharger(demande())).isInstanceOf(DevisInconnuException.class);
    }
}
