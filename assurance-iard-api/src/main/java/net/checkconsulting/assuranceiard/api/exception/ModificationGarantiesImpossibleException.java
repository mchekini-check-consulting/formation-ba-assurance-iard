package net.checkconsulting.assuranceiard.api.exception;

/** US-23, AC6 : le contrat n'est pas actif (Impayé, Suspendu, résilié...) : ses garanties ne se modifient pas. */
public class ModificationGarantiesImpossibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ModificationGarantiesImpossibleException() {
        super("Les garanties de ce contrat ne peuvent pas être modifiées dans sa situation actuelle.");
    }
}
