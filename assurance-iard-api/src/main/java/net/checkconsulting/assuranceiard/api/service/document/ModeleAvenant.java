package net.checkconsulting.assuranceiard.api.service.document;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.checkconsulting.assuranceiard.api.dto.DonneesContrat;
import net.checkconsulting.assuranceiard.api.model.Avenant;
import net.checkconsulting.assuranceiard.api.model.AvenantLigne;
import net.checkconsulting.assuranceiard.api.model.Document;
import net.checkconsulting.assuranceiard.api.model.TypeDocument;
import net.checkconsulting.assuranceiard.api.repository.AvenantLigneRepository;
import net.checkconsulting.assuranceiard.api.repository.AvenantRepository;
import net.checkconsulting.assuranceiard.api.repository.DonneesContratRepository;
import net.checkconsulting.assuranceiard.api.service.avenant.CalculAvenant;
import net.checkconsulting.assuranceiard.api.service.avenant.NumeroAvenant;
import net.checkconsulting.assuranceiard.api.service.document.ContenuDocument.Section;
import org.springframework.stereotype.Component;

/**
 * US-23 : modèle « Avenant » (point d'extension de T-03 ; le service commun ne change pas). Un avenant par
 * document : document.reference = numéro AV-AAAA-NNNNNN (à défaut, l'avenant qui pointe vers ce document, données de
 * test T-05). Contenu obligatoire : numéro d'avenant, contrat, date d'effet, garanties ajoutées ou retirées, ancienne
 * et nouvelle cotisation, date et heure de l'acceptation. Un avenant pas encore accepté s'imprime en « Projet
 * d'avenant ». Les mentions de l'assureur, la charte et le pied de page sont ajoutés par le service commun.
 */
@Component
@RequiredArgsConstructor
public class ModeleAvenant implements ModeleDocument {

    static final String ACCEPTE = "ACCEPTE";
    private static final ZoneId FRANCE = ZoneId.of("Europe/Paris");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final AvenantRepository avenants;
    private final AvenantLigneRepository lignes;
    private final DonneesContratRepository contrats;

    @Override
    public TypeDocument type() {
        return TypeDocument.AVENANT;
    }

    @Override
    public ContenuDocument rediger(Document document) {
        Avenant avenant = NumeroAvenant.identifiant(document.getReference()).flatMap(avenants::findById)
                .or(() -> avenants.findFirstByDocumentId(document.getId()))
                .orElseThrow(() -> new IllegalStateException("Avenant introuvable pour le document " + document.getId()));
        DonneesContrat contrat = contrats.lire(avenant.getContratId())
                .orElseThrow(() -> new IllegalStateException("Contrat introuvable pour le document " + document.getId()));
        String numero = NumeroAvenant.de(avenant);
        List<AvenantLigne> sesLignes = lignes.findByAvenantIdOrderById(avenant.getId());
        boolean accepte = ACCEPTE.equals(avenant.getStatut());

        List<Section> sections = new ArrayList<>();
        sections.add(new Section("Avenant",
                "Avenant n° " + numero + " (avenant " + avenant.getNumero() + " du contrat)",
                "Date d'effet : " + MiseEnForme.date(avenant.getDateEffet())));
        sections.add(new Section("Contrat",
                "Contrat n° " + contrat.numero(),
                "Assurance " + ("AUTO".equals(contrat.produit()) ? "auto" : "habitation") + ", formule "
                        + MiseEnForme.formule(contrat.formule()),
                "Souscripteur : " + MiseEnForme.personne(contrat.souscripteur().civilite(),
                        contrat.souscripteur().prenom(), contrat.souscripteur().nom())));
        sections.add(new Section("Garanties ajoutées", garanties(sesLignes, CalculAvenant.AJOUT, "+")));
        sections.add(new Section("Garanties retirées", garanties(sesLignes, CalculAvenant.RETRAIT, "-")));
        sections.add(new Section("Cotisation",
                "Ancienne cotisation : " + cotisation(avenant.getPrimeAvantTtc()),
                "Nouvelle cotisation : " + cotisation(avenant.getPrimeApresTtc()),
                "La nouvelle cotisation s'applique à partir de la première échéance qui suit la date d'effet."));
        sections.add(new Section("Acceptation", accepte && avenant.getAccepteLe() != null
                ? "Avenant accepté en ligne le " + MiseEnForme.date(avenant.getAccepteLe().atZone(FRANCE).toLocalDate())
                        + " à " + HEURE.format(avenant.getAccepteLe().atZone(FRANCE)) + " (heure de Paris)"
                : "Projet d'avenant : pas encore accepté."));
        return new ContenuDocument(accepte ? "Avenant" : "Projet d'avenant", numero, sections);
    }

    private static List<String> garanties(List<AvenantLigne> lignes, String action, String signe) {
        List<String> texte = lignes.stream().filter(ligne -> action.equals(ligne.getAction()))
                .map(ligne -> "• " + MiseEnForme.option(ligne.getCodeOption()) + " : " + signe + " "
                        + MiseEnForme.montant(ligne.getPrixMensuelTtc()) + " TTC par mois")
                .toList();
        return texte.isEmpty() ? List.of("Aucune") : texte;
    }

    /** « 21,00 € TTC par mois (252,00 € TTC par an) ». */
    private static String cotisation(BigDecimal mensuelle) {
        return MiseEnForme.montant(mensuelle) + " TTC par mois (" + MiseEnForme.montant(CalculAvenant.annuelle(mensuelle))
                + " TTC par an)";
    }
}
