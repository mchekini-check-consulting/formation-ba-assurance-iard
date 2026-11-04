package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** RG4 de l'US-10 : réponses du prospect et recommandation reçue, pour alimenter la fiche conseil. */
@Entity
@Table(name = "recommandation_devis")
@Getter
@Setter
@NoArgsConstructor
public class RecommandationDevis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID reference;

    @Column(name = "valeur_contenu", nullable = false)
    private int valeurContenu;

    @Column(name = "objets_valeur", nullable = false)
    private boolean objetsValeur;

    @Column(name = "appareils_couteux", nullable = false)
    private boolean appareilsCouteux;

    @Column(nullable = false, length = 10)
    private String remboursement;

    @Column(nullable = false)
    private boolean assistance;

    @Column(nullable = false)
    private boolean litiges;

    @Column(nullable = false, length = 12)
    private String priorite;

    @Column(nullable = false, length = 12)
    private String formule;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> raisons;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private List<String> options;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation;
}
