package net.checkconsulting.assuranceiard.api.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * T-03 : réglages des documents PDF générés. Les textes métier (mentions de l'assureur, garanties, exclusions) sont
 * des valeurs fictives PROVISOIRES, à valider ou fournir par le PO : ils se changent dans application.yaml, sans
 * toucher au code.
 *
 * @param assureur mentions légales de l'assureur, reprises sur chaque document.
 * @param garanties garanties incluses par formule (clé : code de la formule, ex. confort, TIERS).
 * @param exclusions renvoi aux exclusions, imprimé sur les conditions particulières.
 * @param exclusionsParFormule US-33 : liste des principales exclusions par code de formule auto (TIERS, INTER, TR).
 * @param tailleMax taille maximum d'un PDF (5 Mo ; la base refuse au-delà).
 * @param reprise tâche qui stocke les documents pas encore stockés.
 */
@ConfigurationProperties("assurance.documents")
public record DocumentsProperties(
        Assureur assureur,
        Map<String, List<String>> garanties,
        String exclusions,
        Map<String, List<String>> exclusionsParFormule,
        DataSize tailleMax,
        Reprise reprise) {

    public DocumentsProperties {
        garanties = garanties == null ? Map.of() : Map.copyOf(garanties);
        exclusionsParFormule = exclusionsParFormule == null ? Map.of() : Map.copyOf(exclusionsParFormule);
        tailleMax = tailleMax == null ? DataSize.ofMegabytes(5) : tailleMax;
        reprise = reprise == null ? new Reprise(true, Duration.ofMinutes(5), Duration.ofMinutes(1), 50) : reprise;
    }

    public List<String> garantiesDe(String formule) {
        return formule == null ? List.of() : garanties.getOrDefault(formule, List.of());
    }

    public List<String> exclusionsDe(String formule) {
        return formule == null ? List.of() : exclusionsParFormule.getOrDefault(formule, List.of());
    }

    public record Assureur(String nom, String adresse, String immatriculation, String controle) {
    }

    /**
     * @param active false pour l'arrêter (tests).
     * @param intervalle délai entre deux passages (5 minutes).
     * @param delaiInitial délai avant le premier passage après le démarrage.
     * @param lot nombre maximum de documents traités par passage.
     */
    public record Reprise(boolean active, Duration intervalle, Duration delaiInitial, int lot) {
    }
}
