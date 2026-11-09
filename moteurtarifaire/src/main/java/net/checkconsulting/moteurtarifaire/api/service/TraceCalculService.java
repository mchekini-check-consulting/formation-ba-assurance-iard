package net.checkconsulting.moteurtarifaire.api.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationRequest;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationResponse;
import net.checkconsulting.moteurtarifaire.api.model.CalculTarif;
import net.checkconsulting.moteurtarifaire.api.repository.CalculTarifRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class TraceCalculService {

    private final CalculTarifRepository repository;
    private final JsonMapper jsonMapper;

    public TraceCalculService(CalculTarifRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void enregistrer(TarifHabitationRequest demande, TarifHabitationResponse resultat, Instant dateCalcul) {
        CalculTarif calcul = new CalculTarif();
        calcul.setIdTarif(resultat.idTarif());
        calcul.setVersionGrille(resultat.versionGrille());
        calcul.setTarifable(resultat.tarifable());
        calcul.setDonneesRecues(jsonMapper.writeValueAsString(demande));
        calcul.setResultat(jsonMapper.writeValueAsString(resultat));
        calcul.setDateCalcul(dateCalcul);
        calcul.setDateValidite(resultat.dateValidite());
        repository.save(calcul);
        log.info("Calcul tarifaire enregistré : idTarif={}", resultat.idTarif());
    }

    @Transactional(readOnly = true)
    public Optional<TarifHabitationResponse> relire(UUID idTarif) {
        return repository.findByIdTarif(idTarif)
                .map(calcul -> jsonMapper.readValue(calcul.getResultat(), TarifHabitationResponse.class));
    }
}
