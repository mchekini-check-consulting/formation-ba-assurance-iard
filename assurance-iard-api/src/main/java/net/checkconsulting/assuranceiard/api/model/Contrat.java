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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * US-15 : contrat généré à l'issue de la souscription, en attente de signature du client.
 * US-18 : lit aussi les contrats auto (T-05) : sans dossier ni logement, rattachés au client par compte_id.
 * Les colonnes T-05 sont en lecture seule : un contrat créé par la souscription habitation s'enregistre comme au
 * sprint 1, et le déclencheur tg_contrat_completer (V23) complète client, formule et mandat depuis le dossier.
 */
@Entity
@Table(name = "contrat")
@Getter
@Setter
@NoArgsConstructor
public class Contrat {

    public static final String HABITATION = "HABITATION";
    public static final String AUTO = "AUTO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    /** Habitation du sprint 1 seulement ; vide pour un contrat auto. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id")
    private DossierSouscription dossier;

    @Column(nullable = false, length = 30)
    private String statut;

    @Column(name = "adresse_logement", length = 255)
    private String adresseLogement;

    @Column(name = "code_postal", length = 5)
    private String codePostal;

    @Column(length = 100)
    private String ville;

    @Column(name = "type_logement", length = 50)
    private String typeLogement;

    private Integer surface;

    /** T-05 : décimal(10,2) en euros. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal franchise;

    @Column(name = "cotisation_mensuelle", nullable = false, precision = 10, scale = 2)
    private BigDecimal cotisationMensuelle;

    @Column(name = "cotisation_annuelle", nullable = false, precision = 10, scale = 2)
    private BigDecimal cotisationAnnuelle;

    @Column(name = "resiliation_hamon", nullable = false)
    private Boolean resiliationHamon;

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    /** US-18 : client propriétaire du contrat (habitation et auto). Lecture seule. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_id", insertable = false, updatable = false)
    private Compte compte;

    /** T-05 : HABITATION ou AUTO. Lecture seule (HABITATION par défaut en base). */
    @Column(name = "produit", length = 12, insertable = false, updatable = false)
    private String produit;

    /** T-05 : lecture seule (complétée depuis le dossier pour l'habitation du sprint 1). */
    @Column(name = "formule", length = 20, insertable = false, updatable = false)
    private String formule;

    @Column(name = "date_fin", insertable = false, updatable = false)
    private LocalDate dateFin;

    /** T-05 : véhicule d'un contrat auto. Lecture seule. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicule_id", insertable = false, updatable = false)
    private Vehicule vehicule;
}
