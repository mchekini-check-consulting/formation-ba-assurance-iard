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

/** T-05 : déclaration d'un véhicule au fichier des véhicules assurés (FVA). */
@Entity
@Table(name = "declaration_fva")
@Getter
@Setter
@NoArgsConstructor
public class DeclarationFva extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contrat_id", nullable = false)
    private Long contratId;

    @Column(name = "immatriculation", length = 10)
    private String immatriculation;

    @Column(name = "statut", nullable = false, length = 10)
    private String statut = "A_ENVOYER";

    @Column(name = "envoye_le")
    private Instant envoyeLe;

    @Column(name = "reference_accuse", length = 100)
    private String referenceAccuse;

    @Column(name = "motif_rejet", length = 255)
    private String motifRejet;
}
