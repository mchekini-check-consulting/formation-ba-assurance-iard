package net.checkconsulting.assuranceiard.api.service.avenant;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.AvenantResponse;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.dto.ProjetAvenantResponse;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.service.EspaceClientService;
import net.checkconsulting.assuranceiard.api.service.document.MiseEnForme;
import net.checkconsulting.assuranceiard.api.service.document.ModeleIPIDauto;
import net.checkconsulting.assuranceiard.api.service.facture.EcheancierService;
import org.springframework.stereotype.Component;

/**
 * US-23, AC3 : ce que l'écran « Relire et accepter l'avenant » affiche : ancienne et nouvelle cotisation, date
 * d'effet et première échéance concernée (RG3), conseil mis à jour si la couverture diminue (RG4), documents à
 * relire (fiche conseil, IPID, conditions générales).
 * <ul>
 *   <li>Fiche conseil et IPID : les modèles d'US-29 sont sur main, mais la fiche conseil auto se rédige depuis un
 *       devis (pas depuis un contrat). L'écran propose donc l'IPID de la formule auto (modèle US-29) et les
 *       documents existants du contrat ; le conseil mis à jour s'affiche à l'écran (RG4).</li>
 *   <li>RG3 : paiement mensuel, la nouvelle mensualité s'applique à la première échéance qui suit la date d'effet
 *       (sans prorata) ; paiement annuel, à la prochaine échéance annuelle.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ProjetsAvenant {

    static final String ANNUEL = "ANNUEL";
    static final String CONSEIL_GENERAL = "Votre couverture diminue : nous avons mis à jour notre conseil avant "
            + "votre acceptation (article L521-4 du Code des assurances). Vérifiez que vous n'avez plus besoin des "
            + "garanties retirées.";
    private static final Set<String> DOCUMENTS_A_RELIRE = Set.of(TypeDocument.FICHE_CONSEIL.name(),
            TypeDocument.IPID.name(), TypeDocument.CONDITIONS_GENERALES.name());
    /** RG4 : ce que protège chaque garantie, pour le conseil mis à jour. */
    private static final Map<String, String> PROTECTIONS = Map.of(
            "objets-valeur", "vos objets de valeur (bijoux, œuvres d'art)",
            "assistance-depannage", "le dépannage d'urgence de votre logement",
            "protection-juridique", "vos frais de litige et l'accompagnement juridique",
            "PROTECTION_JURIDIQUE", "vos frais de litige et l'accompagnement juridique",
            "ASSISTANCE_0KM", "le dépannage de votre véhicule, même devant chez vous",
            "VEHICULE_REMPLACEMENT", "un véhicule de remplacement pendant l'immobilisation du vôtre",
            "PROTECTION_CONDUCTEUR", "vos propres blessures en cas d'accident responsable");

    private final EspaceClientService espaceClient;
    private final EcheancierService echeancier;

    public ProjetAvenantResponse projet(Contrat contrat, DonneesContrat donnees, AvenantResponse avenant) {
        boolean reduction = !avenant.retraits().isEmpty();
        String frequence = donnees.paiement().frequence();
        return new ProjetAvenantResponse(avenant,
                CalculAvenant.annuelle(avenant.ancienneCotisationMensuelle()),
                CalculAvenant.annuelle(avenant.nouvelleCotisationMensuelle()),
                frequence,
                premiereEcheanceConcernee(contrat.getStatut(), donnees, avenant.dateEffet()),
                reduction,
                reduction ? conseils(avenant) : List.of(),
                espaceClient.documentsDeLaFiche(contrat).stream()
                        .filter(document -> DOCUMENTS_A_RELIRE.contains(document.type()))
                        .toList(),
                Contrat.AUTO.equals(contrat.getProduit()) && ModeleIPIDauto.FORMULES_AUTO.contains(contrat.getFormule())
                        ? contrat.getFormule() : null);
    }

    /** RG4 : un conseil général, puis ce que chaque garantie retirée ne couvrira plus. */
    static List<String> conseils(AvenantResponse avenant) {
        List<String> conseils = new ArrayList<>();
        conseils.add(CONSEIL_GENERAL);
        avenant.retraits().forEach(garantie -> conseils.add("Sans « " + garantie.libelle() + " », vous ne serez "
                + "plus couvert pour " + PROTECTIONS.getOrDefault(garantie.code(), "ce qu'elle protège")
                + " à partir du " + MiseEnForme.date(avenant.dateEffet()) + "."));
        return conseils;
    }

    /**
     * RG3 : première échéance de l'échéancier (US-22) à la date d'effet ou après ; au-delà de l'échéancier calculé
     * (fin de l'année d'assurance en cours), l'échéance suivante dans le même rythme. Vide sans échéancier.
     */
    LocalDate premiereEcheanceConcernee(String statut, DonneesContrat donnees, LocalDate dateEffet) {
        List<LocalDate> dates = echeancier.de(statut, donnees).echeances().stream()
                .map(EcheancierService.LigneEcheance::date)
                .sorted(Comparator.naturalOrder())
                .toList();
        if (dates.isEmpty()) {
            return null;
        }
        boolean annuel = ANNUEL.equals(donnees.paiement().frequence());
        LocalDate derniere = dates.getLast();
        LocalDate date = dates.stream().filter(candidate -> !candidate.isBefore(dateEffet)).findFirst()
                .orElse(derniere);
        for (int rang = 1; date.isBefore(dateEffet); rang++) {
            date = annuel ? derniere.plusYears(rang) : derniere.plusMonths(rang);
        }
        return date;
    }
}
