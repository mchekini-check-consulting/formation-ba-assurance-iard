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

/** T-05 : conducteur d'un devis ou d'un contrat auto (aucune donnée de santé). */
@Entity
@Table(name = "conducteur")
@Getter
@Setter
@NoArgsConstructor
public class Conducteur extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "est_souscripteur", nullable = false)
    private Boolean estSouscripteur = true;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "date_permis")
    private LocalDate datePermis;

    @Column(name = "coefficient_bonus_malus", precision = 3, scale = 2)
    private BigDecimal coefficientBonusMalus;

    @Column(name = "annees_assurance")
    private Integer anneesAssurance;

    @Column(name = "nb_sinistres_3_ans")
    private Integer nbSinistres3Ans;

    @Column(name = "resiliation_assureur", nullable = false, length = 20)
    private String resiliationAssureur = "AUCUNE";

    @Column(name = "suspension_permis", nullable = false)
    private Boolean suspensionPermis = false;

    @Column(name = "condamnation_alcool_stupefiants", nullable = false)
    private Boolean condamnationAlcoolStupefiants = false;

    @Column(name = "eligible_en_ligne")
    private Boolean eligibleEnLigne;

    @Column(name = "conducteur_principal", length = 25)
    private String conducteurPrincipal;

    @Column(name = "conduite_accompagnee", nullable = false)
    private Boolean conduiteAccompagnee = false;

    @Column(name = "nb_sinistres_non_responsables_3_ans")
    private Integer nbSinistresNonResponsables3Ans;
}
