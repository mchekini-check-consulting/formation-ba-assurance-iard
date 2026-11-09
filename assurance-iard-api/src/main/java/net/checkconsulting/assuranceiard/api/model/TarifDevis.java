package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tarif_devis")
@Getter
@Setter
@NoArgsConstructor
public class TarifDevis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_devis", nullable = false, unique = true)
    private UUID referenceDevis;

    @Column(name = "id_tarif", nullable = false)
    private UUID idTarif;

    @Column(name = "code_postal", nullable = false)
    private String codePostal;

    @Column(name = "type_logement", nullable = false)
    private String typeLogement;

    @Column(nullable = false)
    private BigDecimal surface;

    @Column
    private Integer franchise;

    @Column(length = 20)
    private String formule;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> options;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;
}
