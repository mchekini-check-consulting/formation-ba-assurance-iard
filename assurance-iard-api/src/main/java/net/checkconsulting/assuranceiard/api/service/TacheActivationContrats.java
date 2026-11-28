package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.HistoriqueStatutContrat;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.HistoriqueStatutContratRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-33 : active chaque nuit les contrats dont la date d'effet est atteinte. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TacheActivationContrats {

    private static final String EN_ATTENTE_EFFET = "EN_ATTENTE_EFFET";
    private static final String ACTIF_JUSTIFICATIFS = "ACTIF_JUSTIFICATIFS_ATTENDUS";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final ContratRepository contratsRepo;
    private final HistoriqueStatutContratRepository historiqueRepo;
    private final Clock horloge;

    @Scheduled(cron = "0 5 0 * * *", zone = "Europe/Paris")
    @Transactional
    public void activerContratsEchus() {
        LocalDate aujourd_hui = LocalDate.now(horloge.withZone(FRANCE));
        List<Contrat> aActiver = contratsRepo.enAttenteEffetAvant(aujourd_hui);
        if (aActiver.isEmpty()) {
            return;
        }
        log.info("layer=service Activation contrats EN_ATTENTE_EFFET : nombre={}, date={}", aActiver.size(), aujourd_hui);
        for (Contrat contrat : aActiver) {
            contrat.setStatut(ACTIF_JUSTIFICATIFS);
            contratsRepo.save(contrat);

            HistoriqueStatutContrat ligne = new HistoriqueStatutContrat();
            ligne.setContratId(contrat.getId());
            ligne.setStatutAvant(EN_ATTENTE_EFFET);
            ligne.setStatutApres(ACTIF_JUSTIFICATIFS);
            ligne.setChangeLe(horloge.instant());
            ligne.setMotif("Date d'effet atteinte");
            ligne.setAuteur("SYSTEME");
            historiqueRepo.save(ligne);
        }
    }
}
