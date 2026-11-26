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

/** US-24 : le fichier d'une pièce jointe de sinistre, conservé en base (10 Mo au plus), jamais sur le disque. */
@Entity
@Table(name = "piece_jointe_contenu")
@Getter
@Setter
@NoArgsConstructor
public class PieceJointeContenu extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "piece_jointe_id", nullable = false, unique = true)
    private Long pieceJointeId;

    @Column(name = "contenu", nullable = false)
    private byte[] contenu;

    @Column(name = "taille_octets", nullable = false)
    private Integer tailleOctets;
}
