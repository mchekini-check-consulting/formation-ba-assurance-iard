package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.util.Locale;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import net.checkconsulting.assuranceiard.api.config.ReinitialisationProperties;
import net.checkconsulting.assuranceiard.api.exception.LienInvalideException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.LienReinitialisation;
import net.checkconsulting.assuranceiard.api.repository.CodeVerificationRepository;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.LienReinitialisationRepository;
import net.checkconsulting.assuranceiard.api.utils.Jetons;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Mot de passe oublié (US-07) : RG1, réponse identique que le compte existe ou non ; RG2, lien à usage unique valable
 * 24 heures par défaut (réglage LIEN_REINITIALISATION_VALIDITE). Le jeton n'est stocké que haché et n'est jamais
 * écrit dans les logs.
 */
@Slf4j
@Service
public class ReinitialisationService {

    private static final String PAGE_REINITIALISATION = "/reinitialisation-mot-de-passe?jeton=";

    private final CompteRepository compteRepository;
    private final LienReinitialisationRepository lienRepository;
    private final CodeVerificationRepository codeRepository;
    private final SessionService sessionService;
    private final EnvoiLienReinitialisation envoiLien;
    private final PasswordEncoder passwordEncoder;
    private final ConnexionProperties proprietes;
    private final ReinitialisationProperties reglages;
    private final Clock horloge;
    private final Executor executeur;
    private final TransactionOperations transaction;

    @SuppressWarnings("PMD.ExcessiveParameterList")
    public ReinitialisationService(
            CompteRepository compteRepository,
            LienReinitialisationRepository lienRepository,
            CodeVerificationRepository codeRepository,
            SessionService sessionService,
            EnvoiLienReinitialisation envoiLien,
            PasswordEncoder passwordEncoder,
            ConnexionProperties proprietes,
            ReinitialisationProperties reglages,
            Clock horloge,
            @Qualifier("applicationTaskExecutor") Executor executeur,
            TransactionOperations transaction) {
        this.compteRepository = compteRepository;
        this.lienRepository = lienRepository;
        this.codeRepository = codeRepository;
        this.sessionService = sessionService;
        this.envoiLien = envoiLien;
        this.passwordEncoder = passwordEncoder;
        this.proprietes = proprietes;
        this.reglages = reglages;
        this.horloge = horloge;
        this.executeur = executeur;
        this.transaction = transaction;
    }

    /**
     * RG1 : la demande est traitée en arrière-plan. La réponse part tout de suite et prend le même temps que le compte
     * existe ou non. Un nouveau lien annule le précédent : un seul reste valable.
     */
    public void demander(String email) {
        String adresse = email.trim().toLowerCase(Locale.ROOT);
        executeur.execute(() -> traiter(adresse));
    }

    @Transactional
    public void verifier(String jeton) {
        trouverLienValable(jeton);
    }

    /** Le client a prouvé qu'il contrôle sa boîte mail : le blocage est levé et tout accès en cours est coupé. */
    @Transactional
    public void reinitialiser(String jeton, String motDePasse) {
        LienReinitialisation lien = trouverLienValable(jeton);
        Compte compte = lien.getCompte();
        compte.setMotDePasseHash(passwordEncoder.encode(motDePasse));
        compte.setEchecsConnexion(0);
        compte.setBloqueJusquA(null);
        codeRepository.deleteByCompte(compte);
        sessionService.fermerToutes(compte);
        lienRepository.delete(lien);
    }

    /** Le lien est enregistré avant l'envoi de l'e-mail : le client ne reçoit jamais un lien encore inconnu. */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void traiter(String adresse) {
        try {
            String lien = transaction.execute(etat -> compteRepository.findByEmail(adresse)
                    .map(this::creerUnLien)
                    .orElse(null));
            if (lien != null) {
                envoiLien.envoyer(adresse, lien);
            }
        } catch (RuntimeException exception) {
            // Le message de l'exception peut contenir l'adresse ou le lien : seul son type est journalisé.
            log.error("Demande de réinitialisation non traitée ({})", exception.getClass().getSimpleName());
        }
    }

    private String creerUnLien(Compte compte) {
        lienRepository.deleteByCompte(compte);
        String jeton = Jetons.nouveauJeton();
        LienReinitialisation lien = new LienReinitialisation();
        lien.setCompte(compte);
        lien.setJetonHash(Jetons.empreinte(jeton));
        lien.setDateExpiration(horloge.instant().plus(reglages.validiteLien()));
        lienRepository.save(lien);
        return proprietes.urlFrontend() + PAGE_REINITIALISATION + jeton;
    }

    private LienReinitialisation trouverLienValable(String jeton) {
        return lienRepository.findByJetonHash(Jetons.empreinte(jeton))
                .filter(lien -> horloge.instant().isBefore(lien.getDateExpiration()))
                .orElseThrow(LienInvalideException::new);
    }
}
