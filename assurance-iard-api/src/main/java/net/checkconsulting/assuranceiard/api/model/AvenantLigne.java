package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-05 : option ajoutée ou retirée par un avenant. */
@Entity
@Table(name = "avenant_ligne")
@Getter
@Setter
@NoArgsConstructor
public class AvenantLigne extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "avenant_id", nullable = false)
    private Long avenantId;

    @Column(name = "code_option", nullable = false, length = 30)
    private String codeOption;

    @Column(name = "action", nullable = false, length = 10)
    private String action;

    @Column(name = "prix_mensuel_ttc", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixMensuelTtc;
}
