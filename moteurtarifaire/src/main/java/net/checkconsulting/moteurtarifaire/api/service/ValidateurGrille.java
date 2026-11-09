package net.checkconsulting.moteurtarifaire.api.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.moteurtarifaire.api.exception.GrilleInvalideException;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.Dependances;
import net.checkconsulting.moteurtarifaire.api.enums.Etage;
import net.checkconsulting.moteurtarifaire.api.enums.Formule;
import net.checkconsulting.moteurtarifaire.api.utils.LimitesSaisie;
import net.checkconsulting.moteurtarifaire.api.enums.OptionTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.RaisonRecommandation;
import net.checkconsulting.moteurtarifaire.api.enums.Residence;
import net.checkconsulting.moteurtarifaire.api.enums.StatutOccupant;
import net.checkconsulting.moteurtarifaire.api.enums.TypeLogement;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidateurGrille {

    private final Validator validateur;

    public void verifier(GrilleTarifaire grille) {
        verifierContraintes(grille);
        verifierCompletude(grille);
    }

    private void verifierContraintes(GrilleTarifaire grille) {
        Set<ConstraintViolation<GrilleTarifaire>> violations = validateur.validate(grille);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(violation -> violation.getPropertyPath() + " : " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining(" ; "));
            throw new GrilleInvalideException("Grille " + grille.version() + " invalide : " + detail);
        }
    }

    private void verifierCompletude(GrilleTarifaire grille) {
        GrilleTarifaire.Multiplicateurs multiplicateurs = grille.multiplicateurs();
        exigerToutes(grille, "prix-base", grille.prixBase(), Formule.class);
        exigerToutes(grille, "prix-option", grille.prixOptions(), OptionTarifaire.class);
        exigerToutes(grille, "raison", grille.recommandation().raisons(), RaisonRecommandation.class);
        exigerToutes(grille, "type-logement", multiplicateurs.typeLogement(), TypeLogement.class);
        exigerToutes(grille, "residence", multiplicateurs.residence(), Residence.class);
        exigerToutes(grille, "etage", multiplicateurs.etage(), Etage.class);
        exigerToutes(grille, "dependances", multiplicateurs.dependances(), Dependances.class);
        Arrays.stream(StatutOccupant.values())
                .filter(statut -> !grille.refus().statuts().containsKey(statut))
                .filter(statut -> !multiplicateurs.statut().containsKey(statut))
                .findFirst()
                .ifPresent(statut -> signalerManque(grille, "statut", statut));
        IntStream.rangeClosed(LimitesSaisie.SINISTRES_MIN, LimitesSaisie.SINISTRES_MAX)
                .boxed()
                .filter(nombre -> !multiplicateurs.sinistres().containsKey(nombre))
                .findFirst()
                .ifPresent(nombre -> signalerManque(grille, "sinistres", nombre));
    }

    private <E extends Enum<E>> void exigerToutes(GrilleTarifaire grille, String categorie, Map<E, ?> valeurs, Class<E> type) {
        Arrays.stream(type.getEnumConstants())
                .filter(cle -> !valeurs.containsKey(cle))
                .findFirst()
                .ifPresent(cle -> signalerManque(grille, categorie, cle));
    }

    private void signalerManque(GrilleTarifaire grille, String categorie, Object cle) {
        throw new GrilleInvalideException(
                "Grille " + grille.version() + " incomplète : " + categorie + " sans valeur pour " + cle + ".");
    }
}
