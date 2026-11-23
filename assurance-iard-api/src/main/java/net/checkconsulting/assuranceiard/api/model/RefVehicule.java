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

/** T-05 : référentiel des véhicules (données chargées par T-04). */
@Entity
@Table(name = "ref_vehicule")
@Getter
@Setter
@NoArgsConstructor
public class RefVehicule extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "marque", nullable = false, length = 60)
    private String marque;

    @Column(name = "modele", nullable = false, length = 60)
    private String modele;

    @Column(name = "energie", nullable = false, length = 12)
    private String energie;

    @Column(name = "version", length = 60)
    private String version;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "valeur_neuve", precision = 10, scale = 2)
    private BigDecimal valeurNeuve;
}
