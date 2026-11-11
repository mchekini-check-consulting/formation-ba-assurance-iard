package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.config.DonneesTestProperties;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Justificatif;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import net.checkconsulting.assuranceiard.api.repository.MandatSepaRepository;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class DonneesRecetteTest {

    private final PasswordEncoder encodeur = mock(PasswordEncoder.class);
    private final CompteRepository comptes = mock(CompteRepository.class);
    private final RecommandationDevisRepository recommandations = mock(RecommandationDevisRepository.class);
    private final DevisEnregistreRepository devis = mock(DevisEnregistreRepository.class);
    private final DossierSouscriptionRepository dossiers = mock(DossierSouscriptionRepository.class);
    private final JustificatifRepository justificatifs = mock(JustificatifRepository.class);
    private final MandatSepaRepository mandats = mock(MandatSepaRepository.class);
    private final ContratRepository contrats = mock(ContratRepository.class);
    private final StockageJustificatifs stockage = mock(StockageJustificatifs.class);
    private final DonneesRecette donnees = new DonneesRecette(new DonneesTestProperties(true, "Recette2026"), encodeur,
            Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC), comptes, recommandations, devis,
            dossiers, justificatifs, mandats, contrats, stockage);

    @BeforeEach
    void preparer() {
        when(encodeur.encode("Recette2026")).thenReturn("hash");
        when(stockage.stocker(any(), anyString())).thenReturn("cle.pdf");
        when(comptes.save(any())).thenAnswer(appel -> appel.getArgument(0));
        when(recommandations.save(any())).thenAnswer(appel -> appel.getArgument(0));
        when(dossiers.save(any())).thenAnswer(appel -> appel.getArgument(0));
        when(mandats.save(any())).thenAnswer(appel -> appel.getArgument(0));
    }

    @Test
    void rg5CreeDeuxAssuresUnGestionnaireEtUnAdministrateur() {
        donnees.run(null);

        ArgumentCaptor<Compte> crees = ArgumentCaptor.forClass(Compte.class);
        verify(comptes, times(4)).save(crees.capture());
        assertThat(crees.getAllValues()).extracting(Compte::getEmail, Compte::getProfil).containsExactly(
                tuple(DonneesRecette.ASSURE_1, Profil.ASSURE),
                tuple(DonneesRecette.ASSURE_2, Profil.ASSURE),
                tuple(DonneesRecette.GESTIONNAIRE, Profil.GESTIONNAIRE),
                tuple(DonneesRecette.ADMINISTRATEUR, Profil.ADMINISTRATEUR));
        assertThat(crees.getAllValues()).extracting(Compte::getMotDePasseHash).containsOnly("hash");

        verify(devis, times(2)).save(any());
        verify(justificatifs, times(2)).save(any(Justificatif.class));
        ArgumentCaptor<MandatSepa> mandat = ArgumentCaptor.forClass(MandatSepa.class);
        verify(mandats, times(2)).save(mandat.capture());
        assertThat(mandat.getAllValues()).extracting(MandatSepa::getIbanFin).containsExactly("0189", "4321");
        ArgumentCaptor<Contrat> contrat = ArgumentCaptor.forClass(Contrat.class);
        verify(contrats, times(2)).save(contrat.capture());
        assertThat(contrat.getAllValues()).extracting(Contrat::getStatut).containsOnly("ACTIF");
    }

    @Test
    void neRecreeRienSiLesComptesExistentEtRedeposeUnDocumentEfface() {
        when(comptes.existsByEmail(anyString())).thenReturn(true);
        Compte assure = new Compte();
        Justificatif efface = new Justificatif();
        efface.setCleStockage("ancienne.pdf");
        Justificatif present = new Justificatif();
        present.setCleStockage("presente.pdf");
        when(comptes.findByEmail(DonneesRecette.ASSURE_1)).thenReturn(Optional.of(assure));
        when(comptes.findByEmail(DonneesRecette.ASSURE_2)).thenReturn(Optional.empty());
        when(justificatifs.findByDossierCompteOrderByIdDesc(assure)).thenReturn(List.of(efface, present));
        when(stockage.lire("ancienne.pdf")).thenReturn(Optional.empty());
        when(stockage.lire("presente.pdf")).thenReturn(Optional.of(new byte[] {1}));

        donnees.run(null);

        verify(comptes, never()).save(any());
        assertThat(efface.getCleStockage()).isEqualTo("cle.pdf");
        assertThat(present.getCleStockage()).isEqualTo("presente.pdf");
    }
}
