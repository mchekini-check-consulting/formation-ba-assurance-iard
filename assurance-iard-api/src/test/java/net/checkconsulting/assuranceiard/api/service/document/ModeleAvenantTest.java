package net.checkconsulting.assuranceiard.api.service.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import net.checkconsulting.assuranceiard.api.model.AvenantLigne;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.AvenantLigneRepository;
import net.checkconsulting.assuranceiard.api.repository.AvenantRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import org.junit.jupiter.api.Test;

/**
 * US-23 : contenu obligatoire de l'avenant (numéro d'avenant, contrat, date d'effet, garanties ajoutées ou retirées,
 * ancienne et nouvelle cotisation, date et heure de l'acceptation).
 */
class ModeleAvenantTest {

    private final AvenantRepository avenants = mock(AvenantRepository.class);
    private final AvenantLigneRepository lignes = mock(AvenantLigneRepository.class);
    private final DonneesContratRepository contrats = mock(DonneesContratRepository.class);
    private final ModeleAvenant modele = new ModeleAvenant(avenants, lignes, contrats);

    private static Avenant avenant(String statut, Instant accepteLe) {
        Avenant avenant = new Avenant();
        avenant.setId(9L);
        avenant.setNumero(2);
        avenant.setContratId(3L);
        avenant.setStatut(statut);
        avenant.setDateEffet(LocalDate.of(2026, 11, 27));
        avenant.setPrimeAvantTtc(new BigDecimal("47.50"));
        avenant.setPrimeApresTtc(new BigDecimal("48.50"));
        avenant.setAccepteLe(accepteLe);
        avenant.setDateCreation(Instant.parse("2026-11-26T09:00:00Z"));
        return avenant;
    }

    private static AvenantLigne ligne(String code, String action, String prix) {
        AvenantLigne ligne = new AvenantLigne();
        ligne.setCodeOption(code);
        ligne.setAction(action);
        ligne.setPrixMensuelTtc(new BigDecimal(prix));
        return ligne;
    }

    private void contrat() {
        when(contrats.lire(3L)).thenReturn(Optional.of(new DonneesContrat(3L, "AE-RECETTE-A03", "AUTO", "TR",
                new DonneesContrat.Souscripteur("madame", "Camille", "Durand"), null, null, null, null, null, null,
                List.of())));
    }

    private static String texte(ContenuDocument contenu) {
        return contenu.sections().stream()
                .map(section -> section.titre() + "\n" + String.join("\n", section.lignes()))
                .reduce("", (a, b) -> a + "\n" + b);
    }

    @Test
    void contenuObligatoireDeLAvenantAccepte() {
        Document document = new Document();
        document.setReference("AV-2026-000009");
        when(avenants.findById(9L)).thenReturn(Optional.of(avenant("ACCEPTE", Instant.parse("2026-11-26T14:05:00Z"))));
        when(lignes.findByAvenantIdOrderById(9L)).thenReturn(List.of(ligne("ASSISTANCE_0KM", "AJOUT", "3.50"),
                ligne("PROTECTION_JURIDIQUE", "RETRAIT", "2.50")));
        contrat();

        ContenuDocument contenu = modele.rediger(document);

        assertThat(modele.type()).isEqualTo(TypeDocument.AVENANT);
        assertThat(contenu.titre()).isEqualTo("Avenant");
        assertThat(contenu.numero()).isEqualTo("AV-2026-000009");
        assertThat(texte(contenu)).contains(
                "Avenant n° AV-2026-000009",
                "Contrat n° AE-RECETTE-A03",
                "Madame Camille Durand",
                "Date d'effet : 27/11/2026",
                "Garanties ajoutées", "Assistance dès 0 km : + 3,50 € TTC par mois",
                "Garanties retirées", "Protection juridique : - 2,50 € TTC par mois",
                "Ancienne cotisation : 47,50 € TTC par mois (570,00 € TTC par an)",
                "Nouvelle cotisation : 48,50 € TTC par mois (582,00 € TTC par an)",
                // 14h05 UTC = 15h05 à Paris.
                "Avenant accepté en ligne le 26/11/2026 à 15:05 (heure de Paris)");
    }

    @Test
    void projetDAvenantDesDonneesDeTestRetrouveParSonDocument() {
        Document document = new Document();
        document.setId(40L);
        when(avenants.findFirstByDocumentId(40L)).thenReturn(Optional.of(avenant("EN_ATTENTE", null)));
        when(lignes.findByAvenantIdOrderById(9L)).thenReturn(List.of(ligne("objets-valeur", "AJOUT", "2.50")));
        contrat();

        ContenuDocument contenu = modele.rediger(document);

        assertThat(contenu.titre()).isEqualTo("Projet d'avenant");
        assertThat(texte(contenu)).contains("Objets de valeur : + 2,50 €", "Garanties retirées\nAucune",
                "pas encore accepté");
    }

    @Test
    void avenantIntrouvable() {
        Document document = new Document();
        document.setId(41L);
        when(avenants.findFirstByDocumentId(41L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modele.rediger(document)).isInstanceOf(IllegalStateException.class);
    }
}
