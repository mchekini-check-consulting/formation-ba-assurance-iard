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

/** US-31 étape 4 : souscripteur d'un devis auto, rattaché au compte lors de l'enregistrement. */
@Entity
@Table(name = "souscripteur_auto")
@Getter
@Setter
@NoArgsConstructor
public class SouscripteurAuto extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "devis_auto_id", nullable = false, unique = true)
    private Long devisAutoId;

    @Column(name = "compte_id")
    private Long compteId;

    @Column(name = "civilite", nullable = false, length = 10)
    private String civilite;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "telephone", nullable = false, length = 10)
    private String telephone;

    @Column(name = "adresse", nullable = false, length = 255)
    private String adresse;

    @Column(name = "code_postal", nullable = false, length = 5)
    private String codePostal;

    @Column(name = "ville", nullable = false, length = 100)
    private String ville;
}
