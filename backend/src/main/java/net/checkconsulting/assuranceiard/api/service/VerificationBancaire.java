package net.checkconsulting.assuranceiard.api.service;

/** RG2 de l'US-14 : demande à la banque si le compte existe et accepte les prélèvements. */
public interface VerificationBancaire {

    boolean compteValide(String iban);
}
