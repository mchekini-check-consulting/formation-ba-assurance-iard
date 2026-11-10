package net.checkconsulting.assuranceiard.api.logging;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;

final class ContexteMdc implements AutoCloseable {

    private final Map<String, String> precedentes = new LinkedHashMap<>();

    private ContexteMdc() {
    }

    static ContexteMdc ouvrir() {
        return new ContexteMdc();
    }

    ContexteMdc avec(String cle, String valeur) {
        precedentes.putIfAbsent(cle, MDC.get(cle));
        MDC.put(cle, valeur);
        return this;
    }

    ContexteMdc avec(Map<String, String> valeurs) {
        valeurs.forEach(this::avec);
        return this;
    }

    @Override
    public void close() {
        precedentes.forEach((cle, precedente) -> {
            if (precedente == null) {
                MDC.remove(cle);
            } else {
                MDC.put(cle, precedente);
            }
        });
    }
}
