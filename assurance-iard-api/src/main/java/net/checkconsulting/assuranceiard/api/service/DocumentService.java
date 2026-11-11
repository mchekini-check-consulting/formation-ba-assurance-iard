package net.checkconsulting.assuranceiard.api.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DocumentResponse;
import net.checkconsulting.assuranceiard.api.exception.DocumentInconnuException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Justificatif;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-04 : « Mes documents ». Le propriétaire d'un document est vérifié par le contrôle d'accès central
 * (ressource DOCUMENT) avant l'ouverture.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final SessionService sessionService;
    private final JustificatifRepository justificatifRepository;
    private final StockageJustificatifs stockage;

    /** Le contenu d'un document à renvoyer au navigateur. */
    public record Fichier(String nom, String type, byte[] contenu) {
    }

    @Transactional
    public List<DocumentResponse> lister(String authorization) {
        Compte compte = sessionService.compteConnecte(authorization);
        return justificatifRepository.findByDossierCompteOrderByIdDesc(compte).stream()
                .map(DocumentService::versReponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Fichier ouvrir(Long id) {
        Justificatif justificatif = justificatifRepository.findById(id).orElseThrow(DocumentInconnuException::new);
        byte[] contenu = stockage.lire(justificatif.getCleStockage()).orElseThrow(DocumentInconnuException::new);
        return new Fichier(justificatif.getNomFichier(), justificatif.getTypeFichier(), contenu);
    }

    static DocumentResponse versReponse(Justificatif justificatif) {
        return new DocumentResponse(justificatif.getId(), justificatif.getDossier().getReferenceDevis(),
                justificatif.getCategorie(), justificatif.getTypeDocument(), justificatif.getNomFichier(),
                justificatif.getTypeFichier(), justificatif.getTaille());
    }
}
