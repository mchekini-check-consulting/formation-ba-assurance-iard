package net.checkconsulting.moteurtarifaire.api.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationRequest;
import net.checkconsulting.moteurtarifaire.api.enums.TypeLogement;

public class ChampsConditionnelsValidator implements ConstraintValidator<ChampsConditionnels, TarifHabitationRequest> {

    private static final String CHAMP_ETAGE = "etage";
    private static final String CHAMP_DEPENDANCES = "dependances";
    private static final String MESSAGE_ETAGE = "{tarif.champConditionnel.etage}";
    private static final String MESSAGE_DEPENDANCES = "{tarif.champConditionnel.dependances}";

    @Override
    public boolean isValid(TarifHabitationRequest demande, ConstraintValidatorContext contexte) {
        if (demande.typeLogement() == TypeLogement.APPARTEMENT && demande.etage() == null) {
            return signaler(contexte, CHAMP_ETAGE, MESSAGE_ETAGE);
        }
        if (demande.typeLogement() == TypeLogement.MAISON && demande.dependances() == null) {
            return signaler(contexte, CHAMP_DEPENDANCES, MESSAGE_DEPENDANCES);
        }
        return true;
    }

    private boolean signaler(ConstraintValidatorContext contexte, String champ, String message) {
        contexte.disableDefaultConstraintViolation();
        contexte.buildConstraintViolationWithTemplate(message).addPropertyNode(champ).addConstraintViolation();
        return false;
    }
}
