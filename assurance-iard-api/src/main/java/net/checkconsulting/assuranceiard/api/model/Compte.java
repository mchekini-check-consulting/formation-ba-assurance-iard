package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "compte")
@Getter
@Setter
@NoArgsConstructor
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "mot_de_passe_hash", nullable = false, length = 100)
    private String motDePasseHash;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "echecs_connexion", nullable = false)
    private int echecsConnexion;

    @Column(name = "bloque_jusqu_a")
    private Instant bloqueJusquA;

    @Column(length = 8)
    private String civilite;

    @Column(length = 100)
    private String nom;

    @Column(length = 100)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(length = 10)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Profil profil = Profil.PROSPECT;
}
