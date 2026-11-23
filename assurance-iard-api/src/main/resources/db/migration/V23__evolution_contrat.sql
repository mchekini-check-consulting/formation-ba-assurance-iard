-- T-05 : le contrat devient multi-produit (habitation, auto).
-- Colonnes existantes réutilisées (aucun renommage) : cotisation_mensuelle / cotisation_annuelle = primes TTC.
-- Le statut des contrats existants n'est pas modifié (décision PO). Aucune suppression en cascade vers contrat.

ALTER TABLE contrat
    ADD COLUMN produit                 VARCHAR(12) NOT NULL DEFAULT 'HABITATION',
    ADD COLUMN compte_id               BIGINT,
    ADD COLUMN formule                 VARCHAR(20),
    ADD COLUMN date_fin                DATE,
    ADD COLUMN motif_fin               VARCHAR(20),
    ADD COLUMN frequence_paiement      VARCHAR(10),
    ADD COLUMN jour_prelevement        INTEGER,
    ADD COLUMN mandat_sepa_id          BIGINT,
    ADD COLUMN vehicule_id             BIGINT,
    ADD COLUMN conducteur_principal_id BIGINT,
    ADD COLUMN devis_id                BIGINT,
    ADD COLUMN signe_le                TIMESTAMP WITH TIME ZONE,
    ADD COLUMN justificatifs_recus     BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN date_modification       TIMESTAMP WITH TIME ZONE;

-- Franchise en décimal, sans perte (les entiers deviennent 300.00, etc.).
ALTER TABLE contrat ALTER COLUMN franchise TYPE NUMERIC(10, 2) USING franchise::NUMERIC(10, 2);

-- Valeurs déduites du dossier de souscription habitation : client, formule (prix du moteur, sinon devis enregistré,
-- sinon recommandation), mandat signé, devis, jour de prélèvement (5, 10 ou 15), paiement mensuel.
-- Utilisée pour la reprise ci-dessous et, par le déclencheur, pour les contrats que crée le code du sprint 1.
CREATE FUNCTION contrat_completer_depuis_dossier() RETURNS TRIGGER AS $$
DECLARE
    v_compte_id    BIGINT;
    v_reference    UUID;
    v_mandat_id    BIGINT;
    v_jour         INTEGER;
BEGIN
    IF NEW.dossier_id IS NULL THEN
        RETURN NEW;
    END IF;
    SELECT ds.compte_id, ds.reference_devis INTO v_compte_id, v_reference
    FROM dossier_souscription ds WHERE ds.id = NEW.dossier_id;

    IF NEW.compte_id IS NULL THEN
        NEW.compte_id := v_compte_id;
    END IF;
    IF NEW.formule IS NULL THEN
        NEW.formule := COALESCE(
            (SELECT t.formule FROM tarif_devis t WHERE t.reference_devis = v_reference),
            (SELECT de.formule FROM devis_enregistre de
             WHERE de.compte_id = v_compte_id AND de.reference = v_reference),
            (SELECT r.formule FROM recommandation_devis r WHERE r.reference = v_reference));
    END IF;
    IF NEW.devis_id IS NULL THEN
        NEW.devis_id := (SELECT de.id FROM devis_enregistre de
                         WHERE de.compte_id = v_compte_id AND de.reference = v_reference);
    END IF;
    SELECT m.id, m.jour_prelevement INTO v_mandat_id, v_jour
    FROM mandat_sepa m WHERE m.dossier_id = NEW.dossier_id AND m.date_signature IS NOT NULL;
    IF NEW.mandat_sepa_id IS NULL THEN
        NEW.mandat_sepa_id := v_mandat_id;
    END IF;
    IF NEW.jour_prelevement IS NULL AND v_jour IN (5, 10, 15) THEN
        NEW.jour_prelevement := v_jour;
    END IF;
    IF NEW.frequence_paiement IS NULL THEN
        NEW.frequence_paiement := 'MENSUEL';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_contrat_completer
    BEFORE INSERT OR UPDATE OF dossier_id ON contrat
    FOR EACH ROW EXECUTE FUNCTION contrat_completer_depuis_dossier();

-- Reprise des contrats du sprint 1 : produit HABITATION (valeur par défaut), le reste par le déclencheur.
UPDATE contrat SET dossier_id = dossier_id;

