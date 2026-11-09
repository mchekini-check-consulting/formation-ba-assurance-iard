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

/** US-12 : devis enregistré par un client, avec son prix garanti et sa date de fin de validité. */
@Entity
@Table(name = "devis_enregistre")
@Getter
@Setter
@NoArgsConstructor
public class DevisEnregistre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @Column(nullable = false)
    private UUID reference;

    @Column(length = 30)
    private String statut;

    @Column(length = 12)
    private String residence;

    @Column(name = "type_logement", nullable = false, length = 12)
    private String typeLogement;

    @Column(nullable = false)
    private String adresse;

    @Column(name = "code_postal", nullable = false, length = 5)
    private String codePostal;

    @Column(nullable = false, length = 100)
    private String ville;

    @Column(nullable = false)
    private int surface;

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

    @Column(nullable = false, length = 12)
    private String formule;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> options;

    @Column(nullable = false, length = 3)
    private String franchise;

    @Column(name = "prix_mensuel", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixMensuel;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_fin_validite", nullable = false)
    private LocalDate dateFinValidite;

    @Column(name = "date_dernier_contact", nullable = false)
    private Instant dateDernierContact;
}
