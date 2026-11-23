package net.checkconsulting.assuranceiard.api.service.document;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.config.DocumentsProperties;
import net.checkconsulting.assuranceiard.api.exception.DocumentAltereException;
import net.checkconsulting.assuranceiard.api.exception.DocumentIndisponibleException;
import net.checkconsulting.assuranceiard.api.exception.DocumentInconnuException;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.DocumentContenu;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.DocumentContenuRepository;
import net.checkconsulting.assuranceiard.api.repository.DocumentRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.service.DocumentService.Fichier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * T-03 : le service commun des documents PDF, appelé par les US (US-21, US-22, US-23, US-29, US-31, US-33).
 * <ul>
 *   <li>Générer : nouvelle version N+1 EN_VIGUEUR et version N REMPLACE, dans la même transaction.</li>
 *   <li>Stocker : PDF en base (document_contenu) avec son empreinte SHA-256, dans la même transaction que le
 *       document. Si le PDF ne peut pas être produit : stocke = faux, l'action du client continue, la tâche de
 *       reprise ({@link RepriseDocuments}) réessaie.</li>
 *   <li>Télécharger : l'empreinte est vérifiée avant l'envoi.</li>
 * </ul>
 * Journaux sans données personnelles : type, contrat (identifiant technique), version.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentsGeneresService {

    static final String EN_VIGUEUR = "EN_VIGUEUR";
    static final String REMPLACE = "REMPLACE";
    static final String PREFIXE_CLE = "base:document_contenu/";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");

    private final DocumentRepository documents;
    private final DocumentContenuRepository contenus;
    private final DonneesContratRepository contrats;
    private final CatalogueModeles catalogue;
    private final GabaritPdf gabarit;
    private final DocumentsProperties reglages;
    private final Clock horloge;

    /** Crée la version suivante du document (la version en vigueur est remplacée), puis la stocke. */
    @Transactional
    public Document generer(DemandeDocument demande) {
        String type = demande.type().name();
        int version = 1;
        Document actuelle = documents.enVigueurVerrouille(cle(demande.contratId()), type,
                demande.reference() == null ? "" : demande.reference()).orElse(null);
        if (actuelle != null) {
            actuelle.setStatut(REMPLACE);
            // Écrit avant la nouvelle version : une seule version EN_VIGUEUR à tout instant (index unique).
            documents.saveAndFlush(actuelle);
            version = actuelle.getVersion() + 1;
        }
        Document nouvelle = new Document();
        nouvelle.setType(type);
        nouvelle.setContratId(demande.contratId());
        nouvelle.setReference(demande.reference());
        nouvelle.setVersion(version);
        nouvelle.setStatut(EN_VIGUEUR);
        nouvelle.setMotif(demande.motif());
        documents.saveAndFlush(nouvelle);
        boolean stocke = stocker(nouvelle);
        log.info("Document généré : type={}, contrat={}, version={}, stocke={}", type, demande.contratId(), version,
                stocke);
        return nouvelle;
    }

    /**
     * Produit et range le PDF du document à sa version actuelle (jamais de nouvelle version), dans la transaction
     * en cours. Faux si le PDF n'a pas pu être produit : le document reste « non stocké » pour la reprise.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public boolean stocker(Document document) {
        byte[] pdf;
        try {
            pdf = produire(document);
        } catch (RuntimeException erreur) {
            log.warn("Document non stocké, repris plus tard : type={}, contrat={}, version={} ({})",
                    document.getType(), document.getContratId(), document.getVersion(),
                    erreur.getClass().getSimpleName());
            // Passe derrière les autres documents à reprendre.
            document.setDateModification(horloge.instant());
            return false;
        }
        DocumentContenu contenu = contenus.findByDocumentId(document.getId()).orElseGet(DocumentContenu::new);
        contenu.setDocumentId(document.getId());
        contenu.setContenu(pdf);
        contenu.setTailleOctets(pdf.length);
        contenus.save(contenu);
        document.setCleStockage(PREFIXE_CLE + document.getId());
        document.setEmpreinte(empreinte(pdf));
        document.setStocke(true);
        return true;
    }

    /** Le PDF du document, après vérification de son empreinte. */
    @Transactional(readOnly = true)
    public Fichier telecharger(long id) {
        Document document = documents.findById(id).orElseThrow(DocumentInconnuException::new);
        if (!Boolean.TRUE.equals(document.getStocke())) {
            throw new DocumentIndisponibleException();
        }
        byte[] pdf = contenus.findByDocumentId(id).map(DocumentContenu::getContenu).orElse(null);
        if (pdf == null || !empreinte(pdf).equals(document.getEmpreinte())) {
            log.error("Empreinte du document différente, envoi refusé : document={}, type={}, version={}", id,
                    document.getType(), document.getVersion());
            throw new DocumentAltereException();
        }
        return new Fichier(nomFichier(document), "pdf", pdf);
    }

    private byte[] produire(Document document) {
        ModeleDocument modele = catalogue.pour(document)
                .orElseThrow(() -> new IllegalStateException("Aucun modèle pour le type " + document.getType()));
        ContenuDocument contenu = modele.rediger(document).avec(sectionAssureur());
        GabaritPdf.Pied pied = new GabaritPdf.Pied(TypeDocument.valueOf(document.getType()).libelle(),
                contenu.numero(), document.getVersion(), aujourdHui());
        byte[] pdf = gabarit.rendre(contenu, pied);
        if (pdf.length > reglages.tailleMax().toBytes()) {
            throw new IllegalStateException("PDF trop volumineux : " + pdf.length + " octets");
        }
        return pdf;
    }

    private ContenuDocument.Section sectionAssureur() {
        DocumentsProperties.Assureur assureur = reglages.assureur();
        return new ContenuDocument.Section("Votre assureur", assureur.nom(), assureur.adresse(),
                assureur.immatriculation(), assureur.controle());
    }

    private String nomFichier(Document document) {
        String repere = document.getContratId() == null ? document.getReference()
                : contrats.numero(document.getContratId()).orElse("contrat");
        return document.getType().toLowerCase(Locale.ROOT).replace('_', '-') + "-" + repere + "-v"
                + document.getVersion() + ".pdf";
    }

    private LocalDate aujourdHui() {
        return LocalDate.ofInstant(Instant.now(horloge), FRANCE);
    }

    private static long cle(Long contratId) {
        return contratId == null ? 0 : contratId;
    }

    static String empreinte(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException absent) {
            throw new IllegalStateException(absent);
        }
    }
}
