package net.checkconsulting.assuranceiard.api.exception;

/** US-24 : une déclaration envoyée ne se modifie plus (seules des pièces peuvent encore être ajoutées, AC7). */
public class DeclarationDejaEnvoyeeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String MESSAGE = "Cette déclaration a déjà été envoyée : elle ne peut plus être modifiée.";

    public DeclarationDejaEnvoyeeException() {
        super(MESSAGE);
    }
}
