package net.checkconsulting.assuranceiard.api.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.ClientResumeResponse;
import net.checkconsulting.assuranceiard.api.dto.ContratClientResponse;
import net.checkconsulting.assuranceiard.api.dto.DossierClientResponse;
import net.checkconsulting.assuranceiard.api.exception.ClientInconnuException;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.MandatSepa;
import net.checkconsulting.assuranceiard.api.model.Profil;
import net.checkconsulting.assuranceiard.api.repository.CompteRepository;
import net.checkconsulting.assuranceiard.api.repository.ContratRepository;
import net.checkconsulting.assuranceiard.api.repository.JustificatifRepository;
import net.checkconsulting.assuranceiard.api.repository.MandatSepaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-04 : dossiers clients consultés par le gestionnaire (lecture seule). */
@Service
@RequiredArgsConstructor
public class BackOfficeService {

    private static final EnumSet<Profil> CLIENTS = EnumSet.of(Profil.PROSPECT, Profil.ASSURE);

    private final CompteRepository compteRepository;
    private final ContratRepository contratRepository;
    private final MandatSepaRepository mandatRepository;
    private final JustificatifRepository justificatifRepository;

    @Transactional(readOnly = true)
    public List<ClientResumeResponse> listerClients() {
        return compteRepository.findByProfilInOrderByNomAscPrenomAscEmailAsc(CLIENTS).stream()
                .map(c -> new ClientResumeResponse(c.getId(), c.getPrenom(), c.getNom(), c.getEmail(),
                        c.getProfil().name()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DossierClientResponse dossier(Long id) {
        Compte client = compteRepository.findById(id)
                .filter(compte -> CLIENTS.contains(compte.getProfil()))
                .orElseThrow(ClientInconnuException::new);
        List<ContratClientResponse> contrats = contratRepository.findByDossierCompteOrderByDateCreationDesc(client)
                .stream()
                .map(this::versContrat)
                .toList();
        return new DossierClientResponse(client.getId(), client.getCivilite(), client.getPrenom(), client.getNom(),
                client.getEmail(), client.getTelephone(), client.getProfil().name(), contrats,
                justificatifRepository.findByDossierCompteOrderByIdDesc(client).stream()
                        .map(DocumentService::versReponse)
                        .toList());
    }

    private ContratClientResponse versContrat(Contrat contrat) {
        Optional<MandatSepa> mandat = mandatRepository.findByDossier(contrat.getDossier());
        return new ContratClientResponse(contrat.getDossier().getReferenceDevis(), contrat.getNumero(),
                contrat.getStatut(), contrat.getAdresseLogement(), contrat.getCodePostal(), contrat.getVille(),
                contrat.getCotisationMensuelle(), contrat.getDateEffet(),
                mandat.map(MandatSepa::getTitulaire).orElse(null),
                mandat.map(MandatSepa::getIbanFin).orElse(null),
                mandat.map(MandatSepa::getJourPrelevement).orElse(null),
                mandat.map(MandatSepa::getRum).orElse(null));
    }
}
