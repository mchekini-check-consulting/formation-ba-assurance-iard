package net.checkconsulting.moteurtarifaire.api.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.moteurtarifaire.api.dto.AjustementFranchiseResponse;
import net.checkconsulting.moteurtarifaire.api.dto.DetailFormuleResponse;
import net.checkconsulting.moteurtarifaire.api.dto.PrixFormuleResponse;
import net.checkconsulting.moteurtarifaire.api.dto.PrixOptionResponse;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalculateurPrix {

    private static final BigDecimal MOIS_PAR_AN = BigDecimal.valueOf(12);

    private final CalculateurCoefficient calculateurCoefficient;

    public List<PrixFormuleResponse> prixDesFormules(ProfilRisque profil, GrilleTarifaire grille) {
        BigDecimal coefficient = calculateurCoefficient.coefficient(profil, grille);
        return Arrays.stream(Formule.values())
                .map(formule -> {
                    BigDecimal mensuel = arrondir(grille.prixBase().get(formule).multiply(coefficient), grille);
                    return new PrixFormuleResponse(formule, mensuel, annuel(mensuel));
                })
                .toList();
    }

    public DetailFormuleResponse detail(
            ProfilRisque profil, Formule formule, List<OptionTarifaire> options, Integer franchise, GrilleTarifaire grille) {
        BigDecimal prixFormule = grille.prixBase().get(formule).multiply(calculateurCoefficient.coefficient(profil, grille));
        List<PrixOptionResponse> lignesOptions = options.stream()
                .distinct()
                .map(option -> new PrixOptionResponse(option, arrondir(grille.prixOptions().get(option), grille)))
                .toList();
        BigDecimal totalOptions = lignesOptions.stream()
                .map(PrixOptionResponse::prixMensuel)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ajustement = franchise == null ? BigDecimal.ZERO : grille.ajustementsFranchise().get(franchise);
        BigDecimal totalMensuel = arrondir(prixFormule.add(totalOptions).add(ajustement), grille);
        log.info("Détail du prix calculé : totalMensuel={}", totalMensuel);
        return new DetailFormuleResponse(
                formule,
                arrondir(prixFormule, grille),
                lignesOptions,
                arrondir(ajustement, grille),
                totalMensuel,
                annuel(totalMensuel));
    }

    public List<PrixOptionResponse> prixDesOptions(GrilleTarifaire grille) {
        return Arrays.stream(OptionTarifaire.values())
                .map(option -> new PrixOptionResponse(option, arrondir(grille.prixOptions().get(option), grille)))
                .toList();
    }

    public List<AjustementFranchiseResponse> ajustementsDesFranchises(GrilleTarifaire grille) {
        return grille.ajustementsFranchise().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(ligne -> new AjustementFranchiseResponse(ligne.getKey(), arrondir(ligne.getValue(), grille)))
                .toList();
    }

    private BigDecimal annuel(BigDecimal mensuel) {
        return mensuel.multiply(MOIS_PAR_AN);
    }

    private BigDecimal arrondir(BigDecimal montant, GrilleTarifaire grille) {
        return montant.setScale(grille.arrondi().decimales(), grille.arrondi().mode());
    }
}
