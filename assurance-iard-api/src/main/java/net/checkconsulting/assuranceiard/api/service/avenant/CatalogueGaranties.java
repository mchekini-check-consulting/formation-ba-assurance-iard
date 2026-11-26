package net.checkconsulting.assuranceiard.api.service.avenant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.exception.MoteurIndisponibleException;
import net.checkconsulting.assuranceiard.api.model.Conducteur;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.ContratOption;
import net.checkconsulting.assuranceiard.api.repository.ConducteurRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratOptionRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.service.FormuleAutoService;
import net.checkconsulting.assuranceiard.api.service.TarifDevisService;
import net.checkconsulting.assuranceiard.api.service.document.MiseEnForme;
import org.springframework.stereotype.Component;

/**
 * US-23, AC1 et AC2 : les garanties du contrat et les options qu'on peut lui ajouter, avec leur prix mensuel TTC.
 * <ul>
 *   <li>Prix : grille de T-04 en auto (véhicule et conducteur du contrat), grille du sprint 1 en habitation, par les
 *       services de calcul existants. Une garantie déjà souscrite garde le prix payé par le client (contrat_option) ;
 *       à défaut (contrat du sprint 1), le prix de la grille.</li>
 *   <li>Obligatoires (non modifiables) : celles marquées obligatoires dans le contrat (contrat_option.obligatoire),
 *       et toujours la responsabilité civile. La grille du moteur ne marque aucune option obligatoire.</li>
 * </ul>
 * Ces prix servent aussi au calcul de la nouvelle cotisation ({@link CalculAvenant}) : l'écran et l'avenant sont
 * cohérents.
 */
@Component
@RequiredArgsConstructor
public class CatalogueGaranties {

    /** AC2 : garanties obligatoires dans tout contrat (responsabilité civile auto). */
    static final String RESPONSABILITE_CIVILE = "RC";
    static final String RAISON_RESPONSABILITE_CIVILE = "La responsabilité civile est obligatoire : la loi impose "
            + "d'assurer tout véhicule qui circule (article L211-1 du Code des assurances).";
    static final String RAISON_OBLIGATOIRE = "Cette garantie est obligatoire dans votre contrat : elle ne peut pas "
            + "être retirée.";
    static final String RAISON_SANS_PRIX = "Cette garantie ne peut pas être modifiée en ligne : contactez votre "
            + "conseiller.";

    private final ContratOptionRepository options;
    private final DonneesContratRepository donneesContrat;
    private final ConducteurRepository conducteurs;
    private final FormuleAutoService formulesAuto;
    private final TarifDevisService tarifsHabitation;

    /** Garanties souscrites d'abord (dans l'ordre du contrat), puis les options qu'on peut ajouter. */
    public List<GarantieProposee> garanties(Contrat contrat, DonneesContrat donnees, LocalDate date) {
        Map<String, BigDecimal> grille = prixDeLaGrille(contrat, donnees);
        Set<String> obligatoires = options.enCours(contrat.getId(), date).stream()
                .filter(option -> Boolean.TRUE.equals(option.getObligatoire()))
                .map(ContratOption::getCodeOption)
                .collect(Collectors.toSet());
        obligatoires.add(RESPONSABILITE_CIVILE);

        List<GarantieProposee> garanties = new ArrayList<>();
        Set<String> souscrites = new HashSet<>();
        for (DonneesContrat.Option option : donnees.options()) {
            if (souscrites.add(option.code())) {
                BigDecimal prix = option.prixMensuelTtc() == null ? grille.get(option.code())
                        : option.prixMensuelTtc();
                garanties.add(proposee(option.code(), prix, true, obligatoires.contains(option.code())));
            }
        }
        grille.forEach((code, prix) -> {
            if (!souscrites.contains(code)) {
                garanties.add(proposee(code, prix, false, false));
            }
        });
        return garanties;
    }

    private static GarantieProposee proposee(String code, BigDecimal prix, boolean souscrite, boolean obligatoire) {
        return new GarantieProposee(code, MiseEnForme.option(code), prix, souscrite, obligatoire,
                raison(code, prix, obligatoire));
    }

    /** AC2 : pourquoi une garantie n'est pas modifiable ; vide si elle l'est. */
    static String raison(String code, BigDecimal prix, boolean obligatoire) {
        if (obligatoire) {
            return RESPONSABILITE_CIVILE.equals(code) ? RAISON_RESPONSABILITE_CIVILE : RAISON_OBLIGATOIRE;
        }
        return prix == null ? RAISON_SANS_PRIX : null;
    }

    private Map<String, BigDecimal> prixDeLaGrille(Contrat contrat, DonneesContrat donnees) {
        if (!Contrat.AUTO.equals(contrat.getProduit())) {
            return tarifsHabitation.prixDesOptions(donnees);
        }
        Conducteur conducteur = donneesContrat.conducteurPrincipal(contrat.getId())
                .flatMap(conducteurs::findById)
                .orElseThrow(MoteurIndisponibleException::new);
        if (contrat.getVehicule() == null) {
            throw new MoteurIndisponibleException();
        }
        return formulesAuto.prixDesOptions(contrat.getVehicule(), conducteur, contrat.getFormule());
    }
}
