package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-05 : changement de statut d'un contrat (statut_avant vide à la création). */
@Entity
@Table(name = "historique_statut_contrat")
@Getter
@Setter
@NoArgsConstructor
public class HistoriqueStatutContrat extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contrat_id", nullable = false)
    private Long contratId;

    @Column(name = "statut_avant", length = 30)
    private String statutAvant;

    @Column(name = "statut_apres", nullable = false, length = 30)
    private String statutApres;

    @Column(name = "change_le", nullable = false)
    private Instant changeLe;

    @Column(name = "motif", length = 255)
    private String motif;

    @Column(name = "auteur", nullable = false, length = 12)
    private String auteur;
}
