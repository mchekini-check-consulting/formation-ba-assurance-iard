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

/** T-03 : le PDF d'un document, conservé en base (5 Mo au plus). */
@Entity
@Table(name = "document_contenu")
@Getter
@Setter
@NoArgsConstructor
public class DocumentContenu extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, unique = true)
    private Long documentId;

    @Column(name = "contenu", nullable = false)
    private byte[] contenu;

    @Column(name = "taille_octets", nullable = false)
    private Integer tailleOctets;
}
