package net.checkconsulting.assuranceiard.api.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.checkconsulting.assuranceiard.api.logging.LogMetier;
import net.checkconsulting.assuranceiard.api.model.Compte;
import net.checkconsulting.assuranceiard.api.model.Contrat;
import net.checkconsulting.assuranceiard.api.model.DossierSouscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    @LogMetier
    Optional<Contrat> findByDossier(DossierSouscription dossier);

    /** US-04 : contrats d'un client passés par un dossier de souscription (back-office du sprint 1). */
    @LogMetier
    List<Contrat> findByDossierCompteOrderByDateCreationDesc(Compte compte);

    /** US-18 : tous les contrats du client, habitation et auto, rattachés par contrat.compte_id. */
    @LogMetier
    @Query("""
            SELECT c FROM Contrat c LEFT JOIN FETCH c.vehicule LEFT JOIN FETCH c.dossier LEFT JOIN FETCH c.mandatSepa
            WHERE c.compte = :compte ORDER BY c.dateCreation DESC, c.id DESC""")
    List<Contrat> duClient(@Param("compte") Compte compte);

    /** US-18 : fiche contrat, identifiée par son numéro. */
    @LogMetier
    @Query("""
            SELECT c FROM Contrat c LEFT JOIN FETCH c.vehicule LEFT JOIN FETCH c.dossier LEFT JOIN FETCH c.mandatSepa
            WHERE c.numero = :numero""")
    Optional<Contrat> parNumero(@Param("numero") String numero);

    /** US-04 / US-18 : comptes propriétaires du contrat de ce devis (par contrat.compte_id). */
    @Query("SELECT c.compte.id FROM Contrat c WHERE c.dossier.referenceDevis = :referenceDevis")
    List<Long> proprietaires(@Param("referenceDevis") UUID referenceDevis);

    /** US-18 : comptes propriétaires du contrat de ce numéro. */
    @Query("SELECT c.compte.id FROM Contrat c WHERE c.numero = :numero")
    List<Long> proprietairesParNumero(@Param("numero") String numero);

    /** US-31 étape 5 : contrat auto généré depuis ce devis (idempotence). */
    @LogMetier
    Optional<Contrat> findByDevisAutoId(Long devisAutoId);

    /** US-33 : contrats en attente d'effet dont la date d'effet est atteinte. */
    @LogMetier
    @Query("SELECT c FROM Contrat c WHERE c.statut = 'EN_ATTENTE_EFFET' AND c.dateEffet <= :date")
    List<Contrat> enAttenteEffetAvant(@Param("date") LocalDate date);

    /** RG1 : prochain numéro de séquence pour l'année donnée. */
    @LogMetier
    @Query(value = """
            SELECT COALESCE(MAX(CAST(SPLIT_PART(numero, '-', 3) AS BIGINT)), 0) + 1
            FROM contrat
            WHERE LEFT(numero, 7) = :prefixe
            """, nativeQuery = true)
    long prochainNumeroAnnee(@Param("prefixe") String prefixe);
}
