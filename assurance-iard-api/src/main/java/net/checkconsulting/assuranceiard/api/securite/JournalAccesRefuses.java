package net.checkconsulting.assuranceiard.api.securite;

import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.AccesRefuseResponse;
import net.checkconsulting.assuranceiard.api.model.AccesRefuse;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.repository.AccesRefuseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** US-04, RG3 : journal des accès refusés, consultable par l'administrateur. */
@Slf4j
@Service
@RequiredArgsConstructor
public class JournalAccesRefuses {

    private static final int LONGUEUR_CHEMIN = 255;
    private static final int LONGUEUR_IDENTIFIANT = 100;

    private final AccesRefuseRepository repository;
    private final Clock horloge;

    /** Transaction à part : la ligne reste enregistrée alors que la requête échoue. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enregistrer(
            Compte compte, Acces acces, String identifiant, ControleAccesService.Requete requete, MotifRefus motif) {
        AccesRefuse ligne = new AccesRefuse();
        ligne.setDateTentative(horloge.instant());
        ligne.setCompteId(compte.getId());
        ligne.setEmail(compte.getEmail());
        ligne.setProfil(compte.getProfil());
        ligne.setMethode(requete.methode());
        ligne.setChemin(tronquer(requete.chemin(), LONGUEUR_CHEMIN));
        ligne.setRessource(acces.ressource());
        ligne.setIdentifiant(tronquer(identifiant, LONGUEUR_IDENTIFIANT));
        ligne.setRubrique(acces.rubrique());
        ligne.setDroit(acces.droit());
        ligne.setMotif(motif);
        repository.save(ligne);
        // Ni e-mail ni identifiant dans les logs : seulement de quoi repérer le refus dans le journal.
        log.warn("Accès refusé : rubrique={} droit={} ressource={} motif={}",
                acces.rubrique(), acces.droit(), acces.ressource(), motif);
    }

    @Transactional(readOnly = true)
    public List<AccesRefuseResponse> derniers() {
        return repository.findTop200ByOrderByDateTentativeDesc().stream()
                .map(l -> new AccesRefuseResponse(l.getDateTentative(), l.getEmail(), l.getProfil().name(),
                        l.getMethode(), l.getChemin(), l.getRessource().name(), l.getIdentifiant(),
                        l.getRubrique().name(), l.getDroit().name(), l.getMotif().name()))
                .toList();
    }

    static String tronquer(String texte, int longueur) {
        return texte == null || texte.length() <= longueur ? texte : texte.substring(0, longueur);
    }
}
