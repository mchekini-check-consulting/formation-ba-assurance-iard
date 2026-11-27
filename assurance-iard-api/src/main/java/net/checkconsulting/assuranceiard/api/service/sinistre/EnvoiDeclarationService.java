package net.checkconsulting.assuranceiard.api.service.sinistre;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.checkconsulting.assuranceiard.api.dto.EnvoiSinistreRequest;
import net.checkconsulting.assuranceiard.api.dto.SinistreEnvoyeResponse;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.Sinistre;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository;
import net.checkconsulting.assuranceiard.api.repository.ReferentielSinistresRepository.TypeSinistre;
import net.checkconsulting.assuranceiard.api.repository.SinistreRepository;
import net.checkconsulting.assuranceiard.api.service.EnvoiSinistre;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-24, AC9 : envoi de la déclaration. Réponses obligatoires et déclaration sur l'honneur vérifiées (RG8), numéro
 * SIN-AAAA-XXXXXX (RG5), statut Déclaré, signalements pour le gestionnaire (hors période AC5, hors délai AC6, pièces
 * manquantes AC7, à requalifier RG11, corbeille RG9). Une pièce obligatoire manquante n'empêche pas l'envoi (validé
 * PO le 27/11/2026). Refusé si le contrat n'est plus ouvert à la déclaration (Impayé, Suspendu...), avec le numéro
 * d'assistance 24h/24.
 * Accusé de réception par e-mail, et information du souscripteur si un proche ou le conducteur secondaire a déclaré
 * (AC13) ; un e-mail qui ne part pas n'annule pas la déclaration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnvoiDeclarationService {

    private final DeclarationSinistreService declarations;
    private final SinistreRepository sinistres;
    private final ReferentielSinistresRepository referentiel;
    private final VueDeclaration vue;
    private final JsonSinistre json;
    private final EnvoiSinistre emails;

    @LogMetier(champs = {"resultat.piecesManquantes"})
    @Transactional
    public SinistreEnvoyeResponse envoyer(Long id, EnvoiSinistreRequest demande) {
        Sinistre sinistre = declarations.brouillon(id);
        Contrat contrat = declarations.contratDe(sinistre);
        declarations.verifierEnvoyable(contrat);
        TypeSinistre type = declarations.typeDuProduit(contrat, sinistre.getType());
        boolean horsDelai = vue.horsDelai(sinistre, type);
        JsonSinistre.Declarant declarant = json.declarant(sinistre.getDeclarantInfos());
        ValidationDeclaration.verifierComplete(sinistre, declarant == null ? null : declarant.nom(),
                declarant == null ? null : declarant.lien(), horsDelai, demande == null ? null
                        : demande.declarationHonneur());

        Instant maintenant = vue.instant();
        sinistre.setStatut(DeclarationSinistreService.DECLARE);
        sinistre.setNumero(NumeroSinistre.de(maintenant, sinistre.getId()));
        sinistre.setDeclareLe(maintenant);
        sinistre.setDeclarationHonneur(true);
        sinistre.setHorsDelai(horsDelai);
        sinistre.setHorsPeriodeContrat(ReglesDeclaration.horsPeriode(contrat, sinistre.getDateSurvenance()));
        sinistre.setRequalifier(type.autre());
        sinistre.setCorbeille(contrat.getProduit());
        boolean manquantes = vue.piecesManquantes(sinistre, referentiel.pieces(contrat.getProduit(), type.code()));
        sinistre.setPiecesManquantes(manquantes);
        sinistre.setEtape((short) ValidationDeclaration.DERNIERE_ETAPE);
        sinistres.save(sinistre);
        log.info("Sinistre déclaré : sinistre={}, corbeille={}, aRequalifier={}, horsDelai={}, horsPeriode={}, "
                + "piecesManquantes={}", sinistre.getId(), sinistre.getCorbeille(), type.autre(), horsDelai,
                sinistre.getHorsPeriodeContrat(), manquantes);

        boolean emailEnvoye = prevenir(dossier(sinistre, contrat, type, declarant, manquantes), declarant != null);
        return new SinistreEnvoyeResponse(sinistre.getId(), sinistre.getNumero(), sinistre.getStatut(), maintenant,
                manquantes, emailEnvoye);
    }

    private boolean prevenir(EnvoiSinistre.Dossier dossier, boolean pourUnAutre) {
        try {
            emails.accuserReception(dossier);
            if (pourUnAutre) {
                emails.informerSouscripteur(dossier);
            }
            return true;
        } catch (RuntimeException erreur) {
            log.error("Accusé de réception du sinistre non envoyé : {}", erreur.getClass().getSimpleName());
            return false;
        }
    }

    private static EnvoiSinistre.Dossier dossier(Sinistre sinistre, Contrat contrat, TypeSinistre type,
            JsonSinistre.Declarant declarant, boolean manquantes) {
        Compte compte = contrat.getCompte();
        return new EnvoiSinistre.Dossier(compte.getEmail(), compte.getPrenom(), sinistre.getNumero(),
                contrat.getNumero(), type.libelle(), sinistre.getDateSurvenance(), manquantes,
                declarant == null ? null : declarant.nom(), declarant == null ? null : declarant.lien());
    }
}
