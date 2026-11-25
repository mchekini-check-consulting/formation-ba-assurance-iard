package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-05 : volet auto d'un devis enregistré (un pour un). */
@Entity
@Table(name = "devis_auto")
@Getter
@Setter
@NoArgsConstructor
public class DevisAuto extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "devis_id", unique = true)
    private Long devisId;

    @Column(name = "vehicule_id")
    private Long vehiculeId;

    @Column(name = "conducteur_id")
    private Long conducteurId;

    @Column(name = "assure_plus_un_an", nullable = false)
    private Boolean assurePlusUnAn = false;

    @Column(name = "ancien_assureur", length = 100)
    private String ancienAssureur;

    @Column(name = "ancien_numero_contrat", length = 40)
    private String ancienNumeroContrat;

    @Column(name = "assure_vehicule", length = 20)
    private String assureVehicule;

    @Column(name = "date_effet_souhaitee")
    private LocalDate dateEffetSouhaitee;

    // US-29 : besoins étape 2
    @Column(name = "valeur_vehicule")
    private Integer valeurVehicule;

    @Column(name = "vehicule_indispensable")
    private Boolean vehiculeIndispensable;

    @Column(name = "assistance_0km")
    private Boolean assistance0km;

    @Column(name = "protection_conducteur")
    private Boolean protectionConducteur;

    @Column(name = "bris_glace")
    private Boolean brisGlace;

    @Column(name = "accompagnement_litige")
    private Boolean accompagnementLitige;

    @Column(name = "priorite", length = 10)
    private String priorite;

    @Column(name = "formule_conseillee", length = 30)
    private String formuleConseillee;

    @Column(name = "raisons_formule", columnDefinition = "text")
    private String raisonsFormule;

    @Column(name = "prix_mensuel_estime", precision = 10, scale = 2)
    private BigDecimal prixMensuelEstime;

    // US-29 étape 3 : formule choisie, options, dérogation, prix finaux, validité
    @Column(name = "formule_choisie", length = 30)
    private String formuleChoisie;

    @Column(name = "options_choisies", columnDefinition = "text")
    private String optionsChoisies;

    @Column(name = "choix_different_du_conseil", nullable = false)
    private Boolean choixDifferentDuConseil = false;

    @Column(name = "date_confirmation_choix_different")
    private Instant dateConfirmationChoixDifferent;

    @Column(name = "prix_mensuel_ttc", precision = 10, scale = 2)
    private BigDecimal prixMensuelTtc;

    @Column(name = "prix_annuel_ttc", precision = 10, scale = 2)
    private BigDecimal prixAnnuelTtc;

    @Column(name = "version_grille", length = 50)
    private String versionGrille;

    @Column(name = "date_fin_validite")
    private LocalDate dateFinValidite;
}
