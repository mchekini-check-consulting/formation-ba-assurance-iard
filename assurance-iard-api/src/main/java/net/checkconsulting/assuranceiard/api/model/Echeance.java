package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-05 : échéance d'un contrat (TTC = HT + taxe sur les conventions d'assurance). */
@Entity
@Table(name = "echeance")
@Getter
@Setter
@NoArgsConstructor
public class Echeance extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contrat_id", nullable = false)
    private Long contratId;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(name = "montant_ht", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantHt;

    @Column(name = "taxe", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxe;

    @Column(name = "montant_ttc", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTtc;

    @Column(name = "statut", nullable = false, length = 10)
    private String statut = "A_VENIR";
}
