package net.checkconsulting.assuranceiard.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.dto.ClientResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.ContratClientResponse;
import net.checkconsulting.assuranceiard.api.dto.DossierClientResponse;
import net.checkconsulting.assuranceiard.api.exception.ClientInconnuException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import net.checkconsulting.assuranceiard.api.repository.MandatSepaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackOfficeServiceTest {

    private static final UUID REFERENCE = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    @Mock
    private CompteRepository compteRepository;

    @Mock
    private ContratRepository contratRepository;

    @Mock
    private MandatSepaRepository mandatRepository;

    @Mock
    private JustificatifRepository justificatifRepository;

    @InjectMocks
    private BackOfficeService service;

    private Compte client;

    @BeforeEach
    void preparer() {
        client = new Compte();
        client.setId(1L);
        client.setEmail("alice@exemple.fr");
        client.setPrenom("Alice");
        client.setNom("Martin");
        client.setProfil(Profil.ASSURE);
    }

    @Test
    void listeLesClients() {
        when(compteRepository.findByProfilInOrderByNomAscPrenomAscEmailAsc(any())).thenReturn(List.of(client));

        assertThat(service.listerClients())
                .containsExactly(new ClientResumeResponse(1L, "Alice", "Martin", "alice@exemple.fr", "ASSURE"));
    }

    @Test
    void ac4LeDossierClientNeDonneQueLesQuatreDerniersCaracteresDeLIban() {
        DossierSouscription dossier = new DossierSouscription();
        dossier.setCompte(client);
        dossier.setReferenceDevis(REFERENCE);
        Contrat contrat = new Contrat();
        contrat.setDossier(dossier);
        contrat.setNumero("AE-2026-000001");
        contrat.setStatut("ACTIF");
        contrat.setCotisationMensuelle(new BigDecimal("18.50"));
        contrat.setDateEffet(LocalDate.of(2026, 12, 1));
        MandatSepa mandat = new MandatSepa();
        mandat.setTitulaire("Alice Martin");
        mandat.setIbanDebut("FR76");
        mandat.setIbanFin("0189");
        mandat.setJourPrelevement(10);
        mandat.setRum("AE-RUM-1");
        when(compteRepository.findById(1L)).thenReturn(Optional.of(client));
        when(contratRepository.findByDossierCompteOrderByDateCreationDesc(client)).thenReturn(List.of(contrat));
        when(mandatRepository.findByDossier(dossier)).thenReturn(Optional.of(mandat));
        when(justificatifRepository.findByDossierCompteOrderByIdDesc(client)).thenReturn(List.of());

        DossierClientResponse dossierClient = service.dossier(1L);

        assertThat(dossierClient.email()).isEqualTo("alice@exemple.fr");
        assertThat(dossierClient.contrats()).containsExactly(new ContratClientResponse(REFERENCE, "AE-2026-000001",
                "ACTIF", null, null, null, new BigDecimal("18.50"), LocalDate.of(2026, 12, 1), "Alice Martin",
                "0189", 10, "AE-RUM-1"));
        assertThat(dossierClient.toString()).doesNotContain("FR76");
    }

    @Test
    void unContratSansMandatNAffichePasDePaiement() {
        DossierSouscription dossier = new DossierSouscription();
        dossier.setReferenceDevis(REFERENCE);
        Contrat contrat = new Contrat();
        contrat.setDossier(dossier);
        when(compteRepository.findById(1L)).thenReturn(Optional.of(client));
        when(contratRepository.findByDossierCompteOrderByDateCreationDesc(client)).thenReturn(List.of(contrat));
        when(mandatRepository.findByDossier(dossier)).thenReturn(Optional.empty());

        ContratClientResponse vu = service.dossier(1L).contrats().get(0);

        assertThat(vu.ibanFin()).isNull();
        assertThat(vu.titulaire()).isNull();
    }

    @Test
    void unCompteInterneOuAbsentNEstPasUnDossierClient() {
        Compte gestionnaire = new Compte();
        gestionnaire.setProfil(Profil.GESTIONNAIRE);
        when(compteRepository.findById(2L)).thenReturn(Optional.of(gestionnaire));
        when(compteRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.dossier(2L)).isInstanceOf(ClientInconnuException.class);
        assertThatThrownBy(() -> service.dossier(3L)).isInstanceOf(ClientInconnuException.class);
    }
}
