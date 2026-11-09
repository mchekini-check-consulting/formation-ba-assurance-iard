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
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Étape « double authentification » d'une connexion : le jeton et le code ne sont stockés que chiffrés. */
@Entity
@Table(name = "code_verification")
@Getter
@Setter
@NoArgsConstructor
public class CodeVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @Column(name = "jeton_hash", nullable = false, unique = true, length = 64)
    private String jetonHash;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(nullable = false)
    private int erreurs;

    @Column(name = "date_expiration", nullable = false)
    private Instant dateExpiration;
}
