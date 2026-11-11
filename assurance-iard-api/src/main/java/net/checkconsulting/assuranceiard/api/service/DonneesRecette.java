package net.checkconsulting.assuranceiard.api.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.config.DonneesTestProperties;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DevisEnregistre;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.Justificatif;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.model.RecommandationDevis;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import net.checkconsulting.assuranceiard.api.repository.MandatSepaRepository;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-04, RG5 : comptes de recette créés au démarrage si DONNEES_TEST_ACTIVES=true (local, dev, qualif ; jamais en
 * preprod ni en prod). Rien n'est recréé si le compte existe déjà ; un document effacé (dossier temporaire vidé au
 * redémarrage) est simplement redéposé.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "assurance.donnees-test.actif", havingValue = "true")
@RequiredArgsConstructor
public class DonneesRecette implements ApplicationRunner {

    static final String ASSURE_1 = "recette.assure1@assureasy.fr";
    static final String ASSURE_2 = "recette.assure2@assureasy.fr";
    static final String GESTIONNAIRE = "recette.gestionnaire@assureasy.fr";
    static final String ADMINISTRATEUR = "recette.admin@assureasy.fr";

    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");
    private static final byte[] PDF = "%PDF-1.4\n% Document de recette Assur'Easy\n%%EOF\n"
            .getBytes(StandardCharsets.US_ASCII);

    private final DonneesTestProperties reglages;
    private final PasswordEncoder passwordEncoder;
    private final Clock horloge;
    private final CompteRepository comptes;
    private final RecommandationDevisRepository recommandations;
    private final DevisEnregistreRepository devis;
    private final DossierSouscriptionRepository dossiers;
    private final JustificatifRepository justificatifs;
    private final MandatSepaRepository mandats;
    private final ContratRepository contrats;
    private final StockageJustificatifs stockage;

