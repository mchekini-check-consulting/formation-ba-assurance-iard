package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** US-13 : description d'un fichier déposé ; le fichier lui-même est rangé sous cleStockage. */
@Entity
@Table(name = "justificatif")
@Getter
@Setter
@NoArgsConstructor
public class Justificatif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierSouscription dossier;

    @Column(nullable = false, length = 10)
    private String categorie;

    @Column(name = "type_document", nullable = false, length = 20)
    private String typeDocument;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "type_fichier", nullable = false, length = 3)
    private String typeFichier;

    @Column(nullable = false)
    private int taille;

    @Column(name = "cle_stockage", nullable = false, length = 40)
    private String cleStockage;
}
