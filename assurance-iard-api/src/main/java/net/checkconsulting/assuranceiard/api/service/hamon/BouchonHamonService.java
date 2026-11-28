package net.checkconsulting.assuranceiard.api.service.hamon;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.model.ResiliationHamon;
import net.checkconsulting.assuranceiard.api.repository.ResiliationHamonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-33 (§8) : bouchon Hamon actif en dev et qualif.
 * Réponse selon l'ancienNumero :
 * – HAM-OK-001 → ACCEPTEE (dateFinAncien = dateEffet - 1 jour)
 * – HAM-404    → INTROUVABLE
 * – HAM-500    → ERREUR (ne change jamais le statut du contrat)
 * – autre      → ACCEPTEE (dateFinAncien = dateEffet - 1 jour)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BouchonHamonService implements HamonService {

    private final ResiliationHamonRepository resiliations;
    private final Clock horloge;

    @Override
    @Transactional
    public void resilier(long contratId, String ancienAssureur, String ancienNumero, LocalDate dateEffet) {
        ResiliationHamon resil = new ResiliationHamon();
        resil.setContratId(contratId);
        resil.setAncienAssureur(ancienAssureur != null ? ancienAssureur : "Assureur inconnu");
        resil.setAncienNumero(ancienNumero != null ? ancienNumero : "INCONNU");
        resil.setEnvoyeLe(horloge.instant());

        if ("HAM-500".equals(ancienNumero)) {
            resil.setStatut("ERREUR");
            resil.setNbEssais(1);
            log.warn("layer=service Hamon erreur serveur (bouchon) : contrat={}", contratId);
        } else if ("HAM-404".equals(ancienNumero)) {
            resil.setStatut("INTROUVABLE");
        } else {
            resil.setStatut("ACCEPTEE");
            resil.setDateFinAncien(dateEffet != null ? dateEffet.minusDays(1) : null);
        }

        resiliations.save(resil);
        log.info("layer=service Hamon simulé (bouchon) : contrat={}, statut={}", contratId, resil.getStatut());
    }
}
