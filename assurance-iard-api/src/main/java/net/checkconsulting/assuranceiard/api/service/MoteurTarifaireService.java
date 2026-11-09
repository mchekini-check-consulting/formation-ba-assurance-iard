package net.checkconsulting.assuranceiard.api.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.client.MoteurTarifaireClient;
import net.checkconsulting.assuranceiard.api.dto.TarifRequest;
import net.checkconsulting.assuranceiard.api.dto.TarifResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoteurTarifaireService {

    private final MoteurTarifaireClient client;

    public TarifResponse calculer(TarifRequest demande) {
        TarifResponse tarif = client.calculer(demande);
        log.info("Tarif obtenu du moteur : idTarif={}, tarifable={}", tarif.idTarif(), tarif.tarifable());
        return tarif;
    }

    public TarifResponse reprendre(UUID idTarif) {
        TarifResponse tarif = client.reprendre(idTarif);
        log.info("Tarif repris depuis le moteur : idTarif={}", idTarif);
        return tarif;
    }
}
