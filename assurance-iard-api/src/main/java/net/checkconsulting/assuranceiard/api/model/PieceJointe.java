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

/** T-05 / US-24 : pièce jointe d'un sinistre (PDF, JPG ou PNG, 10 Mo maximum ; contenu en base, SHA-256). */
@Entity
@Table(name = "piece_jointe")
@Getter
@Setter
@NoArgsConstructor
public class PieceJointe extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sinistre_id", nullable = false)
    private Long sinistreId;

    @Column(name = "type_piece", nullable = false, length = 30)
    private String typePiece;

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "format", nullable = false, length = 3)
    private String format;

    @Column(name = "taille_octets", nullable = false)
    private Integer tailleOctets;

    @Column(name = "cle_stockage", length = 255)
    private String cleStockage;

    @Column(name = "statut", nullable = false, length = 8)
    private String statut = "STOCKEE";

    @Column(name = "motif_refus", length = 255)
    private String motifRefus;

    @Column(name = "empreinte", length = 64)
    private String empreinte;
}
