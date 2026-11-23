package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-05 : volet auto d'un devis enregistré (un pour un). */
@Entity
@Table(name = "devis_auto")
@Getter
@Setter
@NoArgsConstructor
public class DevisAuto extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "devis_id", nullable = false, unique = true)
    private Long devisId;

    @Column(name = "vehicule_id")
    private Long vehiculeId;

    @Column(name = "conducteur_id")
    private Long conducteurId;

    @Column(name = "assure_plus_un_an", nullable = false)
    private Boolean assurePlusUnAn = false;

    @Column(name = "ancien_assureur", length = 100)
    private String ancienAssureur;

    @Column(name = "ancien_numero_contrat", length = 40)
    private String ancienNumeroContrat;

    @Column(name = "date_effet_souhaitee")
    private LocalDate dateEffetSouhaitee;
}
