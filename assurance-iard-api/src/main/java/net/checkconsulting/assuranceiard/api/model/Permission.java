package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permission")
@Getter
@Setter
@NoArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Profil profil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rubrique rubrique;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Droit droit;

    @Column(nullable = false)
    private boolean valeur;
}