ALTER TABLE contrat
    ALTER COLUMN compte_id SET NOT NULL,
    ALTER COLUMN formule SET NOT NULL,
    ALTER COLUMN dossier_id DROP NOT NULL,
    ALTER COLUMN adresse_logement DROP NOT NULL,
    ALTER COLUMN code_postal DROP NOT NULL,
    ALTER COLUMN ville DROP NOT NULL,
    ALTER COLUMN type_logement DROP NOT NULL,
    ALTER COLUMN surface DROP NOT NULL,
    ADD CONSTRAINT fk_contrat_compte FOREIGN KEY (compte_id) REFERENCES compte (id),
    ADD CONSTRAINT fk_contrat_mandat_sepa FOREIGN KEY (mandat_sepa_id) REFERENCES mandat_sepa (id),
    ADD CONSTRAINT fk_contrat_vehicule FOREIGN KEY (vehicule_id) REFERENCES vehicule (id),
    ADD CONSTRAINT fk_contrat_conducteur FOREIGN KEY (conducteur_principal_id) REFERENCES conducteur (id),
    ADD CONSTRAINT fk_contrat_devis FOREIGN KEY (devis_id) REFERENCES devis_enregistre (id),
    ADD CONSTRAINT ck_contrat_produit CHECK (produit IN ('HABITATION', 'AUTO')),
    -- Orthographe du projet conservée (EN_ATTENTE_DE_SIGNATURE, écrite par ContratService).
    ADD CONSTRAINT ck_contrat_statut CHECK (statut IN
        ('EN_ATTENTE_DE_SIGNATURE', 'EN_ATTENTE_EFFET', 'ACTIF', 'ACTIF_JUSTIFICATIFS_ATTENDUS', 'AVENANT_EN_ATTENTE',
         'IMPAYE', 'SUSPENDU', 'RESILIATION_EN_COURS', 'RESILIE', 'RENONCE')),
    ADD CONSTRAINT ck_contrat_motif_fin CHECK (motif_fin IN
        ('ECHEANCE', 'LOI_HAMON', 'VENTE_VEHICULE', 'DEMENAGEMENT', 'NON_PAIEMENT', 'RENONCIATION', 'AUTRE')),
    ADD CONSTRAINT ck_contrat_frequence_paiement CHECK (frequence_paiement IN ('MENSUEL', 'ANNUEL')),
    ADD CONSTRAINT ck_contrat_jour_prelevement CHECK (jour_prelevement IN (5, 10, 15)),
    ADD CONSTRAINT ck_contrat_habitation CHECK (produit <> 'HABITATION' OR (
        adresse_logement IS NOT NULL AND code_postal IS NOT NULL AND ville IS NOT NULL
        AND type_logement IS NOT NULL AND surface IS NOT NULL)),
    ADD CONSTRAINT ck_contrat_auto CHECK (produit <> 'AUTO'
        OR (vehicule_id IS NOT NULL AND conducteur_principal_id IS NOT NULL)),
    ADD CONSTRAINT ck_contrat_signe_mandat CHECK (signe_le IS NULL OR mandat_sepa_id IS NOT NULL);

CREATE INDEX idx_contrat_compte ON contrat (compte_id);
CREATE INDEX idx_contrat_statut ON contrat (statut);

CREATE TABLE contrat_option (
    id                BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    contrat_id        BIGINT                   NOT NULL,
    code_option       VARCHAR(30)              NOT NULL,
    prix_mensuel_ttc  NUMERIC(10, 2)           NOT NULL,
    obligatoire       BOOLEAN                  NOT NULL DEFAULT FALSE,
    date_debut        DATE                     NOT NULL,
    date_fin          DATE,
    date_creation     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    date_modification TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_contrat_option_contrat FOREIGN KEY (contrat_id) REFERENCES contrat (id),
    CONSTRAINT ck_contrat_option_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE INDEX idx_contrat_option_contrat ON contrat_option (contrat_id);

CREATE TABLE historique_statut_contrat (
    id                BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    contrat_id        BIGINT                   NOT NULL,
    statut_avant      VARCHAR(30),
    statut_apres      VARCHAR(30)              NOT NULL,
    change_le         TIMESTAMP WITH TIME ZONE NOT NULL,
    motif             VARCHAR(255),
    auteur            VARCHAR(12)              NOT NULL,
    date_creation     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    date_modification TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_historique_statut_contrat FOREIGN KEY (contrat_id) REFERENCES contrat (id),
    CONSTRAINT ck_historique_statut_avant CHECK (statut_avant IN
        ('EN_ATTENTE_DE_SIGNATURE', 'EN_ATTENTE_EFFET', 'ACTIF', 'ACTIF_JUSTIFICATIFS_ATTENDUS', 'AVENANT_EN_ATTENTE',
         'IMPAYE', 'SUSPENDU', 'RESILIATION_EN_COURS', 'RESILIE', 'RENONCE')),
    CONSTRAINT ck_historique_statut_apres CHECK (statut_apres IN
        ('EN_ATTENTE_DE_SIGNATURE', 'EN_ATTENTE_EFFET', 'ACTIF', 'ACTIF_JUSTIFICATIFS_ATTENDUS', 'AVENANT_EN_ATTENTE',
         'IMPAYE', 'SUSPENDU', 'RESILIATION_EN_COURS', 'RESILIE', 'RENONCE')),
    CONSTRAINT ck_historique_statut_auteur CHECK (auteur IN ('CLIENT', 'SYSTEME', 'GESTIONNAIRE'))
);

CREATE INDEX idx_historique_statut_contrat ON historique_statut_contrat (contrat_id);

-- Reprise : statut actuel de chaque contrat du sprint 1 comme premier état connu.
INSERT INTO historique_statut_contrat (contrat_id, statut_avant, statut_apres, change_le, motif, auteur)
SELECT id, NULL, statut, NOW(), 'Reprise sprint 1 (T-05)', 'SYSTEME'
FROM contrat;
