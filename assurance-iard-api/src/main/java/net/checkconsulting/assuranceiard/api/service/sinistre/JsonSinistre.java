package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.MiseAJourSinistreRequest.Tiers;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** US-24 : colonnes JSON du sinistre (tiers_infos, declarant_infos, details), lues et écrites ici seulement. */
@Component
@RequiredArgsConstructor
public class JsonSinistre {

    private static final TypeReference<LinkedHashMap<String, String>> TEXTES = new TypeReference<>() {
    };
    static final String NOM = "nom";
    static final String LIEN = "lien";

    private final JsonMapper json;

    /** Qui déclare pour l'assuré (AC13) : nom et lien. */
    public record Declarant(String nom, String lien) {
    }

    public String ecrire(Object valeur) {
        return valeur == null ? null : json.writeValueAsString(valeur);
    }

    public Tiers tiers(String colonne) {
        return colonne == null ? null : json.readValue(colonne, Tiers.class);
    }

    public Map<String, String> textes(String colonne) {
        return colonne == null ? Map.of() : json.readValue(colonne, TEXTES);
    }

    public Declarant declarant(String colonne) {
        Map<String, String> infos = textes(colonne);
        return infos.isEmpty() ? null : new Declarant(infos.get(NOM), infos.get(LIEN));
    }

    public String ecrireDeclarant(String nom, String lien) {
        if (nom == null && lien == null) {
            return null;
        }
        Map<String, String> infos = new LinkedHashMap<>();
        infos.put(NOM, nom);
        infos.put(LIEN, lien);
        return ecrire(infos);
    }
}
