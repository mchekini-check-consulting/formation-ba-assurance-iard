package net.checkconsulting.assuranceiard.api.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * T-05 / T-03 : accès à la table de {@link Document}. Un contrat absent s'écrit 0 et une référence absente '' :
 * mêmes valeurs que l'index unique uk_document_en_vigueur (V28).
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /** T-03 : version en vigueur, verrouillée pour être remplacée sans concurrence. */
    @LogMetier
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT d FROM Document d
            WHERE COALESCE(d.contratId, 0) = :contratId AND d.type = :type
              AND COALESCE(d.reference, '') = :reference AND d.statut = 'EN_VIGUEUR'""")
    Optional<Document> enVigueurVerrouille(@Param("contratId") long contratId, @Param("type") String type,
            @Param("reference") String reference);

    /** T-03 : version en vigueur, en lecture (renvoi des conditions particulières vers les conditions générales). */
    @LogMetier
    @Query("""
            SELECT d FROM Document d
            WHERE COALESCE(d.contratId, 0) = :contratId AND d.type = :type
              AND COALESCE(d.reference, '') = :reference AND d.statut = 'EN_VIGUEUR'""")
    Optional<Document> enVigueur(@Param("contratId") long contratId, @Param("type") String type,
            @Param("reference") String reference);

    /**
     * T-03 : documents EN_VIGUEUR pas encore stockés, des types qu'un modèle sait produire. Une version remplacée
     * jamais stockée n'est pas reconstituée (le contrat a changé depuis). Un échec repousse le document en fin de
     * file (date de modification) : il ne bloque pas les suivants.
     */
    @LogMetier
    @Query(value = """
            SELECT id FROM document
            WHERE NOT stocke AND statut = 'EN_VIGUEUR' AND type IN (:types)
            ORDER BY date_modification, id
            LIMIT :limite""", nativeQuery = true)
    List<Long> aStocker(@Param("types") Collection<String> types, @Param("limite") int limite);

    /**
     * T-03 : le document s'il n'est toujours pas stocké, verrouillé ; vide s'il l'est déjà ou si une autre instance
     * du backend le traite (2 réplicas en preprod et prod).
     */
    @LogMetier
    @Query(value = """
            SELECT * FROM document WHERE id = :id AND NOT stocke AND statut = 'EN_VIGUEUR'
            FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    Optional<Document> aStockerVerrouille(@Param("id") long id);

    /**
     * T-03 : comptes propriétaires d'un document, par son contrat (ou celui de son sinistre), sinon par son devis.
     * Vide pour un document commun à tous (conditions générales) ou inconnu.
     */
    @Query(value = """
            SELECT c.compte_id FROM document d
            LEFT JOIN sinistre s ON s.id = d.sinistre_id
            JOIN contrat c ON c.id = COALESCE(d.contrat_id, s.contrat_id)
            WHERE d.id = :id
            UNION
            SELECT de.compte_id FROM document d
            JOIN devis_enregistre de ON de.id = d.devis_id
            WHERE d.id = :id AND d.contrat_id IS NULL AND d.sinistre_id IS NULL""", nativeQuery = true)
    List<Long> proprietaires(@Param("id") long id);

    /** US-18, AC3 : documents en vigueur d'un contrat, de ces types (un document pas encore créé n'apparaît pas). */
    @LogMetier
    @Query("""
            SELECT d FROM Document d
            WHERE d.contratId = :contratId AND d.statut = 'EN_VIGUEUR' AND d.type IN :types
            ORDER BY d.type, d.id""")
    List<Document> enVigueurDuContrat(@Param("contratId") long contratId, @Param("types") Collection<String> types);

    /** US-21, AC2 : toutes les versions d'un document du contrat (en vigueur et remplacées), la plus récente d'abord. */
    @LogMetier
    @Query("""
            SELECT d FROM Document d
            WHERE d.contratId = :contratId AND d.type = :type
            ORDER BY d.version DESC, d.id DESC""")
    List<Document> versionsDuContrat(@Param("contratId") long contratId, @Param("type") String type);
}
