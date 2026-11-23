package net.checkconsulting.moteurtarifaire.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;

/** T-05 : formule auto (TIERS, INTER, TR) d'une version de grille. */
@Entity
@Table(name = "ref_formule")
@Getter
@Setter
public class RefFormule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Version de grille (grille_tarifaire) à laquelle appartient la ligne. */
    @Column(name = "grille_id", nullable = false)
    private Long grilleId;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false, length = 60)
    private String libelle;

    @Column(precision = 10, scale = 2)
    private BigDecimal franchise;

    @Column(name = "prix_mensuel_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixMensuelBase;

    @Column(nullable = false)
    private Integer ordre;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "garanties_incluses", nullable = false, columnDefinition = "text[]")
    private List<String> garantiesIncluses = List.of();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "non_couvert", nullable = false, columnDefinition = "text[]")
    private List<String> nonCouvert = List.of();

    @Column(name = "part_rc", precision = 5, scale = 4)
    private BigDecimal partRc;

    @Column(name = "soumis_bonus_malus", nullable = false)
    private Boolean soumisBonus = false;

    /** Horodatages renseignés par la base (référentiel chargé par script, T-04). */
    @Column(name = "date_creation", insertable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "date_modification", insertable = false, updatable = false)
    private Instant dateModification;
}
