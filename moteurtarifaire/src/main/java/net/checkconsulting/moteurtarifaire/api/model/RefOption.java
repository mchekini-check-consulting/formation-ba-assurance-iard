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

/** T-05 : option tarifée d'une version de grille, avec les formules compatibles. */
@Entity
@Table(name = "ref_option")
@Getter
@Setter
public class RefOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Version de grille (grille_tarifaire) à laquelle appartient la ligne. */
    @Column(name = "grille_id", nullable = false)
    private Long grilleId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String libelle;

    @Column(name = "prix_mensuel", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixMensuel;

    @Column(nullable = false, length = 12)
    private String produit;

    @Column(nullable = false)
    private Boolean obligatoire = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "formules_compatibles", nullable = false, columnDefinition = "text[]")
    private List<String> formulesCompatibles = List.of();

    /** Horodatages renseignés par la base (référentiel chargé par script, T-04). */
    @Column(name = "date_creation", insertable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "date_modification", insertable = false, updatable = false)
    private Instant dateModification;
}
