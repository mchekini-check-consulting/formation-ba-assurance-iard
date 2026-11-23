package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-05 : résiliation de l'ancien contrat par la loi Hamon. */
@Entity
@Table(name = "resiliation_hamon")
@Getter
@Setter
@NoArgsConstructor
public class ResiliationHamon extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contrat_id", nullable = false)
    private Long contratId;

    @Column(name = "ancien_assureur", nullable = false, length = 100)
    private String ancienAssureur;

    @Column(name = "ancien_numero", nullable = false, length = 40)
    private String ancienNumero;

    @Column(name = "statut", nullable = false, length = 12)
    private String statut = "A_ENVOYER";

    @Column(name = "envoye_le")
    private Instant envoyeLe;

    @Column(name = "date_fin_ancien")
    private LocalDate dateFinAncien;

    @Column(name = "nb_essais", nullable = false)
    private Integer nbEssais = 0;
}
