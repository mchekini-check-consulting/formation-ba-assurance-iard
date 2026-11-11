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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.checkconsulting.assuranceiard.api.securite.MotifRefus;
import net.checkconsulting.assuranceiard.api.securite.TypeRessource;

/** US-04, RG3 : tentative d'accès refusée (date, utilisateur, ressource demandée, action). */
@Entity
@Table(name = "acces_refuse")
@Getter
@Setter
@NoArgsConstructor
public class AccesRefuse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_tentative", nullable = false)
    private Instant dateTentative;

    @Column(name = "compte_id")
    private Long compteId;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Profil profil;

    @Column(nullable = false, length = 10)
    private String methode;

    @Column(nullable = false)
    private String chemin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeRessource ressource;

    @Column(length = 100)
    private String identifiant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rubrique rubrique;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Droit droit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MotifRefus motif;
}
