package net.checkconsulting.assuranceiard.api.dto;

/**
 * US-22, AC2 : facture générée, ou déjà générée pour cette période (même numéro, pas de doublon).
 *
 * @param disponible faux si le PDF n'a pas pu être produit tout de suite (la reprise de T-03 le stockera).
 * @param existante vrai si la facture existait déjà pour cette période.
 */
public record FactureGenereeResponse(MesFacturesResponse.Facture facture, boolean disponible, boolean existante) {
}
