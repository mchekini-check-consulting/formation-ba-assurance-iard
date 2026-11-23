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

/** T-05 : option souscrite dans un contrat (date de fin si retirée par avenant). */
@Entity
@Table(name = "contrat_option")
@Getter
@Setter
@NoArgsConstructor
public class ContratOption extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contrat_id", nullable = false)
    private Long contratId;

    @Column(name = "code_option", nullable = false, length = 30)
    private String codeOption;

    @Column(name = "prix_mensuel_ttc", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixMensuelTtc;

    @Column(name = "obligatoire", nullable = false)
    private Boolean obligatoire = false;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;
}
