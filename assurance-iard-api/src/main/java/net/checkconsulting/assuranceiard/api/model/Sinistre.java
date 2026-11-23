package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-05 : déclaration de sinistre (aucune donnée médicale ; numéro attribué à l'envoi). */
@Entity
@Table(name = "sinistre")
@Getter
@Setter
@NoArgsConstructor
public class Sinistre extends EntiteHorodatee {

    private static final Duration DUREE_BROUILLON = Duration.ofDays(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", unique = true, length = 20)
    private String numero;

    @Column(name = "contrat_id", nullable = false)
    private Long contratId;

    @Column(name = "statut", nullable = false, length = 10)
    private String statut = "BROUILLON";

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "date_survenance", nullable = false)
    private LocalDate dateSurvenance;

    @Column(name = "heure_survenance")
    private LocalTime heureSurvenance;

    @Column(name = "lieu", length = 255)
    private String lieu;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "tiers_implique", nullable = false)
    private Boolean tiersImplique = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tiers_infos")
    private String tiersInfos;

    @Column(name = "blesses", nullable = false)
    private Boolean blesses = false;

    @Column(name = "declarant", length = 12)
    private String declarant;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "declarant_infos")
    private String declarantInfos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details")
    private String details;

    @Column(name = "hors_delai", nullable = false)
    private Boolean horsDelai = false;

    @Column(name = "explication_retard", columnDefinition = "text")
    private String explicationRetard;

    @Column(name = "hors_periode_contrat", nullable = false)
    private Boolean horsPeriodeContrat = false;

    @Column(name = "declaration_honneur", nullable = false)
    private Boolean declarationHonneur = false;

    @Column(name = "declare_le")
    private Instant declareLe;

    @Column(name = "brouillon_expire_le", nullable = false)
    private Instant brouillonExpireLe;

    /** Un brouillon expire 30 jours après sa création. */
    @PrePersist
    void expirationDuBrouillon() {
        if (brouillonExpireLe == null) {
            brouillonExpireLe = getDateCreation().plus(DUREE_BROUILLON);
        }
    }
}
