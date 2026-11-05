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

/** US-15 : contrat généré à l'issue de la souscription, en attente de signature du client. */
@Entity
@Table(name = "contrat")
@Getter
@Setter
@NoArgsConstructor
public class Contrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierSouscription dossier;

    @Column(nullable = false, length = 30)
    private String statut;

    @Column(name = "adresse_logement", nullable = false, length = 255)
    private String adresseLogement;

    @Column(name = "code_postal", nullable = false, length = 5)
    private String codePostal;

    @Column(nullable = false, length = 100)
    private String ville;

    @Column(name = "type_logement", nullable = false, length = 50)
    private String typeLogement;

    @Column(nullable = false)
    private Integer surface;

    @Column(nullable = false)
    private Integer franchise;

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
}
