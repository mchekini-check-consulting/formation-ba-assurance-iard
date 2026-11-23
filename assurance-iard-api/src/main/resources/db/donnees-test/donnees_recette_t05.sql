-- T-05 : données de test du sprint 2, en complément des comptes de recette d'US-04 (DonneesRecette).
-- Chargées seulement si DONNEES_TEST_ACTIVES=true (local, dev, qualif ; jamais en preprod ni en prod).
-- Un seul bloc, rejouable : rien n'est recréé si le compte recette.auto existe déjà. Rien n'est supprimé.
-- __MOT_DE_PASSE_HASH__ est remplacé au chargement par l'empreinte du mot de passe de recette commun.
DO $$
DECLARE
    statuts      TEXT[] := ARRAY['EN_ATTENTE_DE_SIGNATURE', 'EN_ATTENTE_EFFET', 'ACTIF', 'ACTIF_JUSTIFICATIFS_ATTENDUS',
                                 'AVENANT_EN_ATTENTE', 'IMPAYE', 'SUSPENDU', 'RESILIATION_EN_COURS', 'RESILIE', 'RENONCE'];
    formules     TEXT[] := ARRAY['TIERS', 'INTER', 'TR'];
    franchises   NUMERIC[] := ARRAY[0, 400, 500];
    etapes       TEXT[] := ARRAY['VEHICULE_RENSEIGNE', 'BESOINS_RENSEIGNES', 'FORMULE_CHOISIE', 'RECAP_VALIDE'];
    aujourd_hui  DATE := CURRENT_DATE;
    v_alice      BIGINT;
    v_contrat    BIGINT;
    v_auto       BIGINT;
    v_mandat     BIGINT;
    v_vehicule   BIGINT;
    v_conducteur BIGINT;
    v_devis      BIGINT;
    v_document   BIGINT;
    v_avenant    BIGINT;
    v_actif      BIGINT;
    v_statut     TEXT;
    v_formule    TEXT;
    v_signe      BOOLEAN;
    i            INTEGER;
