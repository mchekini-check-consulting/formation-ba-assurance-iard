package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T-05 : preuve de la signature électronique d'un contrat. */
@Entity
@Table(name = "preuve_signature")
@Getter
@Setter
@NoArgsConstructor
public class PreuveSignature extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contrat_id", nullable = false)
    private Long contratId;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "empreinte_document", nullable = false, length = 64)
    private String empreinteDocument;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;

    @Column(name = "signe_le", nullable = false)
    private Instant signeLe;

    @Column(name = "nb_essais", nullable = false)
    private Integer nbEssais;
}
