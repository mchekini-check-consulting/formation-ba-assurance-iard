package net.checkconsulting.assuranceiard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** {@code ancienPrixMensuel} n'est rempli que si le devis avait expiré et que son prix a été recalculé. */
public record RepriseDevisResponse(
        UUID reference,
        LogementDevis logement,
        BesoinsDevis besoins,
        String formule,
        List<String> options,
        String franchise,
        BigDecimal prixMensuel,
        BigDecimal ancienPrixMensuel,
        LocalDate dateFinValidite) {
}
