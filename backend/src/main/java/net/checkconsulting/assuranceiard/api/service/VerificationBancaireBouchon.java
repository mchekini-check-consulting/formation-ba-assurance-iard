package net.checkconsulting.assuranceiard.api.service;

import org.springframework.stereotype.Component;

/** RG2 de l'US-14 : aucune banque n'est contactée pour l'instant. Tout compte est considéré comme valide. */
@Component
public class VerificationBancaireBouchon implements VerificationBancaire {

    @Override
    public boolean compteValide(String iban) {
        return true;
    }
}
