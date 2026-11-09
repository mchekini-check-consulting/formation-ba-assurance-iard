package net.checkconsulting.moteurtarifaire.api;

import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.checkconsulting.moteurtarifaire.api.dto.BesoinsRequest;
import net.checkconsulting.moteurtarifaire.api.dto.TarifHabitationRequest;
import net.checkconsulting.moteurtarifaire.api.mapper.GrilleTarifaireAssembleur;
import net.checkconsulting.moteurtarifaire.api.dto.Besoins;
import net.checkconsulting.moteurtarifaire.api.model.ParametreTarifaire;
import net.checkconsulting.moteurtarifaire.api.dto.ProfilRisque;
import net.checkconsulting.moteurtarifaire.api.model.VersionGrille;
import net.checkconsulting.moteurtarifaire.api.dto.GrilleTarifaire;
import net.checkconsulting.moteurtarifaire.api.enums.Etage;
import net.checkconsulting.moteurtarifaire.api.enums.Priorite;
import net.checkconsulting.moteurtarifaire.api.enums.Remboursement;
import net.checkconsulting.moteurtarifaire.api.enums.Residence;
import net.checkconsulting.moteurtarifaire.api.enums.ResiliationAnterieure;
import net.checkconsulting.moteurtarifaire.api.enums.StatutOccupant;
import net.checkconsulting.moteurtarifaire.api.enums.TypeLogement;
import net.checkconsulting.moteurtarifaire.api.repository.ParametreTarifaireRepository;
import net.checkconsulting.moteurtarifaire.api.service.ValidateurGrille;
import net.checkconsulting.moteurtarifaire.api.repository.VersionGrilleRepository;
import net.checkconsulting.moteurtarifaire.api.service.critere.CritereDependances;
import net.checkconsulting.moteurtarifaire.api.service.critere.CritereEtage;
import net.checkconsulting.moteurtarifaire.api.service.critere.CritereResidence;
import net.checkconsulting.moteurtarifaire.api.service.critere.CritereSinistres;
import net.checkconsulting.moteurtarifaire.api.service.critere.CritereStatut;
import net.checkconsulting.moteurtarifaire.api.service.critere.CritereTarifaire;
import net.checkconsulting.moteurtarifaire.api.service.critere.CritereTypeLogement;
import net.checkconsulting.moteurtarifaire.api.service.critere.CritereZone;

public final class JeuxDeDonnees {

    public static final GrilleTarifaire GRILLE = new GrilleTarifaireAssembleur().assembler(versionDeReference(), parametresDeReference());

    public static final String CODE_POSTAL_SANS_MAJORATION = "01000";

    private JeuxDeDonnees() {
    }

    public static void installerGrilleDeReference(VersionGrilleRepository versions, ParametreTarifaireRepository parametres) {
        parametres.deleteAllInBatch();
        versions.deleteAllInBatch();
        Long idGrille = versions.save(versionDeReference()).getId();
        List<ParametreTarifaire> lignes = parametresDeReference();
        lignes.forEach(ligne -> ligne.setGrilleId(idGrille));
        parametres.saveAll(lignes);
    }

    public static ValidateurGrille validateurDeGrille() {
        try (var usine = Validation.buildDefaultValidatorFactory()) {
            return new ValidateurGrille(usine.getValidator());
        }
    }

    public static VersionGrille versionDeReference() {
        VersionGrille version = new VersionGrille();
        version.setVersion("TEST-REFERENCE");
        version.setDateDebut(LocalDate.of(2000, 1, 1));
        return version;
    }

    public static ParametreTarifaire parametre(String categorie, String cle, String valeur) {
        ParametreTarifaire parametre = new ParametreTarifaire();
        parametre.setCategorie(categorie);
        parametre.setCle(cle);
        parametre.setValeur(valeur);
        return parametre;
    }

