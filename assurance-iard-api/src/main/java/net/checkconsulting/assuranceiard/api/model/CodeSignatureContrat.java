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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** US-16 : code à 6 chiffres envoyé par e-mail pour signer le contrat (RG1–RG3). */
@Entity
@Table(name = "code_signature_contrat")
@Getter
@Setter
@NoArgsConstructor
public class CodeSignatureContrat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrat_id", nullable = false)
    private Contrat contrat;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "date_expiration", nullable = false)
    private Instant dateExpiration;

    @Column(nullable = false)
    private int essais;
}
