package net.checkconsulting.assuranceiard.api.exception;

public class CodeSignatureEpuiseException extends RuntimeException {

    public CodeSignatureEpuiseException() {
        super("Nombre d'essais dépassé. Demandez un nouveau code.");
    }
}
