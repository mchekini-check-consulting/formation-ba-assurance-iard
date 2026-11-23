package net.checkconsulting.assuranceiard.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/** T-05 : date_creation et date_modification, présentes sur chaque nouvelle table, renseignées automatiquement. */
@MappedSuperclass
@Getter
@Setter
public abstract class EntiteHorodatee {

    @Column(name = "date_creation", nullable = false, updatable = false)
    private Instant dateCreation;

    @Column(name = "date_modification", nullable = false)
    private Instant dateModification;

    @PrePersist
    void avantCreation() {
        Instant maintenant = Instant.now();
        if (dateCreation == null) {
            dateCreation = maintenant;
        }
        dateModification = maintenant;
    }

    @PreUpdate
    void avantModification() {
        dateModification = Instant.now();
    }
}
