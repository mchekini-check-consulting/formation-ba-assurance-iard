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

    /** Dossier de souscription habitation ; vide pour un mandat auto (T-05). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", unique = true)
    private DossierSouscription dossier;

    /** T-05 : client du mandat. Renseigné par la base à partir du dossier s'il est laissé vide. */
    @Column(name = "compte_id")
    private Long compteId;

    /** T-05 : calculé par la base, 4 derniers caractères seulement (« •••• 4821 »). */
    @Column(name = "iban_masque", length = 34, insertable = false, updatable = false)
    private String ibanMasque;

    @Column(length = 11)
    private String bic;

    @Column(length = 100)
    private String banque;

    /** T-05 : ACTIF ou REVOQUE. */
    @Column(nullable = false, length = 10)
    private String statut = "ACTIF";

    @Column(name = "date_modification")
    private Instant dateModification;

    @Column(nullable = false, unique = true, length = 35)
    private String rum;

    @Column(length = 140)
    private String titulaire;

    @Column(name = "iban_fin", length = 4)
    private String ibanFin;

    @Column(name = "iban_debut", length = 4)
    private String ibanDebut;

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
