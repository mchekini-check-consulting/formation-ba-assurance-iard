package net.checkconsulting.assuranceiard.api.service.fva;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.model.DeclarationFva;
import net.checkconsulting.assuranceiard.api.repository.DeclarationFvaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-33 (§7) : bouchon FVA actif en dev et qualif.
 * Réponse selon l'immatriculation :
 * – AA-001-AA → DECLAREE (referenceAccuse FVA-REF-TEST-{contratId})
 * – ZZ-998-ZZ → REJETEE (motifRejet "Véhicule déjà assuré")
 * – null/vide  → DIFFEREE (immatriculation inconnue, à compléter)
 * – autre      → DECLAREE (referenceAccuse généré)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BouchonFvaService implements FvaService {

    private final DeclarationFvaRepository declarations;
    private final Clock horloge;

    @Override
    @Transactional
    public void declarer(long contratId, String immatriculation) {
        DeclarationFva decl = new DeclarationFva();
        decl.setContratId(contratId);
        decl.setImmatriculation(immatriculation);
        decl.setEnvoyeLe(horloge.instant());

        if (immatriculation == null || immatriculation.isBlank()) {
            decl.setStatut("DIFFEREE");
        } else if ("ZZ-998-ZZ".equals(immatriculation)) {
            decl.setStatut("REJETEE");
            decl.setMotifRejet("Véhicule déjà assuré");
        } else if ("AA-001-AA".equals(immatriculation)) {
            decl.setStatut("DECLAREE");
            decl.setReferenceAccuse("FVA-REF-TEST-" + contratId);
        } else {
            decl.setStatut("DECLAREE");
            decl.setReferenceAccuse("FVA-REF-" + contratId);
        }

        declarations.save(decl);
        log.info("layer=service FVA déclaré (bouchon) : contrat={}, statut={}", contratId, decl.getStatut());
    }
}
