package net.checkconsulting.assuranceiard.api.service.avenant;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.AvenantResponse;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import net.checkconsulting.assuranceiard.api.model.AvenantLigne;
import net.checkconsulting.assuranceiard.api.repository.AvenantLigneRepository;
import net.checkconsulting.assuranceiard.api.repository.AvenantRepository;
import net.checkconsulting.assuranceiard.api.service.document.MiseEnForme;
import org.springframework.stereotype.Component;

/**
 * US-23 : les avenants d'un contrat tels que l'écran les affiche (historique de la fiche, avenant en attente).
 * Un avenant en attente prendra effet le lendemain de son acceptation : sa date d'effet affichée est celle du
 * lendemain d'aujourd'hui.
 */
@Component
@RequiredArgsConstructor
public class HistoriqueAvenants {

    static final String EN_ATTENTE = AvenantRepository.EN_ATTENTE;
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final AvenantRepository avenants;
    private final AvenantLigneRepository lignes;
    private final Clock horloge;

    /** Historique, le plus récent d'abord (numéro, date d'effet, garanties changées, statut). */
    public List<AvenantResponse> duContrat(long contratId) {
        List<Avenant> duContrat = avenants.findByContratIdOrderByNumeroDesc(contratId);
        if (duContrat.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AvenantLigne>> parAvenant = lignes
                .findByAvenantIdInOrderById(duContrat.stream().map(Avenant::getId).toList()).stream()
                .collect(Collectors.groupingBy(AvenantLigne::getAvenantId));
        return duContrat.stream()
                .map(avenant -> decrire(avenant, parAvenant.getOrDefault(avenant.getId(), List.of())))
                .toList();
    }

    /** AC5 : l'avenant en attente du contrat. */
    public Optional<AvenantResponse> enAttente(long contratId) {
        return avenants.findFirstByContratIdAndStatut(contratId, EN_ATTENTE)
                .map(avenant -> decrire(avenant, lignes.findByAvenantIdOrderById(avenant.getId())));
    }

    public AvenantResponse decrire(Avenant avenant, List<AvenantLigne> sesLignes) {
        return new AvenantResponse(NumeroAvenant.de(avenant), avenant.getNumero(), avenant.getStatut(),
                EN_ATTENTE.equals(avenant.getStatut()) ? lendemain() : avenant.getDateEffet(),
                avenant.getPrimeAvantTtc(), avenant.getPrimeApresTtc(),
                garanties(sesLignes, CalculAvenant.AJOUT), garanties(sesLignes, CalculAvenant.RETRAIT),
                avenant.getAccepteLe(), avenant.getDocumentId());
    }

    /** Date d'effet d'un avenant accepté aujourd'hui : le lendemain (heure de Paris). */
    public LocalDate lendemain() {
        return LocalDate.ofInstant(horloge.instant(), FRANCE).plusDays(1);
    }

    private static List<AvenantResponse.Garantie> garanties(List<AvenantLigne> lignes, String action) {
        return lignes.stream().filter(ligne -> action.equals(ligne.getAction()))
                .map(ligne -> new AvenantResponse.Garantie(ligne.getCodeOption(),
                        MiseEnForme.option(ligne.getCodeOption()), ligne.getPrixMensuelTtc()))
                .toList();
    }
}
