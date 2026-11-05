package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** US-14 : mandat de prélèvement SEPA. L'IBAN complet n'est jamais conservé. */
@Entity
@Table(name = "mandat_sepa")
@Getter
@Setter
@NoArgsConstructor
public class MandatSepa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false, unique = true)
    private DossierSouscription dossier;

    @Column(nullable = false, unique = true, length = 35)
    private String rum;

    @Column(length = 140)
    private String titulaire;

    @Column(name = "iban_fin", length = 4)
    private String ibanFin;

    @Column(name = "jour_prelevement")
    private Integer jourPrelevement;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_premier_prelevement")
    private LocalDate datePremierPrelevement;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;

    @Column(name = "date_signature")
    private Instant dateSignature;
}
