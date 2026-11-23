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

/** T-05 : véhicule d'un devis ou d'un contrat auto. */
@Entity
@Table(name = "vehicule")
@Getter
@Setter
@NoArgsConstructor
public class Vehicule extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "immatriculation", length = 10)
    private String immatriculation;

    @Column(name = "non_immatricule", nullable = false)
    private Boolean nonImmatricule = false;

    @Column(name = "marque", nullable = false, length = 60)
    private String marque;

    @Column(name = "modele", nullable = false, length = 60)
    private String modele;

    @Column(name = "version", length = 60)
    private String version;

    @Column(name = "energie", nullable = false, length = 12)
    private String energie;

    @Column(name = "date_mise_circulation")
    private LocalDate dateMiseCirculation;

    @Column(name = "date_acquisition")
    private LocalDate dateAcquisition;

    @Column(name = "mode_achat", length = 10)
    private String modeAchat;

    @Column(name = "titulaire_carte_grise", length = 20)
    private String titulaireCarteGrise;

    @Column(name = "usage", length = 15)
    private String usage;

    @Column(name = "km_annuel")
    private Integer kmAnnuel;

    @Column(name = "stationnement", length = 15)
    private String stationnement;

    @Column(name = "valeur_estimee", precision = 10, scale = 2)
    private BigDecimal valeurEstimee;

    @Column(name = "source", nullable = false, length = 10)
    private String source;
}
