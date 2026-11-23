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

/**
 * T-05 : document versionné. T-03 : une seule version EN_VIGUEUR par contrat, type et référence ; le PDF est rangé
 * dans {@link DocumentContenu}.
 */
@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor
public class Document extends EntiteHorodatee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contrat_id")
    private Long contratId;

    @Column(name = "devis_id")
    private Long devisId;

    @Column(name = "sinistre_id")
    private Long sinistreId;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    /** T-03 : n° de facture ou d'avenant, produit (conditions générales), formule (IPID) ; vide sinon. */
    @Column(name = "reference", length = 40)
    private String reference;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "statut", nullable = false, length = 12)
    private String statut = "EN_VIGUEUR";

    @Column(name = "motif", length = 100)
    private String motif;

    @Column(name = "cle_stockage", length = 255)
    private String cleStockage;

    @Column(name = "empreinte", length = 64)
    private String empreinte;

    @Column(name = "stocke", nullable = false)
    private Boolean stocke = false;
}
