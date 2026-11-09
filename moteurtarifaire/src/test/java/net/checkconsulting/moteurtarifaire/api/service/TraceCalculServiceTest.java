package net.checkconsulting.moteurtarifaire.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.moteurtarifaire.api.JeuxDeDonnees;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationResponse;
import net.checkconsulting.moteurtarifaire.api.model.CalculTarif;
import net.checkconsulting.moteurtarifaire.api.repository.CalculTarifRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class TraceCalculServiceTest {

    private static final UUID ID_TARIF = UUID.randomUUID();
    private static final Instant DATE_CALCUL = Instant.EPOCH;
    private static final LocalDate DATE_VALIDITE = LocalDate.EPOCH;
    private static final String VERSION = "version-de-test";

    @Mock
    private CalculTarifRepository repository;

    private TraceCalculService service() {
        JsonMapper jsonMapper = JsonMapper.builder().build();
        return new TraceCalculService(repository, jsonMapper);
    }

    @Test
    void enregistreLaDemandeLeResultatEtLaVersionDeGrille() {
        TarifHabitationResponse resultat = TarifHabitationResponse.builder()
                .idTarif(ID_TARIF)
                .versionGrille(VERSION)
                .tarifable(true)
                .dateValidite(DATE_VALIDITE)
                .build();

        service().enregistrer(JeuxDeDonnees.demandeCa1(), resultat, DATE_CALCUL);

        ArgumentCaptor<CalculTarif> capture = ArgumentCaptor.forClass(CalculTarif.class);
        verify(repository).save(capture.capture());
        CalculTarif calcul = capture.getValue();
        assertThat(calcul.getIdTarif()).isEqualTo(ID_TARIF);
        assertThat(calcul.getVersionGrille()).isEqualTo(VERSION);
        assertThat(calcul.isTarifable()).isTrue();
        assertThat(calcul.getDateCalcul()).isEqualTo(DATE_CALCUL);
        assertThat(calcul.getDateValidite()).isEqualTo(DATE_VALIDITE);
        assertThat(calcul.getDonneesRecues()).contains(JeuxDeDonnees.demandeCa1().codePostal());
        assertThat(calcul.getResultat()).contains(ID_TARIF.toString());
    }

    @Test
    void uneTraceAncienneAvecUnChampDisparuResteLisible() {
        CalculTarif ancien = new CalculTarif();
        ancien.setResultat("{\"idTarif\":\"" + ID_TARIF + "\",\"tarifable\":true,\"champDisparu\":true}");
        when(repository.findByIdTarif(ID_TARIF)).thenReturn(Optional.of(ancien));

        assertThat(service().relire(ID_TARIF)).map(TarifHabitationResponse::idTarif).contains(ID_TARIF);
    }

    @Test
    void uneTraceInconnueDonneUnResultatVide() {
        when(repository.findByIdTarif(ID_TARIF)).thenReturn(Optional.empty());

        assertThat(service().relire(ID_TARIF)).isEmpty();
    }
}