    /** Les données propres à chaque assuré de recette. */
    private record Assure(String email, String prenom, String nom, String ville, String codePostal, String ibanFin,
            int rang) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        creerAssure(new Assure(ASSURE_1, "Alice", "Martin", "Paris", "75011", "0189", 1));
        creerAssure(new Assure(ASSURE_2, "Bruno", "Petit", "Lyon", "69003", "4321", 2));
        creerInterne(GESTIONNAIRE, "Gina", "Gestion", Profil.GESTIONNAIRE);
        creerInterne(ADMINISTRATEUR, "Adam", "Admin", Profil.ADMINISTRATEUR);
        redeposerLesDocumentsEffaces();
        log.info("Données de recette prêtes");
    }

    private void creerInterne(String email, String prenom, String nom, Profil profil) {
        if (!comptes.existsByEmail(email)) {
            Compte compte = nouveauCompte(email, prenom, nom);
            compte.setProfil(profil);
            comptes.save(compte);
        }
    }

    private void creerAssure(Assure assure) {
        if (comptes.existsByEmail(assure.email())) {
            return;
        }
        Compte compte = nouveauCompte(assure.email(), assure.prenom(), assure.nom());
        compte.setProfil(Profil.ASSURE);
        compte.setCivilite("madame");
        compte.setDateNaissance(LocalDate.of(1990, 1, assure.rang()));
        compte.setTelephone("061234567" + assure.rang());
        compte = comptes.save(compte);

        RecommandationDevis recommandation = recommandations.save(recommandation());
        devis.save(devisEnregistre(compte, recommandation, assure));
        DossierSouscription dossier = dossiers.save(dossier(compte, recommandation.getReference()));
        justificatifs.save(justificatif(dossier));
        MandatSepa mandat = mandats.save(mandat(dossier, assure));
        contrats.save(contrat(dossier, mandat, assure));
    }

    private Compte nouveauCompte(String email, String prenom, String nom) {
        Compte compte = new Compte();
        compte.setEmail(email);
        compte.setMotDePasseHash(passwordEncoder.encode(reglages.motDePasse()));
        compte.setPrenom(prenom);
        compte.setNom(nom);
        return compte;
    }

    private RecommandationDevis recommandation() {
        RecommandationDevis recommandation = new RecommandationDevis();
        recommandation.setReference(UUID.randomUUID());
        recommandation.setValeurContenu(20_000);
        recommandation.setRemboursement("neuf");
        recommandation.setPriorite("prix");
        recommandation.setFormule("confort");
        recommandation.setRaisons(List.of("Données de recette"));
        recommandation.setOptions(List.of());
        recommandation.setDateCreation(horloge.instant());
        return recommandation;
    }

    private DevisEnregistre devisEnregistre(Compte compte, RecommandationDevis recommandation, Assure assure) {
        Instant maintenant = horloge.instant();
        DevisEnregistre enregistre = new DevisEnregistre();
        enregistre.setCompte(compte);
        enregistre.setReference(recommandation.getReference());
        enregistre.setTypeLogement("appartement");
        enregistre.setAdresse(assure.rang() + " rue de la Recette");
        enregistre.setCodePostal(assure.codePostal());
        enregistre.setVille(assure.ville());
        enregistre.setSurface(40 + assure.rang() * 10);
        enregistre.setFormule("confort");
        enregistre.setOptions(List.of());
        enregistre.setFranchise("300");
        enregistre.setPrixMensuel(new BigDecimal("18.50"));
        enregistre.setDateCreation(maintenant);
        enregistre.setDateFinValidite(LocalDate.ofInstant(maintenant, FRANCE).plusDays(30));
        enregistre.setDateDernierContact(maintenant);
        return enregistre;
    }

    private DossierSouscription dossier(Compte compte, UUID reference) {
        DossierSouscription dossier = new DossierSouscription();
        dossier.setCompte(compte);
        dossier.setReferenceDevis(reference);
        dossier.setColocataires(List.of());
        dossier.setDateCreation(horloge.instant());
        return dossier;
    }

    private Justificatif justificatif(DossierSouscription dossier) {
        Justificatif justificatif = new Justificatif();
        justificatif.setDossier(dossier);
        justificatif.setCategorie("identite");
        justificatif.setTypeDocument("carte-identite");
        justificatif.setNomFichier("piece-identite-recette.pdf");
        justificatif.setTypeFichier("pdf");
        justificatif.setTaille(PDF.length);
        justificatif.setCleStockage(stockage.stocker(PDF, "pdf"));
        return justificatif;
    }

    private MandatSepa mandat(DossierSouscription dossier, Assure assure) {
        LocalDate debut = LocalDate.ofInstant(horloge.instant(), FRANCE).plusDays(1);
        MandatSepa mandat = new MandatSepa();
        mandat.setDossier(dossier);
        mandat.setRum("AE-RUM-RECETTE-" + assure.rang());
        mandat.setTitulaire(assure.prenom() + " " + assure.nom());
        mandat.setIbanDebut("FR76");
        mandat.setIbanFin(assure.ibanFin());
        mandat.setJourPrelevement(10);
        mandat.setDateDebut(debut);
        mandat.setDatePremierPrelevement(debut.withDayOfMonth(10).plusMonths(1));
        mandat.setDateCreation(horloge.instant());
        mandat.setDateSignature(horloge.instant());
        return mandat;
    }

    private Contrat contrat(DossierSouscription dossier, MandatSepa mandat, Assure assure) {
        Contrat contrat = new Contrat();
        contrat.setDossier(dossier);
        contrat.setNumero("AE-RECETTE-" + assure.rang());
        contrat.setStatut("ACTIF");
        contrat.setAdresseLogement(assure.rang() + " rue de la Recette");
        contrat.setCodePostal(assure.codePostal());
        contrat.setVille(assure.ville());
        contrat.setTypeLogement("appartement");
        contrat.setSurface(40 + assure.rang() * 10);
        contrat.setFranchise(300);
        contrat.setCotisationMensuelle(new BigDecimal("18.50"));
        contrat.setCotisationAnnuelle(new BigDecimal("222.00"));
        contrat.setResiliationHamon(false);
        contrat.setDateEffet(mandat.getDateDebut());
        contrat.setDateCreation(horloge.instant());
        return contrat;
    }

    private void redeposerLesDocumentsEffaces() {
        for (String email : List.of(ASSURE_1, ASSURE_2)) {
            comptes.findByEmail(email).ifPresent(compte ->
                    justificatifs.findByDossierCompteOrderByIdDesc(compte).stream()
                            .filter(justificatif -> stockage.lire(justificatif.getCleStockage()).isEmpty())
                            .forEach(justificatif -> justificatif.setCleStockage(stockage.stocker(PDF, "pdf"))));
        }
    }
}
