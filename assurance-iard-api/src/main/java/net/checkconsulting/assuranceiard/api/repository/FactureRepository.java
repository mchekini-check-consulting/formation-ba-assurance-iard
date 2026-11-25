package net.checkconsulting.assuranceiard.api.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** T-05 : accès à la table de {@link Facture} (requêtes ajoutées avec les fonctionnalités du sprint 2). */
public interface FactureRepository extends JpaRepository<Facture, Long> {

    /** US-18, AC1 : les 3 factures les plus récentes de ces contrats (hypothèse à valider PO). */
    @LogMetier
    List<Facture> findTop3ByContratIdInOrderByPeriodeFinDescIdDesc(Collection<Long> contrats);

    /** US-22, AC2 : toutes les factures de ces contrats, la plus récente d'abord. */
    @LogMetier
    List<Facture> findByContratIdInOrderByPeriodeFinDescIdDesc(Collection<Long> contrats);

    /** US-22, AC2 : une seule facture par contrat et par période. */
    @LogMetier
    Optional<Facture> findFirstByContratIdAndPeriodeDebutAndPeriodeFinOrderByIdAsc(Long contratId,
            LocalDate periodeDebut, LocalDate periodeFin);

    /** US-22 : la facture d'un document PDF (modèle Facture). */
    @LogMetier
    Optional<Facture> findFirstByDocumentId(Long documentId);

    /** US-22 : la facture d'un numéro (document.reference). */
    @LogMetier
    Optional<Facture> findByNumero(String numero);

    /**
     * US-22, RG4 : réserve l'identifiant de la prochaine facture. Le compteur de la table ne revient jamais en
     * arrière : le numéro FAC-AAAA-NNNNNN construit avec lui n'est jamais réutilisé.
     */
    @LogMetier
    @Query(value = "SELECT nextval(pg_get_serial_sequence('facture', 'id'))", nativeQuery = true)
    long reserverIdentifiant();

    /** US-22 : enregistre la facture avec l'identifiant réservé (son numéro en dépend). */
    @LogMetier
    @Modifying
    @Query(value = """
            INSERT INTO facture (id, numero, contrat_id, periode_debut, periode_fin, montant_ht, taxe, montant_ttc,
                                 generee_le)
            VALUES (:id, :numero, :contratId, :periodeDebut, :periodeFin, :montantHt, :taxe, :montantTtc, :genereeLe)
            """, nativeQuery = true)
    void inserer(@Param("id") long id, @Param("numero") String numero, @Param("contratId") long contratId,
            @Param("periodeDebut") LocalDate periodeDebut, @Param("periodeFin") LocalDate periodeFin,
            @Param("montantHt") BigDecimal montantHt, @Param("taxe") BigDecimal taxe,
            @Param("montantTtc") BigDecimal montantTtc, @Param("genereeLe") Instant genereeLe);

    /** US-22, AC2 : verrouille le contrat : deux clics simultanés ne créent pas deux factures pour la même période. */
    @LogMetier
    @Query(value = "SELECT id FROM contrat WHERE id = :id FOR UPDATE", nativeQuery = true)
    List<Long> verrouillerContrat(@Param("id") long contratId);
}
