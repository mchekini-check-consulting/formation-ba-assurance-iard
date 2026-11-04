package net.checkconsulting.assuranceiard.api.service;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.config.ConnexionProperties;
import net.checkconsulting.assuranceiard.api.exception.LienInvalideException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.LienReinitialisation;
import net.checkconsulting.assuranceiard.api.repository.CodeVerificationRepository;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.LienReinitialisationRepository;
import net.checkconsulting.assuranceiard.api.utils.Jetons;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mot de passe oublié (US-07) : RG1, réponse identique que le compte existe ou non ; RG2, lien de 24 heures à usage unique. */
@Service
@RequiredArgsConstructor
public class ReinitialisationService {

    private static final Duration VALIDITE_LIEN = Duration.ofHours(24);
    private static final String PAGE_REINITIALISATION = "/reinitialisation-mot-de-passe?jeton=";

    private final CompteRepository compteRepository;
    private final LienReinitialisationRepository lienRepository;
    private final CodeVerificationRepository codeRepository;
    private final SessionService sessionService;
    private final EnvoiLienReinitialisation envoiLien;
    private final PasswordEncoder passwordEncoder;
    private final ConnexionProperties proprietes;
    private final Clock horloge;

    /** Un nouveau lien annule le précédent : un seul reste valable. */
    @Transactional
    public void demander(String email) {
        compteRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT)).ifPresent(this::envoyerUnLien);
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

    private void envoyerUnLien(Compte compte) {
        lienRepository.deleteByCompte(compte);
        String jeton = Jetons.nouveauJeton();
        LienReinitialisation lien = new LienReinitialisation();
        lien.setCompte(compte);
        lien.setJetonHash(Jetons.empreinte(jeton));
        lien.setDateExpiration(horloge.instant().plus(VALIDITE_LIEN));
        lienRepository.save(lien);
        envoiLien.envoyer(compte.getEmail(), proprietes.urlFrontend() + PAGE_REINITIALISATION + jeton);
    }

    private LienReinitialisation trouverLienValable(String jeton) {
        return lienRepository.findByJetonHash(Jetons.empreinte(jeton))
                .filter(lien -> horloge.instant().isBefore(lien.getDateExpiration()))
                .orElseThrow(LienInvalideException::new);
    }
}
