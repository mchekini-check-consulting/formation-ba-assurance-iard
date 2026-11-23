package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-05 : facture d'un contrat sur une période. */
@Entity
@Table(name = "facture")
@Getter
@Setter
@NoArgsConstructor
public class Facture extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 20)
    private String numero;

    @Column(name = "contrat_id", nullable = false)
    private Long contratId;

    @Column(name = "periode_debut", nullable = false)
    private LocalDate periodeDebut;

    @Column(name = "periode_fin", nullable = false)
    private LocalDate periodeFin;

    @Column(name = "montant_ht", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantHt;

    @Column(name = "taxe", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxe;

    @Column(name = "montant_ttc", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTtc;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "generee_le", nullable = false)
    private Instant genereeLe;
}
