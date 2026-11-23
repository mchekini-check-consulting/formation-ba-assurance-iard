package net.checkconsulting.moteurtarifaire.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** T-05 : coefficient de tarif auto, par tranche (âge, bonus-malus, valeur du véhicule) ou par valeur (usage). */
@Entity
@Table(name = "ref_tarif")
@Getter
@Setter
public class RefTarif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Version de grille (grille_tarifaire) à laquelle appartient la ligne. */
    @Column(name = "grille_id", nullable = false)
    private Long grilleId;

    @Column(nullable = false, length = 20)
    private String critere;

    @Column(name = "borne_min", precision = 10, scale = 2)
    private BigDecimal borneMin;

    @Column(name = "borne_max", precision = 10, scale = 2)
    private BigDecimal borneMax;

    @Column(length = 30)
    private String valeur;

    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal coefficient;

    /** Horodatages renseignés par la base (référentiel chargé par script, T-04). */
    @Column(name = "date_creation", insertable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "date_modification", insertable = false, updatable = false)
    private Instant dateModification;
}
