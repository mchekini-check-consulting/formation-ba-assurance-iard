package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.RecommandationRequest;
import net.checkconsulting.assuranceiard.api.mapper.RecommandationMapper;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RG4 de l'US-10 : conserve les réponses et la recommandation calculée par l'écran.
 * PROVISOIRE : la recommandation n'est pas recalculée ici ; à faire avant d'en tirer une vraie fiche conseil.
 */
@Service
@RequiredArgsConstructor
public class RecommandationService {

    private final RecommandationDevisRepository repository;
    private final RecommandationMapper mapper;
    private final Clock horloge;

    @Transactional
    public UUID enregistrer(RecommandationRequest demande) {
        RecommandationDevis recommandation = mapper.versEntite(demande);
        recommandation.setReference(UUID.randomUUID());
        recommandation.setDateCreation(horloge.instant());
        return repository.save(recommandation).getReference();
    }
}
