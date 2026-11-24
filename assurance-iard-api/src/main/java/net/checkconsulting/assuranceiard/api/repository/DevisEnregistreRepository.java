package net.checkconsulting.assuranceiard.api.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.DevisEnregistre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * US-12 / US-18 : devis enregistrés. Les parcours du sprint 1 (Mes devis, reprise, suppression, PDF, purge RGPD) ne
 * concernent que l'habitation : leurs requêtes filtrent sur le produit. Les devis auto ne sont lus que par le tableau
 * de bord (US-18), en consultation.
 */
public interface DevisEnregistreRepository extends JpaRepository<DevisEnregistre, Long> {

    /** Devis habitation du client (enregistrement, reprise, suppression). */
    @LogMetier
    @Query("""
            SELECT d FROM DevisEnregistre d
            WHERE d.compte = :compte AND d.reference = :reference AND d.produit = 'HABITATION'""")
    Optional<DevisEnregistre> habitationDuClient(@Param("compte") Compte compte,
            @Param("reference") UUID reference);

    /** « Mes devis » : devis habitation du client, les plus récents d'abord. */
    @LogMetier
    @Query("""
            SELECT d FROM DevisEnregistre d
            WHERE d.compte = :compte AND d.produit = 'HABITATION'
            ORDER BY d.dateCreation DESC""")
    List<DevisEnregistre> habitationsDuClient(@Param("compte") Compte compte);

    /** PDF d'un devis habitation (sans compte). */
    @Query("""
            SELECT d FROM DevisEnregistre d
            WHERE d.reference = :reference AND d.produit = 'HABITATION'
            ORDER BY d.dateCreation DESC LIMIT 1""")
    Optional<DevisEnregistre> derniereHabitation(@Param("reference") UUID reference);

    /**
     * US-18, AC1 : devis en cours du client, habitation et auto (non transformés, non expirés, avec une formule et
     * un prix : un devis auto incomplet n'est pas affiché).
     */
    @LogMetier
    @Query("""
            SELECT d FROM DevisEnregistre d
            WHERE d.compte = :compte AND d.statutDevis NOT IN ('TRANSFORME', 'EXPIRE')
              AND d.dateFinValidite >= :aujourdhui AND d.formule IS NOT NULL AND d.prixMensuel IS NOT NULL
            ORDER BY d.dateCreation DESC""")
    List<DevisEnregistre> enCours(@Param("compte") Compte compte, @Param("aujourdhui") LocalDate aujourdhui);

    /** RGPD : purge des devis habitation (les devis auto ont des lignes liées ; leur purge viendra avec eux). */
    @LogMetier
    @Modifying
    @Query("""
            DELETE FROM DevisEnregistre d
            WHERE d.dateDernierContact < :limite AND d.produit = 'HABITATION'""")
    int supprimerHabitationsSansContactDepuis(@Param("limite") Instant limite);

    /** US-04 : comptes propriétaires du devis. */
    @Query("SELECT d.compte.id FROM DevisEnregistre d WHERE d.reference = :reference")
    List<Long> proprietaires(@Param("reference") UUID reference);
}
