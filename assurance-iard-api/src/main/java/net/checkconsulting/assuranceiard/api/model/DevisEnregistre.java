package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * US-12 : devis enregistré par un client, avec son prix garanti et sa date de fin de validité.
 * T-05 : la table accueille aussi les devis auto (colonnes logement, formule et prix vides selon l'étape).
 * US-18 : l'entité lit les deux produits ; les parcours habitation du sprint 1 (reprise, suppression, PDF, purge)
 * filtrent sur le produit dans {@link net.checkconsulting.assuranceiard.api.repository.DevisEnregistreRepository}.
 */
@Entity
@Table(name = "devis_enregistre")
@Getter
@Setter
@NoArgsConstructor
public class DevisEnregistre {

    public static final String PRODUIT_HABITATION = "HABITATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    /** Vide pour un devis auto (T-05). */
    @Column
    private UUID reference;

    @Column(length = 30)
    private String statut;

    @Column(length = 12)
    private String residence;

    @Column(name = "type_logement", length = 12)
    private String typeLogement;

    @Column
    private String adresse;

    @Column(name = "code_postal", length = 5)
    private String codePostal;

    @Column(length = 100)
    private String ville;

    private Integer surface;

    @Column(name = "nombre_pieces", length = 5)
    private String nombrePieces;

    @Column(length = 30)
    private String antecedent;

    @Column(name = "resilie_contrat", length = 30)
    private String resilieContrat;

    @Column(name = "assurance_antecedent", length = 30)
    private String assuranceAntecedent;

    @Column(length = 30)
    private String etage;

    @Column(length = 20)
    private String dependances;

    @Column(name = "nombre_colocataires", length = 5)
    private String nombreColocataires;

    @Column(name = "assureur_actuel", length = 100)
    private String assureurActuel;

    @Column(name = "nombre_sinistres", length = 5)
    private String nombreSinistres;

    @Column(length = 12)
    private String formule;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> options;

    @Column(length = 3)
    private String franchise;

    @Column(name = "prix_mensuel", precision = 10, scale = 2)
    private BigDecimal prixMensuel;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_fin_validite", nullable = false)
    private LocalDate dateFinValidite;

    @Column(name = "date_dernier_contact", nullable = false)
    private Instant dateDernierContact;

    /** T-05 : HABITATION ou AUTO. */
    @Column(nullable = false, length = 12)
    private String produit = PRODUIT_HABITATION;

    /** T-05 : étape du devis (la colonne statut existante décrit l'occupant du logement). */
    @Column(name = "statut_devis", nullable = false, length = 20)
    private String statutDevis = "EN_COURS";

    @Column(name = "formule_conseillee", length = 20)
    private String formuleConseillee;

    @Column(name = "choix_different_conseil")
    private Boolean choixDifferentConseil;

    /** T-05 : réponses aux questions de besoins (devis auto) ; l'habitation garde recommandation_devis. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reponses_besoins")
    private String reponsesBesoins;

    @Column(name = "date_modification")
    private Instant dateModification;
}
