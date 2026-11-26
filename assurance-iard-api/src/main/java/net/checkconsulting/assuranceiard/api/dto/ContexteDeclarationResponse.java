package net.checkconsulting.assuranceiard.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * US-24, AC1 et AC10 : de quoi commencer une déclaration : prénom du client, numéro d'assistance 24h/24 et contrats
 * ouverts à la déclaration (aucun n'est présélectionné, même s'il n'y en a qu'un).
 */
public record ContexteDeclarationResponse(String prenom, String numeroAssistance, List<ContratDeclarable> contrats) {

    public record ContratDeclarable(String numero, String produit, String statut, String formule, String bienAssure,
            LocalDate dateEffet, LocalDate dateFin) {
    }
}
