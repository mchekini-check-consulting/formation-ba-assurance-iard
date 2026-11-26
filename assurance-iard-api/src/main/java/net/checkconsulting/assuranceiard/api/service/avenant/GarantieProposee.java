package net.checkconsulting.assuranceiard.api.service.avenant;

import java.math.BigDecimal;

/**
 * US-23, AC1 et AC2 : une garantie du contrat, ou une option qu'on peut lui ajouter, avec son prix mensuel TTC.
 *
 * @param souscrite vrai si elle est dans le contrat aujourd'hui
 * @param obligatoire vrai si elle ne peut pas être retirée
 * @param raison pourquoi elle n'est pas modifiable ; vide si elle l'est
 */
public record GarantieProposee(String code, String libelle, BigDecimal prixMensuelTtc, boolean souscrite,
        boolean obligatoire, String raison) {

    public boolean modifiable() {
        return raison == null;
    }
}
