package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-05 : plaque d'immatriculation de test et comportement simulé de la recherche (données T-04). */
@Entity
@Table(name = "ref_plaque_test")
@Getter
@Setter
@NoArgsConstructor
public class RefPlaqueTest extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "immatriculation", nullable = false, unique = true, length = 10)
    private String immatriculation;

    @Column(name = "ref_vehicule_id")
    private Long refVehiculeId;

    @Column(name = "comportement", nullable = false, length = 10)
    private String comportement;
}