BEGIN
    IF EXISTS (SELECT 1 FROM compte WHERE email = 'recette.auto@assureasy.fr') THEN
        RAISE NOTICE 'Données de test T-05 déjà présentes';
        RETURN;
    END IF;

    ---------------------------------------------------------------------------------------------------------------
    -- Client 1 (Alice, compte US-04) : contrat habitation ACTIF, documents V1 REMPLACE / V2 EN_VIGUEUR,
    -- échéances payées et à venir, une facture, un sinistre DECLARE.
    ---------------------------------------------------------------------------------------------------------------
    SELECT c.id, c.compte_id INTO v_contrat, v_alice
    FROM contrat c WHERE c.numero = 'AE-RECETTE-1';

    IF v_contrat IS NOT NULL THEN
        -- La V1 vient de la reprise V27 (base existante) ; sur une base neuve, elle est créée ici.
        UPDATE document SET statut = 'REMPLACE', motif = 'Remplacée par la version 2 (recette T-05)'
        WHERE contrat_id = v_contrat AND type = 'CONDITIONS_PARTICULIERES' AND statut = 'EN_VIGUEUR';
        IF NOT FOUND THEN
            INSERT INTO document (contrat_id, type, version, statut, motif, stocke)
            VALUES (v_contrat, 'CONDITIONS_PARTICULIERES', 1, 'REMPLACE', 'Version initiale (recette T-05)', FALSE);
        END IF;
        INSERT INTO document (contrat_id, type, version, statut, motif, stocke)
        VALUES (v_contrat, 'CONDITIONS_PARTICULIERES', 2, 'EN_VIGUEUR', 'Mise à jour de recette T-05', FALSE);

        -- 18,50 € TTC par mois = 16,97 € HT + 1,53 € de taxe sur les conventions d'assurance.
        INSERT INTO echeance (contrat_id, date_echeance, montant_ht, taxe, montant_ttc, statut)
        SELECT v_contrat, (DATE_TRUNC('month', aujourd_hui) + (n || ' month')::INTERVAL + INTERVAL '9 days')::DATE,
               16.97, 1.53, 18.50, CASE WHEN n < 0 THEN 'PAYEE' ELSE 'A_VENIR' END
        FROM generate_series(-2, 1) AS n;

        INSERT INTO document (contrat_id, type, version, statut, motif, stocke)
        VALUES (v_contrat, 'FACTURE', 1, 'EN_VIGUEUR', 'Facture de recette T-05', FALSE)
        RETURNING id INTO v_document;
        INSERT INTO facture (numero, contrat_id, periode_debut, periode_fin, montant_ht, taxe, montant_ttc,
                             document_id, generee_le)
        VALUES ('FAC-RECETTE-0001', v_contrat, (DATE_TRUNC('month', aujourd_hui) - INTERVAL '1 month')::DATE,
                (DATE_TRUNC('month', aujourd_hui) - INTERVAL '1 day')::DATE, 16.97, 1.53, 18.50, v_document, NOW());

        INSERT INTO sinistre (numero, contrat_id, statut, type, date_survenance, heure_survenance, lieu, description,
                              declarant, details, declaration_honneur, declare_le)
        VALUES ('SIN-RECETTE-0001', v_contrat, 'DECLARE', 'DEGAT_EAUX', aujourd_hui - 10, '08:30', 'Domicile',
                'Fuite du ballon d''eau chaude (recette T-05)', 'SOUSCRIPTEUR',
                '{"piece": "salle de bain", "origine": "fuite"}', TRUE, NOW());
    ELSE
        RAISE NOTICE 'Contrat AE-RECETTE-1 absent : complément du client 1 ignoré';
    END IF;

    ---------------------------------------------------------------------------------------------------------------
    -- Client 2 (nouveau, auto) : un mandat SEPA, un contrat auto par statut (10), 4 devis auto (un par étape),
    -- un sinistre BROUILLON. Il porte aussi le contrat habitation avec un avenant EN_ATTENTE.
    ---------------------------------------------------------------------------------------------------------------
    INSERT INTO compte (email, mot_de_passe_hash, date_creation, profil, civilite, nom, prenom, date_naissance,
                        telephone)
    VALUES ('recette.auto@assureasy.fr', '__MOT_DE_PASSE_HASH__', NOW(), 'ASSURE', 'madame', 'Durand', 'Camille',
            DATE '1988-04-12', '0612345670')
    RETURNING id INTO v_auto;

    INSERT INTO mandat_sepa (compte_id, rum, titulaire, iban_debut, iban_fin, bic, banque, jour_prelevement,
                             date_debut, date_premier_prelevement, date_creation, date_signature, statut)
    VALUES (v_auto, 'AE-RUM-RECETTE-A', 'Camille Durand', 'FR76', '7788', 'AGRIFRPPXXX', 'Banque de recette', 5,
            aujourd_hui - 60, aujourd_hui - 55, NOW(), NOW(), 'ACTIF')
    RETURNING id INTO v_mandat;

    FOR i IN 1 .. array_length(statuts, 1) LOOP
        v_statut := statuts[i];
        v_formule := formules[1 + (i - 1) % 3];
        v_signe := v_statut <> 'EN_ATTENTE_DE_SIGNATURE';

        INSERT INTO vehicule (immatriculation, marque, modele, version, energie, date_mise_circulation,
                              date_acquisition, mode_achat, titulaire_carte_grise, usage, km_annuel, stationnement,
                              valeur_estimee, source)
        VALUES ('AB-' || LPAD(i::TEXT, 3, '0') || '-CD', 'Renault', 'Clio', 'V TCe 90', 'ESSENCE',
                DATE '2021-03-01', DATE '2021-06-15', 'CREDIT', 'SOUSCRIPTEUR', 'TRAJET_TRAVAIL', 12000,
                'PARKING_PRIVE', 11500, 'PLAQUE')
        RETURNING id INTO v_vehicule;

        INSERT INTO conducteur (est_souscripteur, date_naissance, date_permis, coefficient_bonus_malus,
                                annees_assurance, nb_sinistres_3_ans, eligible_en_ligne)
        VALUES (TRUE, DATE '1988-04-12', DATE '2007-05-20', 0.68, 15, 0, TRUE)
        RETURNING id INTO v_conducteur;

        INSERT INTO contrat (numero, produit, compte_id, statut, formule, franchise, cotisation_mensuelle,
                             cotisation_annuelle, resiliation_hamon, date_effet, date_fin, motif_fin,
                             frequence_paiement, jour_prelevement, mandat_sepa_id, vehicule_id,
                             conducteur_principal_id, signe_le, justificatifs_recus, date_creation)
        VALUES ('AE-RECETTE-A' || LPAD(i::TEXT, 2, '0'), 'AUTO', v_auto, v_statut, v_formule,
                franchises[1 + (i - 1) % 3], 30 + 5 * i, (30 + 5 * i) * 12, v_statut = 'ACTIF',
                CASE WHEN v_statut IN ('EN_ATTENTE_DE_SIGNATURE', 'EN_ATTENTE_EFFET') THEN aujourd_hui + 15
                     ELSE aujourd_hui - 60 END,
                CASE v_statut WHEN 'RESILIATION_EN_COURS' THEN aujourd_hui + 30
                              WHEN 'RESILIE' THEN aujourd_hui - 5
                              WHEN 'RENONCE' THEN aujourd_hui - 50 END,
                CASE v_statut WHEN 'RESILIATION_EN_COURS' THEN 'VENTE_VEHICULE'
                              WHEN 'RESILIE' THEN 'ECHEANCE'
                              WHEN 'RENONCE' THEN 'RENONCIATION' END,
                'MENSUEL', 5, CASE WHEN v_signe THEN v_mandat END, v_vehicule, v_conducteur,
                CASE WHEN v_signe THEN NOW() END, v_statut <> 'ACTIF_JUSTIFICATIFS_ATTENDUS', NOW())
        RETURNING id INTO v_contrat;

        INSERT INTO contrat_option (contrat_id, code_option, prix_mensuel_ttc, obligatoire, date_debut)
        SELECT v_contrat, 'RC', 12.00, TRUE, date_effet FROM contrat WHERE id = v_contrat;

        INSERT INTO historique_statut_contrat (contrat_id, statut_avant, statut_apres, change_le, motif, auteur)
        VALUES (v_contrat, NULL, v_statut, NOW(), 'Données de recette T-05', 'SYSTEME');

        INSERT INTO document (contrat_id, type, version, statut, motif, stocke)
        VALUES (v_contrat, 'CONDITIONS_PARTICULIERES', 1, 'EN_VIGUEUR', 'Données de recette T-05', FALSE)
        RETURNING id INTO v_document;

        IF v_statut = 'ACTIF' THEN
            v_actif := v_contrat;
            INSERT INTO preuve_signature (contrat_id, document_id, empreinte_document, email, adresse_ip, signe_le,
                                          nb_essais)
            VALUES (v_contrat, v_document, encode(sha256(convert_to('recette T-05', 'UTF8')), 'hex'),
                    'recette.auto@assureasy.fr', '192.0.2.10', NOW(), 1);
            INSERT INTO declaration_fva (contrat_id, immatriculation, statut, envoye_le, reference_accuse)
            VALUES (v_contrat, 'AB-' || LPAD(i::TEXT, 3, '0') || '-CD', 'DECLAREE', NOW(), 'FVA-RECETTE-0001');
            INSERT INTO resiliation_hamon (contrat_id, ancien_assureur, ancien_numero, statut, envoye_le,
                                           date_fin_ancien, nb_essais)
            VALUES (v_contrat, 'Assureur précédent', 'ANC-123456', 'ENVOYEE', NOW(), aujourd_hui + 30, 1);
        END IF;
    END LOOP;

    INSERT INTO sinistre (contrat_id, statut, type, date_survenance, lieu, description, tiers_implique, declarant)
    VALUES (v_actif, 'BROUILLON', 'ACCIDENT_SANS_TIERS', aujourd_hui - 2, 'Parking du travail',
            'Rétroviseur arraché (brouillon de recette T-05)', FALSE, 'CONDUCTEUR');

    -- 4 devis auto, un à chaque étape.
    FOR i IN 1 .. array_length(etapes, 1) LOOP
        INSERT INTO vehicule (immatriculation, marque, modele, energie, date_mise_circulation, date_acquisition,
                              usage, km_annuel, stationnement, source)
        VALUES ('EF-' || LPAD(i::TEXT, 3, '0') || '-GH', 'Peugeot', '208', 'HYBRIDE', DATE '2023-01-10',
                DATE '2023-02-01', 'PRIVE', 8000, 'GARAGE_FERME', 'PLAQUE')
        RETURNING id INTO v_vehicule;
        INSERT INTO conducteur (est_souscripteur, date_naissance, date_permis, coefficient_bonus_malus,
                                annees_assurance, nb_sinistres_3_ans, eligible_en_ligne)
        VALUES (TRUE, DATE '1988-04-12', DATE '2007-05-20', 0.68, 15, 0, TRUE)
        RETURNING id INTO v_conducteur;

        INSERT INTO devis_enregistre (compte_id, produit, statut_devis, formule_conseillee, formule,
                                      choix_different_conseil, reponses_besoins, options, franchise, prix_mensuel,
                                      date_creation, date_fin_validite, date_dernier_contact)
        VALUES (v_auto, 'AUTO', etapes[i],
                CASE WHEN i >= 2 THEN 'INTER' END,
                CASE WHEN i >= 3 THEN 'TR' END,
                CASE WHEN i >= 3 THEN TRUE END,
                CASE WHEN i >= 2 THEN '{"usage": "PRIVE", "vol": true, "bris_glace": true, "assistance": false,
                    "valeur_vehicule": "MOYENNE", "kilometrage": "FAIBLE", "priorite": "PROTECTION"}'::JSONB END,
                CASE WHEN i >= 3 THEN ARRAY['BRIS_GLACE'] END,
                CASE WHEN i >= 3 THEN '500' END,
                CASE WHEN i >= 3 THEN 54.90 END,
                NOW(), aujourd_hui + 30, NOW())
        RETURNING id INTO v_devis;

        INSERT INTO devis_auto (devis_id, vehicule_id, conducteur_id, assure_plus_un_an, ancien_assureur,
                                ancien_numero_contrat, date_effet_souhaitee)
        VALUES (v_devis, v_vehicule, v_conducteur, TRUE, 'Assureur précédent', 'ANC-65432' || i, aujourd_hui + 15);

        IF i >= 3 THEN
            INSERT INTO devis_option (devis_id, code_option, prix_mensuel_ttc)
            VALUES (v_devis, 'RC', 12.00), (v_devis, 'BRIS_GLACE', 4.50);
        END IF;
    END LOOP;

    -- Contrat habitation avec un avenant EN_ATTENTE (ajout de l'option bris de glace).
    INSERT INTO contrat (numero, produit, compte_id, statut, formule, adresse_logement, code_postal, ville,
                         type_logement, surface, franchise, cotisation_mensuelle, cotisation_annuelle,
                         resiliation_hamon, date_effet, frequence_paiement, jour_prelevement, mandat_sepa_id,
                         signe_le, justificatifs_recus, date_creation)
    VALUES ('AE-RECETTE-H01', 'HABITATION', v_auto, 'AVENANT_EN_ATTENTE', 'confort', '3 rue de la Recette', '33000',
            'Bordeaux', 'appartement', 55, 300, 18.50, 222.00, FALSE, aujourd_hui - 90, 'MENSUEL', 5, v_mandat,
            NOW() - INTERVAL '90 days', TRUE, NOW())
    RETURNING id INTO v_contrat;

    INSERT INTO historique_statut_contrat (contrat_id, statut_avant, statut_apres, change_le, motif, auteur)
    VALUES (v_contrat, NULL, 'ACTIF', NOW() - INTERVAL '90 days', 'Signature (recette T-05)', 'CLIENT'),
           (v_contrat, 'ACTIF', 'AVENANT_EN_ATTENTE', NOW(), 'Demande d''ajout d''une option', 'CLIENT');

    INSERT INTO document (contrat_id, type, version, statut, motif, stocke)
    VALUES (v_contrat, 'AVENANT', 1, 'EN_VIGUEUR', 'Projet d''avenant (recette T-05)', FALSE)
    RETURNING id INTO v_document;

    INSERT INTO avenant (numero, contrat_id, statut, date_effet, prime_avant_ttc, prime_apres_ttc, document_id)
    VALUES (1, v_contrat, 'EN_ATTENTE', aujourd_hui + 1, 18.50, 21.00, v_document)
    RETURNING id INTO v_avenant;

    INSERT INTO avenant_ligne (avenant_id, code_option, action, prix_mensuel_ttc)
    VALUES (v_avenant, 'BRIS_GLACE', 'AJOUT', 2.50);

    ---------------------------------------------------------------------------------------------------------------
    -- Client 4 (nouveau) : un prospect sans aucun contrat.
    ---------------------------------------------------------------------------------------------------------------
    INSERT INTO compte (email, mot_de_passe_hash, date_creation, profil, civilite, nom, prenom, date_naissance,
                        telephone)
    VALUES ('recette.prospect@assureasy.fr', '__MOT_DE_PASSE_HASH__', NOW(), 'PROSPECT', 'monsieur', 'Leroy',
            'David', DATE '1995-09-03', '0612345679');

    RAISE NOTICE 'Données de test T-05 chargées';
END $$;
