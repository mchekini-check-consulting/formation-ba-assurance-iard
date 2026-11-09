package net.checkconsulting.moteurtarifaire.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "parametre_tarifaire")
@Getter
@Setter
public class ParametreTarifaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grille_id", nullable = false)
    private Long grilleId;

    @Column(nullable = false)
    private String categorie;

    @Column(nullable = false)
    private String cle;

    @Column(nullable = false)
    private String valeur;
}
