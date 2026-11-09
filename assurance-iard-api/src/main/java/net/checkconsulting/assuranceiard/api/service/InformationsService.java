package net.checkconsulting.assuranceiard.api.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.IdentiteResponse;
import net.checkconsulting.assuranceiard.api.dto.InformationsRequest;
import net.checkconsulting.assuranceiard.api.exception.DevisInconnuException;
import net.checkconsulting.assuranceiard.api.exception.InformationRefuseeException;
import net.checkconsulting.assuranceiard.api.mapper.CompteMapper;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import net.checkconsulting.assuranceiard.api.model.Justificatif;
import net.checkconsulting.assuranceiard.api.repository.DossierSouscriptionRepository;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import net.checkconsulting.assuranceiard.api.repository.RecommandationDevisRepository;
import net.checkconsulting.assuranceiard.api.utils.TypesFichier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** US-13 : identité du client, colocataires et justificatifs. Aucune vérification réelle des documents. */
@Service
@RequiredArgsConstructor
public class InformationsService {

    public static final String CHAMP_PIECE_IDENTITE = "pieceIdentite";
    public static final String CHAMP_JUSTIFICATIF_DOMICILE = "justificatifDomicile";

    private static final int AGE_MAJORITE = 18;
    private static final int LONGUEUR_NOM_MAX = 255;
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");
    private static final String ATTENDU = "Déposez un fichier PDF, JPG ou PNG de 5 Mo maximum.";

    private final SessionService sessionService;
    private final CompteMapper compteMapper;
    private final RecommandationDevisRepository recommandationRepository;
    private final DossierSouscriptionRepository dossierRepository;
    private final JustificatifRepository justificatifRepository;
    private final StockageJustificatifs stockage;
    private final Clock horloge;

    private record FichierControle(String nom, String type, byte[] contenu) {
    }

    @Transactional
    public IdentiteResponse lire(String authorization) {
        return compteMapper.versIdentite(sessionService.compteConnecte(authorization));
    }

    @Transactional
    public void enregistrer(
            String authorization, InformationsRequest demande, MultipartFile pieceIdentite, MultipartFile domicile) {
        Compte compte = sessionService.compteConnecte(authorization);
        verifierMajeur(demande.dateNaissance());
        FichierControle fichierIdentite = controler(CHAMP_PIECE_IDENTITE, pieceIdentite);
        FichierControle fichierDomicile = controler(CHAMP_JUSTIFICATIF_DOMICILE, domicile);
        recommandationRepository.findByReference(demande.referenceDevis()).orElseThrow(DevisInconnuException::new);

        mettreAJourIdentite(compte, demande);
        DossierSouscription dossier = enregistrerDossier(compte, demande);
        List<String> anciennesCles = justificatifRepository.findByDossier(dossier).stream()
                .map(Justificatif::getCleStockage)
                .toList();
        List<String> nouvellesCles = new ArrayList<>();
        try {
            nouvellesCles.add(enregistrerFichier(dossier, "identite", demande.typePieceIdentite(), fichierIdentite));
            nouvellesCles.add(enregistrerFichier(
                    dossier, "domicile", demande.typeJustificatifDomicile(), fichierDomicile));
        } catch (UncheckedIOException erreur) {
            nouvellesCles.forEach(stockage::supprimer);
            throw erreur;
        }
        anciennesCles.forEach(stockage::supprimer);
    }

    // RG1 : majeur le jour même de ses 18 ans
    private void verifierMajeur(LocalDate dateNaissance) {
        LocalDate aujourdhui = LocalDate.ofInstant(horloge.instant(), FRANCE);
        if (Period.between(dateNaissance, aujourdhui).getYears() < AGE_MAJORITE) {
            throw new InformationRefuseeException("dateNaissance", "Vous devez avoir au moins 18 ans.");
        }
    }

    // RG4
    private FichierControle controler(String champ, MultipartFile fichier) {
        if (fichier.getSize() > TypesFichier.TAILLE_MAX) {
            throw new InformationRefuseeException(champ, "Ce fichier dépasse 5 Mo. " + ATTENDU);
        }
        byte[] contenu = lireContenu(fichier);
        String nom = fichier.getOriginalFilename();
        String type = TypesFichier.typeReel(contenu)
                .filter(trouve -> TypesFichier.extensionAcceptee(nom))
                .orElseThrow(() -> new InformationRefuseeException(champ, "Ce format n'est pas accepté. " + ATTENDU));
        return new FichierControle(nom, type, contenu);
    }

    private byte[] lireContenu(MultipartFile fichier) {
        try {
            return fichier.getBytes();
        } catch (IOException erreur) {
            throw new UncheckedIOException(erreur);
        }
    }

    private void mettreAJourIdentite(Compte compte, InformationsRequest demande) {
        compte.setCivilite(demande.civilite());
        compte.setNom(demande.nom().trim());
        compte.setPrenom(demande.prenom().trim());
        compte.setDateNaissance(demande.dateNaissance());
        compte.setTelephone(demande.telephone());
    }

    private DossierSouscription enregistrerDossier(Compte compte, InformationsRequest demande) {
        DossierSouscription dossier = dossierRepository
                .findByCompteAndReferenceDevis(compte, demande.referenceDevis())
                .orElseGet(DossierSouscription::new);
        dossier.setCompte(compte);
        dossier.setReferenceDevis(demande.referenceDevis());
        dossier.setColocataires(demande.colocataires().stream().map(String::trim).toList());
        dossier.setDateCreation(horloge.instant());
        return dossierRepository.save(dossier);
    }

    // Un nouveau dépôt remplace le précédent dans le même dossier
    private String enregistrerFichier(
            DossierSouscription dossier, String categorie, String typeDocument, FichierControle fichier) {
        Justificatif justificatif = justificatifRepository.findByDossierAndCategorie(dossier, categorie)
                .orElseGet(Justificatif::new);
        String cle = stockage.stocker(fichier.contenu(), fichier.type());
        justificatif.setDossier(dossier);
        justificatif.setCategorie(categorie);
        justificatif.setTypeDocument(typeDocument);
        justificatif.setNomFichier(fichier.nom().substring(0, Math.min(fichier.nom().length(), LONGUEUR_NOM_MAX)));
        justificatif.setTypeFichier(fichier.type());
        justificatif.setTaille(fichier.contenu().length);
        justificatif.setCleStockage(cle);
        justificatifRepository.save(justificatif);
        return cle;
    }
}
