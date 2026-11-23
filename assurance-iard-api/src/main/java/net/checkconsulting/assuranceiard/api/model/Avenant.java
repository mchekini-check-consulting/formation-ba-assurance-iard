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

/** T-05 : avenant d'un contrat (un seul EN_ATTENTE par contrat). */
@Entity
@Table(name = "avenant")
@Getter
@Setter
@NoArgsConstructor
public class Avenant extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "contrat_id", nullable = false)
    private Long contratId;

    @Column(name = "statut", nullable = false, length = 10)
    private String statut = "EN_ATTENTE";

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Column(name = "prime_avant_ttc", nullable = false, precision = 10, scale = 2)
    private BigDecimal primeAvantTtc;

    @Column(name = "prime_apres_ttc", nullable = false, precision = 10, scale = 2)
    private BigDecimal primeApresTtc;

    @Column(name = "accepte_le")
    private Instant accepteLe;

    @Column(name = "accepte_ip", length = 45)
    private String accepteIp;

    @Column(name = "document_id")
    private Long documentId;
}
