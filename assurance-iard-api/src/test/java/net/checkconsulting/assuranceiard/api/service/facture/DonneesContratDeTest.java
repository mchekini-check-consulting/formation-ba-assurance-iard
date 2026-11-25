package net.checkconsulting.assuranceiard.api.service.facture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;

/** US-22 : contrats fictifs pour les tests de facturation. */
final class DonneesContratDeTest {

    static final long ID = 12L;

    private DonneesContratDeTest() {
    }

    static DonneesContrat contrat(String produit, LocalDate effet, LocalDate fin, String frequence, Integer jour,
            String mensuelle, List<DonneesContrat.Option> options) {
        BigDecimal parMois = new BigDecimal(mensuelle);
        return new DonneesContrat(ID, "AE-2026-000012", produit, "AUTO".equals(produit) ? "TR" : "confort",
                new DonneesContrat.Souscripteur("madame", "Camille", "Durand"),
                new DonneesContrat.Logement("appartement", 45, "12 rue des Lilas", "75001", "Paris"),
                new DonneesContrat.Vehicule(null, null, null, null),
                new DonneesContrat.Conducteur(null, null),
                new DonneesContrat.Tarif(new BigDecimal("300.00"), parMois, parMois.multiply(BigDecimal.valueOf(12)),
                        effet, fin),
                new DonneesContrat.Paiement(frequence, jour, "•••• 4821", "AE-RUM-1"),
                null,
                options);
    }

    /** Contrat sans cotisation connue. */
    static DonneesContrat sansCotisation(LocalDate effet) {
        DonneesContrat modele = habitation(effet, null, 10);
        return new DonneesContrat(modele.id(), modele.numero(), modele.produit(), modele.formule(),
                modele.souscripteur(), modele.logement(), modele.vehicule(), modele.conducteur(),
                new DonneesContrat.Tarif(null, null, null, effet, null), modele.paiement(), null, List.of());
    }

    static DonneesContrat habitation(LocalDate effet, LocalDate fin, Integer jour) {
        return contrat("HABITATION", effet, fin, "MENSUEL", jour, "18.50", List.of());
    }
}
