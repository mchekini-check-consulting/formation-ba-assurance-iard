package net.checkconsulting.assuranceiard.api.securite;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.checkconsulting.assuranceiard.api.exception.AccesNonAutoriseException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.repository.PermissionRepository;
import org.springframework.stereotype.Service;

/**
 * US-04 : la règle d'accès, en un seul endroit.
 * <ol>
 *   <li>Back-office : profils internes uniquement (un assuré ne voit jamais le back-office).</li>
 *   <li>RG2 : le droit doit être accordé explicitement au profil ; il est relu en base à chaque requête (AC5).</li>
 *   <li>AC1/AC2 : un client (Prospect, Assuré) n'accède qu'à ses propres ressources.</li>
 * </ol>
 * Chaque refus est enregistré dans le journal (RG3) puis renvoyé en « Accès non autorisé ».
 */
@Service
public class ControleAccesService {

    /** Profils du back-office : ils voient les ressources de tous les clients, selon leurs droits. */
    public static final Set<Profil> PROFILS_INTERNES = EnumSet.of(Profil.GESTIONNAIRE, Profil.ADMINISTRATEUR);

    private final PermissionRepository permissions;
    private final JournalAccesRefuses journal;
    private final Map<TypeRessource, RessourceProtegee> ressources;

    public ControleAccesService(
            PermissionRepository permissions, JournalAccesRefuses journal, List<RessourceProtegee> ressources) {
        this.permissions = permissions;
        this.journal = journal;
        this.ressources = ressources.stream()
                .collect(Collectors.toUnmodifiableMap(RessourceProtegee::type, Function.identity()));
    }

    /** Refuse (et journalise) si le compte n'a pas l'accès demandé. */
    public void verifier(Compte compte, Acces acces, String identifiant, Requete requete) {
        Optional<MotifRefus> motif = motifDeRefus(compte, acces, identifiant);
        if (motif.isPresent()) {
            journal.enregistrer(compte, acces, identifiant, requete, motif.get());
            throw new AccesNonAutoriseException();
        }
    }

    private Optional<MotifRefus> motifDeRefus(Compte compte, Acces acces, String identifiant) {
        boolean interne = PROFILS_INTERNES.contains(compte.getProfil());
        if (acces.backOffice() && !interne) {
            return Optional.of(MotifRefus.BACK_OFFICE);
        }
        if (!permissions.existsByProfilAndRubriqueAndDroitAndValeurTrue(
                compte.getProfil(), acces.rubrique(), acces.droit())) {
            return Optional.of(MotifRefus.DROIT_ABSENT);
        }
        if (!interne && acces.ressource() != TypeRessource.AUCUNE && !proprietaire(compte, acces, identifiant)) {
            return Optional.of(MotifRefus.PAS_PROPRIETAIRE);
        }
        return Optional.empty();
    }

    /** Une ressource introuvable n'est pas refusée ici : la route répond « introuvable », sans rien montrer. */
    private boolean proprietaire(Compte compte, Acces acces, String identifiant) {
        RessourceProtegee ressource = ressources.get(acces.ressource());
        if (ressource == null) {
            throw new IllegalStateException("Ressource sans contrôle de propriétaire : " + acces.ressource());
        }
        Set<Long> proprietaires = ressource.proprietaires(identifiant);
        return proprietaires.isEmpty() || proprietaires.contains(compte.getId());
    }

    /** Ce que le journal garde de la requête : méthode et chemin, jamais le corps ni les paramètres. */
    public record Requete(String methode, String chemin) {
    }
}
