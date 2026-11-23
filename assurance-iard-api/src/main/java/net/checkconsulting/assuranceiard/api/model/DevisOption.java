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

/** T-05 : option choisie dans un devis, avec son prix. */
@Entity
@Table(name = "devis_option")
@Getter
@Setter
@NoArgsConstructor
public class DevisOption extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "devis_id", nullable = false)
    private Long devisId;

    @Column(name = "code_option", nullable = false, length = 30)
    private String codeOption;

    @Column(name = "prix_mensuel_ttc", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixMensuelTtc;
}
