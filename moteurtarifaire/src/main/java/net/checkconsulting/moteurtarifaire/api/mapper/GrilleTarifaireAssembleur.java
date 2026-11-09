package net.checkconsulting.moteurtarifaire.api.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleInvalideException;
import net.checkconsulting.moteurtarifaire.api.model.ParametreTarifaire;
import net.checkconsulting.moteurtarifaire.api.model.VersionGrille;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.Dependances;
import net.checkconsulting.moteurtarifaire.api.enums.Etage;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.RaisonRecommandation;
import net.checkconsulting.moteurtarifaire.api.enums.Residence;
import net.checkconsulting.moteurtarifaire.api.enums.ResiliationAnterieure;
import net.checkconsulting.moteurtarifaire.api.enums.StatutOccupant;
import net.checkconsulting.moteurtarifaire.api.enums.TypeLogement;
import org.springframework.stereotype.Component;

@Component
public class GrilleTarifaireAssembleur {

    private static final String GENERAL = "general";
    private static final String ARRONDI = "arrondi";
    private static final String COEFFICIENT_DEPART = "coefficient-depart";
    private static final String RECOMMANDATION = "recommandation";

    public GrilleTarifaire assembler(VersionGrille version, List<ParametreTarifaire> parametres) {
        Map<String, Map<String, String>> valeurs = parametres.stream()
                .collect(Collectors.groupingBy(
                        ParametreTarifaire::getCategorie,
                        Collectors.toMap(ParametreTarifaire::getCle, ParametreTarifaire::getValeur)));
        try {
            return new GrilleTarifaire(
                    version.getVersion(),
                    version.getDateDebut(),
                    version.getDateFin(),
                    entier(valeurs, GENERAL, "duree-validite-jours"),
                    arrondi(valeurs),
                    decimauxParEnum(valeurs, "prix-base", Formule.class),
                    coefficientDepart(valeurs),
                    multiplicateurs(valeurs),
                    decimauxParEnum(valeurs, "prix-option", OptionTarifaire.class),
                    decimauxParEntier(valeurs, "franchise"),
                    new GrilleTarifaire.Refus(
                            textesParEnum(valeurs, "refus-statut", StatutOccupant.class),
                            textesParEnum(valeurs, "refus-resiliation", ResiliationAnterieure.class)),
                    new GrilleTarifaire.ReglesRecommandation(
                            decimal(valeurs, RECOMMANDATION, "seuil-contenu-serenite"),
                            decimal(valeurs, RECOMMANDATION, "seuil-contenu-essentielle"),
                            textesParEnum(valeurs, "raison", RaisonRecommandation.class)));
        } catch (IllegalArgumentException erreur) {
            throw new GrilleInvalideException(
                    "Grille " + version.getVersion() + " illisible : " + erreur.getMessage(), erreur);
        }
    }

    private GrilleTarifaire.Arrondi arrondi(Map<String, Map<String, String>> valeurs) {
        String mode = texte(valeurs, ARRONDI, "mode");
        return new GrilleTarifaire.Arrondi(
                entier(valeurs, ARRONDI, "decimales"),
                mode == null ? null : RoundingMode.valueOf(mode));
    }

    private GrilleTarifaire.CoefficientDepart coefficientDepart(Map<String, Map<String, String>> valeurs) {
        return new GrilleTarifaire.CoefficientDepart(
                decimal(valeurs, COEFFICIENT_DEPART, "surface-reference"),
                decimal(valeurs, COEFFICIENT_DEPART, "surface-plafond"),
                decimal(valeurs, COEFFICIENT_DEPART, "pente-surface"),
                decimal(valeurs, COEFFICIENT_DEPART, "coefficient-surface-minimum"),
                entier(valeurs, COEFFICIENT_DEPART, "pieces-reference"),
                decimal(valeurs, COEFFICIENT_DEPART, "increment-par-piece"));
    }

    private GrilleTarifaire.Multiplicateurs multiplicateurs(Map<String, Map<String, String>> valeurs) {
        return new GrilleTarifaire.Multiplicateurs(
                decimauxParEnum(valeurs, "statut", StatutOccupant.class),
                decimauxParEnum(valeurs, "type-logement", TypeLogement.class),
                decimauxParEnum(valeurs, "residence", Residence.class),
                decimauxParEntier(valeurs, "sinistres"),
                zones(valeurs),
                decimauxParEnum(valeurs, "etage", Etage.class),
                decimauxParEnum(valeurs, "dependances", Dependances.class));
    }

    private List<GrilleTarifaire.Zone> zones(Map<String, Map<String, String>> valeurs) {
        Map<BigDecimal, List<String>> departementsParCoefficient = new LinkedHashMap<>();
        categorie(valeurs, "zone").forEach((departement, coefficient) -> departementsParCoefficient
                .computeIfAbsent(new BigDecimal(coefficient), cle -> new ArrayList<>())
                .add(departement));
        return departementsParCoefficient.entrySet().stream()
                .map(zone -> new GrilleTarifaire.Zone(zone.getKey(), zone.getValue()))
                .toList();
    }

    private Map<String, String> categorie(Map<String, Map<String, String>> valeurs, String categorie) {
        return valeurs.getOrDefault(categorie, Map.of());
    }

    private String texte(Map<String, Map<String, String>> valeurs, String categorie, String cle) {
        return categorie(valeurs, categorie).get(cle);
    }

    private BigDecimal decimal(Map<String, Map<String, String>> valeurs, String categorie, String cle) {
        String valeur = texte(valeurs, categorie, cle);
        return valeur == null ? null : new BigDecimal(valeur);
    }

    private Integer entier(Map<String, Map<String, String>> valeurs, String categorie, String cle) {
        String valeur = texte(valeurs, categorie, cle);
        return valeur == null ? null : Integer.valueOf(valeur);
    }

    private <E extends Enum<E>> Map<E, BigDecimal> decimauxParEnum(
            Map<String, Map<String, String>> valeurs, String categorie, Class<E> type) {
        return parEnum(valeurs, categorie, type, BigDecimal::new);
    }

    private <E extends Enum<E>> Map<E, String> textesParEnum(
            Map<String, Map<String, String>> valeurs, String categorie, Class<E> type) {
        return parEnum(valeurs, categorie, type, Function.identity());
    }

    private <E extends Enum<E>, V> Map<E, V> parEnum(
            Map<String, Map<String, String>> valeurs, String categorie, Class<E> type, Function<String, V> conversion) {
        Map<E, V> resultat = new EnumMap<>(type);
        categorie(valeurs, categorie).forEach((cle, valeur) -> resultat.put(Enum.valueOf(type, cle), conversion.apply(valeur)));
        return resultat;
    }

    private Map<Integer, BigDecimal> decimauxParEntier(Map<String, Map<String, String>> valeurs, String categorie) {
        return categorie(valeurs, categorie).entrySet().stream()
                .collect(Collectors.toMap(ligne -> Integer.valueOf(ligne.getKey()), ligne -> new BigDecimal(ligne.getValue())));
    }
}