    public static List<ParametreTarifaire> parametresDeReference() {
        String[][] lignes = {
            {"general", "duree-validite-jours", "30"},
            {"arrondi", "decimales", "2"},
            {"arrondi", "mode", "HALF_UP"},
            {"prix-base", "ESSENTIELLE", "7.90"},
            {"prix-base", "CONFORT", "10.90"},
            {"prix-base", "SERENITE", "15.90"},
            {"coefficient-depart", "surface-reference", "45"},
            {"coefficient-depart", "surface-plafond", "400"},
            {"coefficient-depart", "pente-surface", "0.35"},
            {"coefficient-depart", "coefficient-surface-minimum", "0.80"},
            {"coefficient-depart", "pieces-reference", "2"},
            {"coefficient-depart", "increment-par-piece", "0.06"},
            {"statut", "LOCATAIRE", "1"},
            {"statut", "PROPRIETAIRE_OCCUPANT", "1.6"},
            {"type-logement", "APPARTEMENT", "1"},
            {"type-logement", "MAISON", "1.35"},
            {"residence", "PRINCIPALE", "1"},
            {"residence", "SECONDAIRE", "1.25"},
            {"sinistres", "0", "1"},
            {"sinistres", "1", "1.15"},
            {"sinistres", "2", "1.3"},
            {"sinistres", "3", "1.5"},
            {"zone", "75", "1.2"},
            {"zone", "92", "1.2"},
            {"zone", "93", "1.2"},
            {"zone", "94", "1.2"},
            {"zone", "13", "1.1"},
            {"zone", "69", "1.1"},
            {"zone", "06", "1.1"},
            {"zone", "33", "1.1"},
            {"zone", "31", "1.1"},
            {"zone", "59", "1.1"},
            {"etage", "RDC", "1.05"},
            {"etage", "INTERMEDIAIRE", "1"},
            {"etage", "DERNIER", "1"},
            {"dependances", "AUCUNE", "1"},
            {"dependances", "JARDIN", "1.05"},
            {"dependances", "GARAGE", "1.05"},
            {"dependances", "PISCINE", "1.15"},
            {"dependances", "PLUSIEURS", "1.2"},
            {"prix-option", "OBJETS_VALEUR", "2.50"},
            {"prix-option", "ASSISTANCE", "1.50"},
            {"prix-option", "PROTECTION_JURIDIQUE", "2.90"},
            {"franchise", "150", "1.00"},
            {"franchise", "300", "0"},
            {"franchise", "500", "-1.00"},
            {"refus-statut", "PROPRIETAIRE_NON_OCCUPANT", "Motif de test : propriétaire non occupant."},
            {"refus-resiliation", "NON_PAIEMENT", "Motif de test : résiliation pour non-paiement."},
            {"recommandation", "seuil-contenu-serenite", "20000"},
            {"recommandation", "seuil-contenu-essentielle", "10000"},
            {"raison", "CONTENU_ELEVE", "Raison de test : contenu élevé"},
            {"raison", "REMBOURSEMENT_NEUF", "Raison de test : remboursement à neuf"},
            {"raison", "APPAREILS_COUTEUX", "Raison de test : appareils coûteux"},
            {"raison", "PRIORITE_PROTECTION", "Raison de test : priorité protection"},
            {"raison", "CONTENU_MOYEN", "Raison de test : contenu moyen"},
            {"raison", "OBJETS_VALEUR", "Raison de test : objets de valeur"},
            {"raison", "PRIORITE_EQUILIBRE", "Raison de test : priorité équilibre"},
            {"raison", "CONTENU_FAIBLE", "Raison de test : contenu faible"},
            {"raison", "AUCUN_OBJET_VALEUR", "Raison de test : aucun objet de valeur"},
            {"raison", "PRIORITE_PRIX", "Raison de test : priorité prix"}
        };
        List<ParametreTarifaire> parametres = new ArrayList<>();
        for (String[] ligne : lignes) {
            parametres.add(parametre(ligne[0], ligne[1], ligne[2]));
        }
        return parametres;
    }

    public static List<CritereTarifaire> criteres() {
        return List.of(
                new CritereStatut(),
                new CritereTypeLogement(),
                new CritereResidence(),
                new CritereSinistres(),
                new CritereZone(),
                new CritereEtage(),
                new CritereDependances());
    }

    public static ProfilRisque profilCa1() {
        return ProfilRisque.builder()
                .statut(StatutOccupant.LOCATAIRE)
                .residence(Residence.PRINCIPALE)
                .typeLogement(TypeLogement.APPARTEMENT)
                .codePostal("69003")
                .surface(BigDecimal.valueOf(45))
                .pieces(2)
                .etage(Etage.INTERMEDIAIRE)
                .sinistres3ans(0)
                .resiliationAnterieure(ResiliationAnterieure.NON)
                .build();
    }

    public static ProfilRisque profilNeutre() {
        return profilCa1().toBuilder().codePostal(CODE_POSTAL_SANS_MAJORATION).build();
    }

    public static TarifHabitationRequest demandeCa1() {
        return TarifHabitationRequest.builder()
                .statut(StatutOccupant.LOCATAIRE)
                .residence(Residence.PRINCIPALE)
                .typeLogement(TypeLogement.APPARTEMENT)
                .codePostal("69003")
                .surface(BigDecimal.valueOf(45))
                .pieces(2)
                .etage(Etage.INTERMEDIAIRE)
                .sinistres3ans(0)
                .resiliationAnterieure(ResiliationAnterieure.NON)
                .build();
    }

    public static Map<String, Object> corpsCa1() {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("statut", "LOCATAIRE");
        corps.put("residence", "PRINCIPALE");
        corps.put("typeLogement", "APPARTEMENT");
        corps.put("codePostal", "69003");
        corps.put("surface", 45);
        corps.put("pieces", 2);
        corps.put("etage", "INTERMEDIAIRE");
        corps.put("sinistres3ans", 0);
        corps.put("resiliationAnterieure", "NON");
        return corps;
    }

    public static Map<String, Object> corpsBesoins() {
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("valeurContenu", 15_000);
        corps.put("objetsValeur", false);
        corps.put("appareilsCouteux", false);
        corps.put("remboursement", "VETUSTE");
        corps.put("assistance", false);
        corps.put("litiges", false);
        corps.put("priorite", "EQUILIBRE");
        return corps;
    }

    public static Besoins besoinsConfort() {
        return Besoins.builder()
                .valeurContenu(GRILLE.recommandation().seuilContenuEssentielle())
                .objetsValeur(false)
                .appareilsCouteux(false)
                .remboursement(Remboursement.VETUSTE)
                .assistance(false)
                .litiges(false)
                .priorite(Priorite.EQUILIBRE)
                .build();
    }

    public static BesoinsRequest besoinsRequestConfort() {
        return BesoinsRequest.builder()
                .valeurContenu(GRILLE.recommandation().seuilContenuEssentielle())
                .objetsValeur(false)
                .appareilsCouteux(false)
                .remboursement(Remboursement.VETUSTE)
                .assistance(false)
                .litiges(false)
                .priorite(Priorite.EQUILIBRE)
                .build();
    }

}
