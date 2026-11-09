package net.checkconsulting.moteurtarifaire.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "calcul_tarif")
@Getter
@Setter
public class CalculTarif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_tarif", nullable = false, unique = true, updatable = false)
    private UUID idTarif;

    @Column(name = "version_grille", nullable = false, updatable = false)
    private String versionGrille;

    @Column(nullable = false, updatable = false)
    private boolean tarifable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "donnees_recues", nullable = false, updatable = false)
    private String donneesRecues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private String resultat;

    @Column(name = "date_calcul", nullable = false, updatable = false)
    private Instant dateCalcul;

    @Column(name = "date_validite", updatable = false)
    private LocalDate dateValidite;
}
