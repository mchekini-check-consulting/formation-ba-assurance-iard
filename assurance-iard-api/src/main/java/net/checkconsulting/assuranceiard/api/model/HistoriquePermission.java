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

@Entity
@Table(name = "historique_permission")
@Getter
@Setter
@NoArgsConstructor
public class HistoriquePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "modifie_le", nullable = false)
    private Instant modifieLe;

    @Column(name = "modifie_par_email", nullable = false, length = 254)
    private String modifieParEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Profil profil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rubrique rubrique;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Droit droit;

    @Column(name = "ancienne_valeur", nullable = false)
    private boolean ancienneValeur;

    @Column(name = "nouvelle_valeur", nullable = false)
    private boolean nouvelleValeur;
}
