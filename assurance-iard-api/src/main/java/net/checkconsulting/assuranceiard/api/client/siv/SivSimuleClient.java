package net.checkconsulting.assuranceiard.api.client.siv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.model.RefPlaqueTest;
import net.checkconsulting.assuranceiard.api.model.RefVehicule;
import net.checkconsulting.assuranceiard.api.repository.RefPlaqueTestRepository;
import net.checkconsulting.assuranceiard.api.repository.RefVehiculeRepository;
import org.springframework.stereotype.Component;

/**
 * US-27 : simulateur SIV — remplace l'appel au SIV réel tant que l'intégration n'est pas faite.
 * Se comporte comme {@link net.checkconsulting.assuranceiard.api.service.VerificationBancaireBouchon}
 * (pas de @Profile : actif dans tous les environnements jusqu'au branchement du SIV réel).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SivSimuleClient implements SivClient {

    private final RefPlaqueTestRepository plaques;
    private final RefVehiculeRepository vehicules;

    @Override
    public SivReponse consulterPlaque(String immatriculation) {
        RefPlaqueTest entree = plaques.findByImmatriculation(immatriculation).orElse(null);
        if (entree == null) {
            log.debug("Plaque {} non trouvée dans ref_plaque_test → INCONNU", immatriculation);
            return new SivReponse(SivReponse.INCONNU, null);
        }
        return switch (entree.getComportement()) {
            case SivReponse.TROUVE -> {
                RefVehicule v = vehicules.findById(entree.getRefVehiculeId())
                        .orElseThrow(() -> new IllegalStateException(
                                "ref_plaque_test.ref_vehicule_id introuvable : " + entree.getRefVehiculeId()));
                yield new SivReponse(SivReponse.TROUVE, versInfo(v));
            }
            default -> new SivReponse(entree.getComportement(), null);
        };
    }

    private VehiculeInfoSiv versInfo(RefVehicule v) {
        return new VehiculeInfoSiv(v.getId(), v.getMarque(), v.getModele(),
                v.getEnergie(), v.getVersion(), v.getAnnee(), v.getValeurNeuve());
    }
}
