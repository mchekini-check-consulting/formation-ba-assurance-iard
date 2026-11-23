# Dictionnaire de données — Assur'Easy

Schéma après le ticket **T-05** (IA-236, sprint 2) : migrations `V1` à `V27` d'`assurance-iard-api`
(base `assurance_iard`) et `V1` à `V5` de `moteurtarifaire` (base `moteurtarifaire`).
Les tableaux des colonnes et des contraintes en fin de document sont **générés depuis la base** après migration :
ils font foi en cas de doute.

## Conventions

| Sujet | Règle |
|---|---|
| Noms | minuscules et tirets bas ; clé primaire `id` (identité) ; clé étrangère `<table>_id` |
| Client | la table s'appelle `compte` (pas de table `client`) : les clés étrangères sont `compte_id` |
| Devis | `devis_enregistre` (« devis » du dictionnaire T-05) ; réponses aux besoins habitation dans `recommandation_devis` |
| Horodatages | `date_creation` et `date_modification` (convention du projet, à la place de `cree_le` / `modifie_le`) sur chaque nouvelle table ; en UTC (`timestamp with time zone`) |
| Montants | `décimal(10,2)`, en euros ; taxe = taxe sur les conventions d'assurance (pas de TVA) ; TTC = HT + taxe contrôlé en base |
| Énumérations | stockées en texte, limitées par une contrainte `CHECK` (pas de type `ENUM` PostgreSQL) |
| Suppression | aucune suppression en cascade vers `contrat` : l'historique est conservé |
| Schéma | uniquement par migrations Flyway ; une migration déjà sur `main` n'est jamais modifiée |

### Colonnes existantes réutilisées (aucun renommage)

| Dictionnaire T-05 | Colonne du projet |
|---|---|
| `contrat.prime_mensuelle_ttc` / `prime_annuelle_ttc` | `contrat.cotisation_mensuelle` / `cotisation_annuelle` |
| `devis.formule_choisie` | `devis_enregistre.formule` |
| `devis.prime_mensuelle_ttc` | `devis_enregistre.prix_mensuel` |
| `devis.date_validite` | `devis_enregistre.date_fin_validite` |
| `devis.statut` | `devis_enregistre.statut_devis` (la colonne `statut` existante décrit l'occupant du logement) |
| `mandat_sepa.signe_le` | `mandat_sepa.date_signature` |
| code de signature à 6 chiffres | `code_signature_contrat` (V10), aucune nouvelle table de code |

## Énumérations

| Énumération | Valeurs | Colonnes |
|---|---|---|
| PRODUIT | HABITATION, AUTO | `contrat.produit`, `devis_enregistre.produit` |
| STATUT_CONTRAT | EN_ATTENTE_DE_SIGNATURE, EN_ATTENTE_EFFET, ACTIF, ACTIF_JUSTIFICATIFS_ATTENDUS, AVENANT_EN_ATTENTE, IMPAYE, SUSPENDU, RESILIATION_EN_COURS, RESILIE, RENONCE | `contrat.statut`, `historique_statut_contrat.statut_avant` / `statut_apres` |
| STATUT_DEVIS | EN_COURS, VEHICULE_RENSEIGNE, BESOINS_RENSEIGNES, FORMULE_CHOISIE, RECAP_VALIDE, TRANSFORME, REFUSE_EN_LIGNE, EXPIRE | `devis_enregistre.statut_devis` |
| MOTIF_FIN | ECHEANCE, LOI_HAMON, VENTE_VEHICULE, DEMENAGEMENT, NON_PAIEMENT, RENONCIATION, AUTRE | `contrat.motif_fin` |
| FREQUENCE_PAIEMENT | MENSUEL, ANNUEL | `contrat.frequence_paiement` |
| ENERGIE | ESSENCE, DIESEL, HYBRIDE, ELECTRIQUE, GPL | `vehicule.energie`, `ref_vehicule.energie` |
| MODE_ACHAT | COMPTANT, CREDIT, LOA, LLD | `vehicule.mode_achat` |
| TITULAIRE | SOUSCRIPTEUR, CONJOINT, ENFANT, SOCIETE_LOCATION, AUTRE_PERSONNE | `vehicule.titulaire_carte_grise` |
| USAGE | PRIVE, TRAJET_TRAVAIL, PROFESSIONNEL | `vehicule.usage` |
| STATIONNEMENT | GARAGE_FERME, PARKING_PRIVE, RUE | `vehicule.stationnement` |
| SOURCE | PLAQUE, MARQUE | `vehicule.source` |
| RESILIATION_ASSUREUR | AUCUNE, NON_PAIEMENT, SINISTRALITE, FAUSSE_DECLARATION | `conducteur.resiliation_assureur` |
| AUTEUR | CLIENT, SYSTEME, GESTIONNAIRE | `historique_statut_contrat.auteur` |
| STATUT_MANDAT | ACTIF, REVOQUE | `mandat_sepa.statut` |
| TYPE_DOCUMENT | CONDITIONS_PARTICULIERES, CONDITIONS_GENERALES, IPID, FICHE_CONSEIL, MANDAT_SEPA, ECHEANCIER, FACTURE, AVENANT, MEMO_VEHICULE, ATTESTATION, DEVIS, DECLARATION_SINISTRE | `document.type` |
| STATUT_DOCUMENT | EN_VIGUEUR, REMPLACE | `document.statut` |
| STATUT_ECHEANCE | A_VENIR, PAYEE, IMPAYEE | `echeance.statut` |
| STATUT_AVENANT | EN_ATTENTE, ACCEPTE, ABANDONNE | `avenant.statut` |
| ACTION_AVENANT | AJOUT, RETRAIT | `avenant_ligne.action` |
| STATUT_FVA | A_ENVOYER, DECLAREE, REJETEE, DIFFEREE | `declaration_fva.statut` |
| STATUT_HAMON | A_ENVOYER, ENVOYEE, ACCEPTEE, INTROUVABLE, ERREUR | `resiliation_hamon.statut` |
| STATUT_SINISTRE | BROUILLON, DECLARE | `sinistre.statut` |
| TYPE_SINISTRE (auto) | ACCIDENT_TIERS, ACCIDENT_SANS_TIERS, VOL, BRIS_GLACE, INCENDIE, CLIMATIQUE_CATNAT, AUTRE | `sinistre.type` (la base accepte l'union auto + habitation ; la cohérence avec le produit est une règle de l'application, US-24) |
| TYPE_SINISTRE (habitation) | DEGAT_EAUX, VOL_CAMBRIOLAGE, INCENDIE, BRIS_GLACE, CLIMATIQUE_CATNAT, RC_TIERS, AUTRE | `sinistre.type` |
| DECLARANT | SOUSCRIPTEUR, CONDUCTEUR, PROCHE | `sinistre.declarant` |
| FORMAT_PIECE | PDF, JPG, PNG | `piece_jointe.format` |
| STATUT_PIECE | STOCKEE, REFUSEE | `piece_jointe.statut` |
| COMPORTEMENT_PLAQUE | TROUVE, INCONNU, ERREUR, DELAI | `ref_plaque_test.comportement` |
| CODE_FORMULE (moteur) | TIERS, INTER, TR | `ref_formule.code` |
| CRITERE_TARIF (moteur) | AGE, BONUS_MALUS, USAGE, VALEUR_VEHICULE | `ref_tarif.critere` |

## Liens entre les tables

```
compte ─┬─< dossier_souscription ─┬─ mandat_sepa (1 par dossier, habitation)
        │                         └─< contrat (habitation du sprint 1)
        ├─< mandat_sepa (compte_id, habitation et auto)
        ├─< devis_enregistre ─┬─ devis_auto ─> vehicule, conducteur
        │                     └─< devis_option
        └─< contrat ─┬─> mandat_sepa, vehicule, conducteur (principal), devis_enregistre
                     ├─< contrat_option, historique_statut_contrat
                     ├─< document (versionné) <─ facture, avenant, preuve_signature
                     ├─< echeance, facture
                     ├─< avenant ─< avenant_ligne
                     ├─< preuve_signature, declaration_fva, resiliation_hamon
                     └─< sinistre ─< piece_jointe ; sinistre ─< document
ref_vehicule ─< ref_plaque_test                       (référentiels, données T-04)
grille_tarifaire ─< ref_formule, ref_option, ref_tarif   (base moteurtarifaire, données T-04)
```

## Règles portées par la base

| Règle | Où |
|---|---|
| Uniques : `contrat.numero`, `facture.numero`, `sinistre.numero`, `mandat_sepa.rum`, `ref_plaque_test.immatriculation` ; `avenant (contrat_id, numero)` ; `devis_auto.devis_id` | contraintes `UNIQUE` |
| Une seule version EN_VIGUEUR par (contrat, type) de document ; un seul avenant EN_ATTENTE par contrat | index uniques partiels `uk_document_en_vigueur`, `uk_avenant_en_attente` |
| Bonus-malus entre 0,50 et 3,50 ; jour de prélèvement 5, 10 ou 15 ; TTC = HT + taxe | `CHECK` |
| Date d'acquisition ≥ mise en circulation et pas après la création de la ligne ; date du sinistre pas après la création ; date d'effet souhaitée entre le lendemain et 90 jours après la création | `CHECK` (la comparaison avec la date du jour est faite par l'application) |
| Immatriculation au format `AA-123-AA` si renseignée | `CHECK` (pas de contrôle de format sur les numéros de contrat, facture, sinistre) |
| Contrat AUTO : véhicule et conducteur principal obligatoires ; contrat HABITATION : adresse, code postal, ville, type de logement et surface obligatoires ; contrat signé (`signe_le`) : mandat obligatoire | `CHECK` sur `contrat` |
| Devis HABITATION : référence, logement, formule, options, franchise et prix obligatoires | `CHECK` sur `devis_enregistre` |
| Devis auto : ancien assureur et ancien numéro obligatoires si assuré depuis plus d'un an | `CHECK` sur `devis_auto` |
| Sinistre DECLARE : déclaration sur l'honneur vraie, numéro et date de déclaration renseignés ; pièce jointe ≤ 5 Mo | `CHECK` |
| Contrat créé par le code du sprint 1 (sans les colonnes T-05) : client, formule, mandat signé, devis, jour de prélèvement et fréquence MENSUEL déduits du dossier | déclencheur `tg_contrat_completer` |
| Mandat créé par le code du sprint 1 : client déduit du dossier | déclencheur `tg_mandat_sepa_compte` |
| `iban_masque` = « •••• » + 4 derniers caractères | colonne calculée (règle US-04) |

## RGPD

- **IBAN** : jamais conservé en entier, ni en clair ni chiffré (`iban_debut`, `iban_fin`, `iban_masque` seulement).
- **Santé** : aucune donnée médicale ; `sinistre.blesses` est un simple booléen.
- **Devis** : `devis_enregistre.date_dernier_contact` sert à la purge à 3 ans (US-12).

## Reprise des données du sprint 1 (V21 à V23, V27)

- Contrats existants : produit HABITATION, **statut inchangé**, client, formule, mandat, devis et jour de prélèvement
  retrouvés à partir du dossier, une ligne d'historique (auteur SYSTEME), conditions particulières en version 1
  EN_VIGUEUR (`stocke` = faux : le PDF n'a jamais été stocké).
- Devis existants : produit HABITATION ; statut TRANSFORME si un contrat existe, EXPIRE si la validité est dépassée,
  sinon EN_COURS ; formule conseillée reprise de la recommandation.
- Mandats existants : client repris du dossier, statut ACTIF.

## Annulation

Un script par migration : `assurance-iard-api/db/annulation/U19__…sql` à `U27__…sql` et
`moteurtarifaire/db/annulation/U5__…sql`, à exécuter **à la main, dans l'ordre inverse** (`psql -f`). Ils ne sont
jamais lancés par Flyway. Chacun retire aussi sa ligne de `flyway_schema_history`, pour pouvoir rejouer la migration.
U21, U22 et U23 s'arrêtent sans rien modifier s'il existe des données qui ne tiennent pas dans le schéma du sprint 1
(contrats ou devis auto, mandats sans dossier, franchise non entière) : les supprimer d'abord, en connaissance de cause.

## Données de test

`assurance-iard-api/src/main/resources/db/donnees-test/donnees_recette_t05.sql`, chargé au démarrage par
`DonneesRecetteSprint2`, juste après les comptes de recette d'US-04 (`DonneesRecette`), seulement si
`DONNEES_TEST_ACTIVES=true` (local, dev, qualif ; jamais en preprod ni en prod). Mot de passe commun : celui des
comptes de recette d'US-04 (`DONNEES_TEST_MOT_DE_PASSE`). Rejouable sans doublon.

| Compte | Contenu |
|---|---|
| `recette.assure1@assureasy.fr` (US-04) | contrat habitation `AE-RECETTE-1` ACTIF : conditions particulières V1 REMPLACE et V2 EN_VIGUEUR, 2 échéances payées et 2 à venir, facture `FAC-RECETTE-0001`, sinistre `SIN-RECETTE-0001` DECLARE |
| `recette.auto@assureasy.fr` (T-05) | 10 contrats auto `AE-RECETTE-A01` à `A10`, un par statut, chacun avec véhicule, conducteur et option RC ; mandat `AE-RUM-RECETTE-A` ; 4 devis auto (VEHICULE_RENSEIGNE, BESOINS_RENSEIGNES, FORMULE_CHOISIE, RECAP_VALIDE) ; contrat habitation `AE-RECETTE-H01` avec un avenant EN_ATTENTE ; un sinistre BROUILLON |
| `recette.prospect@assureasy.fr` (T-05) | prospect sans aucun contrat |
| `recette.assure2`, `recette.gestionnaire`, `recette.admin` (US-04) | inchangés |

## Hypothèses non validées par la PO

Taille maximale des pièces jointes 5 Mo (contrôlée en base) ; format de facture `FAC-AAAA-NNNNNN` (non contrôlé en
base) ; franchises 400 € (Intermédiaire) et 500 € (Tous risques) (données T-04) ; conducteur de 17 ans en conduite
accompagnée non géré.

## Points pour le sprint 2

- **US-18** : « Mes contrats » et le contrôle d'accès retrouvent encore les contrats par le dossier ; les contrats
  sans dossier (auto) n'y apparaissent pas tant qu'ils ne passent pas à `contrat.compte_id`.
- L'entité `DevisEnregistre` ne lit que les devis HABITATION (`@SQLRestriction`) : à lever avec les écrans de devis auto.
- L'entité `Contrat` n'expose pas encore les colonnes T-05 (seule la franchise a changé) : à ajouter avec les
  fonctionnalités qui les utilisent.
- Lien entre les justificatifs d'US-04 (`justificatif`) et la nouvelle table `document` : à faire.
- L'écran `devis-contrat` teste une valeur de statut `SIGNE` que la base n'accepte pas et que le backend n'écrit
  jamais (non corrigé, hors périmètre T-05).

---

*Tableaux ci-dessous générés depuis la base après migration (V27 et V5).*

## Base `assurance_iard` (backend `assurance-iard-api`)

### `acces_refuse`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `date_tentative` | horodatage UTC | oui |  |  |
| `compte_id` | entier |  |  | → compte |
| `email` | texte(254) | oui |  |  |
| `profil` | texte(30) | oui |  |  |
| `methode` | texte(10) | oui |  |  |
| `chemin` | texte(255) | oui |  |  |
| `ressource` | texte(30) | oui |  |  |
| `identifiant` | texte(100) |  |  |  |
| `rubrique` | texte(30) | oui |  |  |
| `droit` | texte(30) | oui |  |  |
| `motif` | texte(30) | oui |  |  |

### `avenant` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `numero` | entier | oui |  |  |
| `contrat_id` | entier | oui |  | → contrat |
| `statut` | texte(10) | oui | 'EN_ATTENTE' |  |
| `date_effet` | date | oui |  |  |
| `prime_avant_ttc` | décimal(10,2) | oui |  |  |
| `prime_apres_ttc` | décimal(10,2) | oui |  |  |
| `accepte_le` | horodatage UTC |  |  |  |
| `accepte_ip` | texte(45) |  |  |  |
| `document_id` | entier |  |  | → document |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `avenant_ligne` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `avenant_id` | entier | oui |  | → avenant |
| `code_option` | texte(30) | oui |  |  |
| `action` | texte(10) | oui |  |  |
| `prix_mensuel_ttc` | décimal(10,2) | oui |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `code_signature_contrat`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | nextval('code_signature_contrat_id_seq') |  |
| `contrat_id` | entier | oui |  | → contrat |
| `code_hash` | texte(255) | oui |  |  |
| `date_expiration` | horodatage UTC | oui |  |  |
| `essais` | entier | oui | 0 |  |

### `code_verification`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `compte_id` | entier | oui |  | → compte |
| `jeton_hash` | texte(64) | oui |  |  |
| `code_hash` | texte(100) | oui |  |  |
| `erreurs` | entier | oui | 0 |  |
| `date_expiration` | horodatage UTC | oui |  |  |

### `compte`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `email` | texte(254) | oui |  |  |
| `mot_de_passe_hash` | texte(100) | oui |  |  |
| `date_creation` | horodatage UTC | oui |  |  |
| `echecs_connexion` | entier | oui | 0 |  |
| `bloque_jusqu_a` | horodatage UTC |  |  |  |
| `civilite` | texte(8) |  |  |  |
| `nom` | texte(100) |  |  |  |
| `prenom` | texte(100) |  |  |  |
| `date_naissance` | date |  |  |  |
| `telephone` | texte(10) |  |  |  |
| `profil` | texte(30) | oui | 'PROSPECT' |  |

### `conducteur` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `est_souscripteur` | booléen | oui | true |  |
| `date_naissance` | date |  |  |  |
| `date_permis` | date |  |  |  |
| `coefficient_bonus_malus` | décimal(3,2) |  |  |  |
| `annees_assurance` | entier |  |  |  |
| `nb_sinistres_3_ans` | entier |  |  |  |
| `resiliation_assureur` | texte(20) | oui | 'AUCUNE' |  |
| `suspension_permis` | booléen | oui | false |  |
| `condamnation_alcool_stupefiants` | booléen | oui | false |  |
| `eligible_en_ligne` | booléen |  |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `contrat`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | nextval('contrat_id_seq') |  |
| `numero` | texte(20) | oui |  |  |
| `dossier_id` | entier |  |  | → dossier_souscription |
| `statut` | texte(30) | oui | 'EN_ATTENTE_DE_SIGNATURE' |  |
| `adresse_logement` | texte(255) |  |  |  |
| `code_postal` | texte(5) |  |  |  |
| `ville` | texte(100) |  |  |  |
| `type_logement` | texte(50) |  |  |  |
| `surface` | entier |  |  |  |
| `franchise` | décimal(10,2) | oui |  |  |
| `cotisation_mensuelle` | décimal(10,2) | oui |  |  |
| `cotisation_annuelle` | décimal(10,2) | oui |  |  |
| `resiliation_hamon` | booléen | oui | false |  |
| `date_effet` | date | oui |  |  |
| `date_creation` | horodatage (sans fuseau) | oui | now() |  |
| `produit` | texte(12) | oui | 'HABITATION' |  |
| `compte_id` | entier | oui |  | → compte |
| `formule` | texte(20) | oui |  |  |
| `date_fin` | date |  |  |  |
| `motif_fin` | texte(20) |  |  |  |
| `frequence_paiement` | texte(10) |  |  |  |
| `jour_prelevement` | entier |  |  |  |
| `mandat_sepa_id` | entier |  |  | → mandat_sepa |
| `vehicule_id` | entier |  |  | → vehicule |
| `conducteur_principal_id` | entier |  |  | → conducteur |
| `devis_id` | entier |  |  | → devis_enregistre |
| `signe_le` | horodatage UTC |  |  |  |
| `justificatifs_recus` | booléen | oui | false |  |
| `date_modification` | horodatage UTC |  |  |  |

### `contrat_option` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `contrat_id` | entier | oui |  | → contrat |
| `code_option` | texte(30) | oui |  |  |
| `prix_mensuel_ttc` | décimal(10,2) | oui |  |  |
| `obligatoire` | booléen | oui | false |  |
| `date_debut` | date | oui |  |  |
| `date_fin` | date |  |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `declaration_fva` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `contrat_id` | entier | oui |  | → contrat |
| `immatriculation` | texte(10) |  |  |  |
| `statut` | texte(10) | oui | 'A_ENVOYER' |  |
| `envoye_le` | horodatage UTC |  |  |  |
| `reference_accuse` | texte(100) |  |  |  |
| `motif_rejet` | texte(255) |  |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `devis_auto` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `devis_id` | entier | oui |  | → devis_enregistre |
| `vehicule_id` | entier |  |  | → vehicule |
| `conducteur_id` | entier |  |  | → conducteur |
| `assure_plus_un_an` | booléen | oui | false |  |
| `ancien_assureur` | texte(100) |  |  |  |
| `ancien_numero_contrat` | texte(40) |  |  |  |
| `date_effet_souhaitee` | date |  |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `devis_enregistre`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `compte_id` | entier | oui |  | → compte |
| `reference` | uuid |  |  | → recommandation_devis |
| `statut` | texte(30) |  |  |  |
| `residence` | texte(12) |  |  |  |
| `type_logement` | texte(12) |  |  |  |
| `adresse` | texte(255) |  |  |  |
| `code_postal` | texte(5) |  |  |  |
| `ville` | texte(100) |  |  |  |
| `surface` | entier |  |  |  |
| `nombre_pieces` | texte(5) |  |  |  |
| `antecedent` | texte(30) |  |  |  |
| `resilie_contrat` | texte(30) |  |  |  |
| `assurance_antecedent` | texte(30) |  |  |  |
| `etage` | texte(30) |  |  |  |
| `dependances` | texte(20) |  |  |  |
| `nombre_colocataires` | texte(5) |  |  |  |
| `assureur_actuel` | texte(100) |  |  |  |
| `nombre_sinistres` | texte(5) |  |  |  |
| `formule` | texte(12) |  |  |  |
| `options` | liste de textes |  |  |  |
| `franchise` | texte(3) |  |  |  |
| `prix_mensuel` | décimal(10,2) |  |  |  |
| `date_creation` | horodatage UTC | oui |  |  |
| `date_fin_validite` | date | oui |  |  |
| `date_dernier_contact` | horodatage UTC | oui |  |  |
| `produit` | texte(12) | oui | 'HABITATION' |  |
| `statut_devis` | texte(20) | oui | 'EN_COURS' |  |
| `formule_conseillee` | texte(20) |  |  |  |
| `choix_different_conseil` | booléen |  |  |  |
| `reponses_besoins` | JSON |  |  |  |
| `date_modification` | horodatage UTC |  |  |  |

### `devis_option` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `devis_id` | entier | oui |  | → devis_enregistre |
| `code_option` | texte(30) | oui |  |  |
| `prix_mensuel_ttc` | décimal(10,2) | oui |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `document` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `contrat_id` | entier |  |  | → contrat |
| `devis_id` | entier |  |  | → devis_enregistre |
| `sinistre_id` | entier |  |  | → sinistre |
| `type` | texte(30) | oui |  |  |
| `version` | entier | oui | 1 |  |
| `statut` | texte(12) | oui | 'EN_VIGUEUR' |  |
| `motif` | texte(100) |  |  |  |
| `cle_stockage` | texte(255) |  |  |  |
| `empreinte` | texte(64) |  |  |  |
| `stocke` | booléen | oui | false |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `dossier_souscription`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `compte_id` | entier | oui |  | → compte |
| `reference_devis` | uuid | oui |  | → recommandation_devis |
| `colocataires` | liste de textes | oui |  |  |
| `date_creation` | horodatage UTC | oui |  |  |

### `echeance` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `contrat_id` | entier | oui |  | → contrat |
| `date_echeance` | date | oui |  |  |
| `montant_ht` | décimal(10,2) | oui |  |  |
| `taxe` | décimal(10,2) | oui |  |  |
| `montant_ttc` | décimal(10,2) | oui |  |  |
| `statut` | texte(10) | oui | 'A_VENIR' |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `facture` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `numero` | texte(20) | oui |  |  |
| `contrat_id` | entier | oui |  | → contrat |
| `periode_debut` | date | oui |  |  |
| `periode_fin` | date | oui |  |  |
| `montant_ht` | décimal(10,2) | oui |  |  |
| `taxe` | décimal(10,2) | oui |  |  |
| `montant_ttc` | décimal(10,2) | oui |  |  |
| `document_id` | entier |  |  | → document |
| `generee_le` | horodatage UTC | oui |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `historique_permission`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `modifie_le` | horodatage UTC | oui |  |  |
| `modifie_par_email` | texte(254) | oui |  |  |
| `profil` | texte(30) | oui |  |  |
| `rubrique` | texte(30) | oui |  |  |
| `droit` | texte(30) | oui |  |  |
| `ancienne_valeur` | booléen | oui |  |  |
| `nouvelle_valeur` | booléen | oui |  |  |

### `historique_statut_contrat` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `contrat_id` | entier | oui |  | → contrat |
| `statut_avant` | texte(30) |  |  |  |
| `statut_apres` | texte(30) | oui |  |  |
| `change_le` | horodatage UTC | oui |  |  |
| `motif` | texte(255) |  |  |  |
| `auteur` | texte(12) | oui |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `justificatif`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `dossier_id` | entier | oui |  | → dossier_souscription |
| `categorie` | texte(10) | oui |  |  |
| `type_document` | texte(20) | oui |  |  |
| `nom_fichier` | texte(255) | oui |  |  |
| `type_fichier` | texte(3) | oui |  |  |
| `taille` | entier | oui |  |  |
| `cle_stockage` | texte(40) | oui |  |  |

### `lien_reinitialisation`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `compte_id` | entier | oui |  | → compte |
| `jeton_hash` | texte(64) | oui |  |  |
| `date_expiration` | horodatage UTC | oui |  |  |

### `mandat_sepa`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `dossier_id` | entier |  |  | → dossier_souscription |
| `rum` | texte(35) | oui |  |  |
| `titulaire` | texte(140) |  |  |  |
| `iban_fin` | texte(4) |  |  |  |
| `jour_prelevement` | entier |  |  |  |
| `date_debut` | date |  |  |  |
| `date_premier_prelevement` | date |  |  |  |
| `date_creation` | horodatage UTC | oui |  |  |
| `date_signature` | horodatage UTC |  |  |  |
| `iban_debut` | texte(4) |  |  |  |
| `compte_id` | entier | oui |  | → compte |
| `iban_masque` | texte(34) |  | calculé :  |  |

### `CASE`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `` |  |  |  |  |

### `    WHEN (iban_fin IS NULL) THEN NULL::text`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `` |  |  |  |  |

### `    ELSE ('•••• '::text `

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| ` (iban_fin)::text)` |  |  |  |  |

### `END`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `` |  |  |  |  |

### `mandat_sepa`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `bic` | texte(11) |  |  |  |
| `banque` | texte(100) |  |  |  |
| `statut` | texte(10) | oui | 'ACTIF' |  |
| `date_modification` | horodatage UTC |  |  |  |

### `permission`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `profil` | texte(30) | oui |  |  |
| `rubrique` | texte(30) | oui |  |  |
| `droit` | texte(30) | oui |  |  |
| `valeur` | booléen | oui |  |  |

### `piece_jointe` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `sinistre_id` | entier | oui |  | → sinistre |
| `type_piece` | texte(30) | oui |  |  |
| `nom_fichier` | texte(255) | oui |  |  |
| `format` | texte(3) | oui |  |  |
| `taille_octets` | entier | oui |  |  |
| `cle_stockage` | texte(255) |  |  |  |
| `statut` | texte(8) | oui | 'STOCKEE' |  |
| `motif_refus` | texte(255) |  |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `preuve_signature` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `contrat_id` | entier | oui |  | → contrat |
| `document_id` | entier |  |  | → document |
| `empreinte_document` | texte(64) | oui |  |  |
| `email` | texte(254) | oui |  |  |
| `adresse_ip` | texte(45) |  |  |  |
| `signe_le` | horodatage UTC | oui |  |  |
| `nb_essais` | entier | oui |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `recommandation_devis`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `reference` | uuid | oui |  |  |
| `valeur_contenu` | entier | oui |  |  |
| `objets_valeur` | booléen | oui |  |  |
| `appareils_couteux` | booléen | oui |  |  |
| `remboursement` | texte(10) | oui |  |  |
| `assistance` | booléen | oui |  |  |
| `litiges` | booléen | oui |  |  |
| `priorite` | texte(12) | oui |  |  |
| `formule` | texte(12) | oui |  |  |
| `raisons` | liste de textes | oui |  |  |
| `options` | liste de textes | oui |  |  |
| `date_creation` | horodatage UTC | oui |  |  |

### `ref_plaque_test` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `immatriculation` | texte(10) | oui |  |  |
| `ref_vehicule_id` | entier |  |  | → ref_vehicule |
| `comportement` | texte(10) | oui |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `ref_vehicule` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `marque` | texte(60) | oui |  |  |
| `modele` | texte(60) | oui |  |  |
| `energie` | texte(12) | oui |  |  |
| `version` | texte(60) |  |  |  |
| `annee` | entier |  |  |  |
| `valeur_neuve` | décimal(10,2) |  |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `resiliation_hamon` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `contrat_id` | entier | oui |  | → contrat |
| `ancien_assureur` | texte(100) | oui |  |  |
| `ancien_numero` | texte(40) | oui |  |  |
| `statut` | texte(12) | oui | 'A_ENVOYER' |  |
| `envoye_le` | horodatage UTC |  |  |  |
| `date_fin_ancien` | date |  |  |  |
| `nb_essais` | entier | oui | 0 |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `session_client`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `compte_id` | entier | oui |  | → compte |
| `jeton_hash` | texte(64) | oui |  |  |
| `date_expiration` | horodatage UTC | oui |  |  |

### `sinistre` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `numero` | texte(20) |  |  |  |
| `contrat_id` | entier | oui |  | → contrat |
| `statut` | texte(10) | oui | 'BROUILLON' |  |
| `type` | texte(20) | oui |  |  |
| `date_survenance` | date | oui |  |  |
| `heure_survenance` | heure |  |  |  |
| `lieu` | texte(255) |  |  |  |
| `description` | text |  |  |  |
| `tiers_implique` | booléen | oui | false |  |
| `tiers_infos` | JSON |  |  |  |
| `blesses` | booléen | oui | false |  |
| `declarant` | texte(12) |  |  |  |
| `declarant_infos` | JSON |  |  |  |
| `details` | JSON |  |  |  |
| `hors_delai` | booléen | oui | false |  |
| `explication_retard` | text |  |  |  |
| `hors_periode_contrat` | booléen | oui | false |  |
| `declaration_honneur` | booléen | oui | false |  |
| `declare_le` | horodatage UTC |  |  |  |
| `brouillon_expire_le` | horodatage UTC | oui | (now() + '30 days') |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `tarif_devis`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `reference_devis` | uuid | oui |  |  |
| `id_tarif` | uuid | oui |  |  |
| `code_postal` | texte(5) | oui |  |  |
| `type_logement` | texte(50) | oui |  |  |
| `surface` | décimal(10,2) | oui |  |  |
| `franchise` | entier |  |  |  |
| `date_creation` | horodatage UTC | oui |  |  |
| `formule` | texte(20) |  |  |  |
| `options` | liste de textes |  |  |  |

### `vehicule` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `immatriculation` | texte(10) |  |  |  |
| `non_immatricule` | booléen | oui | false |  |
| `marque` | texte(60) | oui |  |  |
| `modele` | texte(60) | oui |  |  |
| `version` | texte(60) |  |  |  |
| `energie` | texte(12) | oui |  |  |
| `date_mise_circulation` | date |  |  |  |
| `date_acquisition` | date |  |  |  |
| `mode_achat` | texte(10) |  |  |  |
| `titulaire_carte_grise` | texte(20) |  |  |  |
| `usage` | texte(15) |  |  |  |
| `km_annuel` | entier |  |  |  |
| `stationnement` | texte(15) |  |  |  |
| `valeur_estimee` | décimal(10,2) |  |  |  |
| `source` | texte(10) | oui |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### Contraintes (uniques et contrôles) — Base `assurance_iard` (backend `assurance-iard-api`)

| Table | Contrainte | Nature | Définition |
|---|---|---|---|
| `avenant` | `ck_avenant_numero` | contrôle | `CHECK ((numero >= 1))` |
| `avenant` | `ck_avenant_statut` | contrôle | `CHECK (((statut) IN [EN_ATTENTE, ACCEPTE, ABANDONNE])))` |
| `avenant` | `uk_avenant_en_attente` | index unique partiel | `UNIQUE (contrat_id) WHERE ((statut) = EN_ATTENTE)` |
| `avenant` | `uk_avenant_numero` | unique | `UNIQUE (contrat_id, numero)` |
| `avenant_ligne` | `ck_avenant_ligne_action` | contrôle | `CHECK (((action) IN [AJOUT, RETRAIT])))` |
| `code_signature_contrat` | `uk_code_signature_contrat` | unique | `UNIQUE (contrat_id)` |
| `code_verification` | `uk_code_verification_jeton` | unique | `UNIQUE (jeton_hash)` |
| `compte` | `uk_compte_email` | unique | `UNIQUE (email)` |
| `conducteur` | `ck_conducteur_annees_assurance` | contrôle | `CHECK (((annees_assurance IS NULL) OR (annees_assurance >= 0)))` |
| `conducteur` | `ck_conducteur_bonus_malus` | contrôle | `CHECK (((coefficient_bonus_malus >= 0.50) AND (coefficient_bonus_malus <= 3.50)))` |
| `conducteur` | `ck_conducteur_nb_sinistres` | contrôle | `CHECK (((nb_sinistres_3_ans IS NULL) OR (nb_sinistres_3_ans >= 0)))` |
| `conducteur` | `ck_conducteur_resiliation` | contrôle | `CHECK (((resiliation_assureur) IN [AUCUNE, NON_PAIEMENT, SINISTRALITE, FAUSSE_DECLARATION])))` |
| `contrat` | `ck_contrat_auto` | contrôle | `CHECK ((((produit) <> AUTO) OR ((vehicule_id IS NOT NULL) AND (conducteur_principal_id IS NOT NULL))))` |
| `contrat` | `ck_contrat_frequence_paiement` | contrôle | `CHECK (((frequence_paiement) IN [MENSUEL, ANNUEL])))` |
| `contrat` | `ck_contrat_habitation` | contrôle | `CHECK ((((produit) <> HABITATION) OR ((adresse_logement IS NOT NULL) AND (code_postal IS NOT NULL) AND (ville IS NOT NULL) AND (type_logement IS NOT NULL) AND (surface IS NOT NULL))))` |
| `contrat` | `ck_contrat_jour_prelevement` | contrôle | `CHECK ((jour_prelevement = ANY [5, 10, 15]))` |
| `contrat` | `ck_contrat_motif_fin` | contrôle | `CHECK (((motif_fin) IN [ECHEANCE, LOI_HAMON, VENTE_VEHICULE, DEMENAGEMENT, NON_PAIEMENT, RENONCIATION, AUTRE])))` |
| `contrat` | `ck_contrat_produit` | contrôle | `CHECK (((produit) IN [HABITATION, AUTO])))` |
| `contrat` | `ck_contrat_signe_mandat` | contrôle | `CHECK (((signe_le IS NULL) OR (mandat_sepa_id IS NOT NULL)))` |
| `contrat` | `ck_contrat_statut` | contrôle | `CHECK (((statut) IN [EN_ATTENTE_DE_SIGNATURE, EN_ATTENTE_EFFET, ACTIF, ACTIF_JUSTIFICATIFS_ATTENDUS, AVENANT_EN_ATTENTE, IMPAYE, SUSPENDU, RESILIATION_EN_COURS, RESILIE, RENONCE])))` |
| `contrat` | `contrat_numero_key` | unique | `UNIQUE (numero)` |
| `contrat_option` | `ck_contrat_option_dates` | contrôle | `CHECK (((date_fin IS NULL) OR (date_fin >= date_debut)))` |
| `declaration_fva` | `ck_declaration_fva_statut` | contrôle | `CHECK (((statut) IN [A_ENVOYER, DECLAREE, REJETEE, DIFFEREE])))` |
| `devis_auto` | `ck_devis_auto_ancien_assureur` | contrôle | `CHECK (((NOT assure_plus_un_an) OR ((ancien_assureur IS NOT NULL) AND (ancien_numero_contrat IS NOT NULL))))` |
| `devis_auto` | `ck_devis_auto_date_effet` | contrôle | `CHECK (((date_effet_souhaitee IS NULL) OR ((date_effet_souhaitee >= ((date_creation)::date + 1)) AND (date_effet_souhaitee <= ((date_creation)::date + 90)))))` |
| `devis_auto` | `uk_devis_auto_devis` | unique | `UNIQUE (devis_id)` |
| `devis_enregistre` | `ck_devis_enregistre_habitation` | contrôle | `CHECK ((((produit) <> HABITATION) OR ((reference IS NOT NULL) AND (type_logement IS NOT NULL) AND (adresse IS NOT NULL) AND (code_postal IS NOT NULL) AND (ville IS NOT NULL) AND (surface IS NOT NULL) AND (formule IS NOT NULL) AND (options IS NOT NULL) AND (franchise IS NOT NULL) AND (prix_mensuel IS NOT NULL))))` |
| `devis_enregistre` | `ck_devis_enregistre_produit` | contrôle | `CHECK (((produit) IN [HABITATION, AUTO])))` |
| `devis_enregistre` | `ck_devis_enregistre_statut_devis` | contrôle | `CHECK (((statut_devis) IN [EN_COURS, VEHICULE_RENSEIGNE, BESOINS_RENSEIGNES, FORMULE_CHOISIE, RECAP_VALIDE, TRANSFORME, REFUSE_EN_LIGNE, EXPIRE])))` |
| `devis_enregistre` | `uk_devis_enregistre_compte_reference` | unique | `UNIQUE (compte_id, reference)` |
| `devis_option` | `uk_devis_option` | unique | `UNIQUE (devis_id, code_option)` |
| `document` | `ck_document_statut` | contrôle | `CHECK (((statut) IN [EN_VIGUEUR, REMPLACE])))` |
| `document` | `ck_document_stocke` | contrôle | `CHECK (((NOT stocke) OR (cle_stockage IS NOT NULL)))` |
| `document` | `ck_document_type` | contrôle | `CHECK (((type) IN [CONDITIONS_PARTICULIERES, CONDITIONS_GENERALES, IPID, FICHE_CONSEIL, MANDAT_SEPA, ECHEANCIER, FACTURE, AVENANT, MEMO_VEHICULE, ATTESTATION, DEVIS, DECLARATION_SINISTRE])))` |
| `document` | `ck_document_version` | contrôle | `CHECK ((version >= 1))` |
| `document` | `uk_document_en_vigueur` | index unique partiel | `UNIQUE (contrat_id, type) WHERE ((statut) = EN_VIGUEUR)` |
| `dossier_souscription` | `uk_dossier_souscription_devis` | unique | `UNIQUE (compte_id, reference_devis)` |
| `echeance` | `ck_echeance_montant_ttc` | contrôle | `CHECK ((montant_ttc = (montant_ht + taxe)))` |
| `echeance` | `ck_echeance_statut` | contrôle | `CHECK (((statut) IN [A_VENIR, PAYEE, IMPAYEE])))` |
| `facture` | `ck_facture_montant_ttc` | contrôle | `CHECK ((montant_ttc = (montant_ht + taxe)))` |
| `facture` | `ck_facture_periode` | contrôle | `CHECK ((periode_fin >= periode_debut))` |
| `facture` | `uk_facture_numero` | unique | `UNIQUE (numero)` |
| `historique_statut_contrat` | `ck_historique_statut_apres` | contrôle | `CHECK (((statut_apres) IN [EN_ATTENTE_DE_SIGNATURE, EN_ATTENTE_EFFET, ACTIF, ACTIF_JUSTIFICATIFS_ATTENDUS, AVENANT_EN_ATTENTE, IMPAYE, SUSPENDU, RESILIATION_EN_COURS, RESILIE, RENONCE])))` |
| `historique_statut_contrat` | `ck_historique_statut_auteur` | contrôle | `CHECK (((auteur) IN [CLIENT, SYSTEME, GESTIONNAIRE])))` |
| `historique_statut_contrat` | `ck_historique_statut_avant` | contrôle | `CHECK (((statut_avant) IN [EN_ATTENTE_DE_SIGNATURE, EN_ATTENTE_EFFET, ACTIF, ACTIF_JUSTIFICATIFS_ATTENDUS, AVENANT_EN_ATTENTE, IMPAYE, SUSPENDU, RESILIATION_EN_COURS, RESILIE, RENONCE])))` |
| `justificatif` | `uk_justificatif_categorie` | unique | `UNIQUE (dossier_id, categorie)` |
| `lien_reinitialisation` | `uk_lien_reinitialisation_jeton` | unique | `UNIQUE (jeton_hash)` |
| `mandat_sepa` | `ck_mandat_sepa_statut` | contrôle | `CHECK (((statut) IN [ACTIF, REVOQUE])))` |
| `mandat_sepa` | `uk_mandat_sepa_dossier` | unique | `UNIQUE (dossier_id)` |
| `mandat_sepa` | `uk_mandat_sepa_rum` | unique | `UNIQUE (rum)` |
| `permission` | `uk_permission` | unique | `UNIQUE (profil, rubrique, droit)` |
| `piece_jointe` | `ck_piece_jointe_format` | contrôle | `CHECK (((format) IN [PDF, JPG, PNG])))` |
| `piece_jointe` | `ck_piece_jointe_statut` | contrôle | `CHECK (((statut) IN [STOCKEE, REFUSEE])))` |
| `piece_jointe` | `ck_piece_jointe_taille` | contrôle | `CHECK (((taille_octets >= 1) AND (taille_octets <= 5242880)))` |
| `preuve_signature` | `ck_preuve_signature_nb_essais` | contrôle | `CHECK (((nb_essais >= 1) AND (nb_essais <= 3)))` |
| `recommandation_devis` | `uk_recommandation_devis_reference` | unique | `UNIQUE (reference)` |
| `ref_plaque_test` | `ck_ref_plaque_test_comportement` | contrôle | `CHECK (((comportement) IN [TROUVE, INCONNU, ERREUR, DELAI])))` |
| `ref_plaque_test` | `uk_ref_plaque_test_immatriculation` | unique | `UNIQUE (immatriculation)` |
| `ref_vehicule` | `ck_ref_vehicule_energie` | contrôle | `CHECK (((energie) IN [ESSENCE, DIESEL, HYBRIDE, ELECTRIQUE, GPL])))` |
| `resiliation_hamon` | `ck_resiliation_hamon_nb_essais` | contrôle | `CHECK ((nb_essais >= 0))` |
| `resiliation_hamon` | `ck_resiliation_hamon_statut` | contrôle | `CHECK (((statut) IN [A_ENVOYER, ENVOYEE, ACCEPTEE, INTROUVABLE, ERREUR])))` |
| `session_client` | `uk_session_client_jeton` | unique | `UNIQUE (jeton_hash)` |
| `sinistre` | `ck_sinistre_date_survenance` | contrôle | `CHECK ((date_survenance <= (date_creation)::date))` |
| `sinistre` | `ck_sinistre_declarant` | contrôle | `CHECK (((declarant) IN [SOUSCRIPTEUR, CONDUCTEUR, PROCHE])))` |
| `sinistre` | `ck_sinistre_declare` | contrôle | `CHECK ((((statut) <> DECLARE) OR (declaration_honneur AND (numero IS NOT NULL) AND (declare_le IS NOT NULL))))` |
| `sinistre` | `ck_sinistre_statut` | contrôle | `CHECK (((statut) IN [BROUILLON, DECLARE])))` |
| `sinistre` | `ck_sinistre_type` | contrôle | `CHECK (((type) IN [ACCIDENT_TIERS, ACCIDENT_SANS_TIERS, VOL, BRIS_GLACE, INCENDIE, CLIMATIQUE_CATNAT, AUTRE, DEGAT_EAUX, VOL_CAMBRIOLAGE, RC_TIERS])))` |
| `sinistre` | `uk_sinistre_numero` | unique | `UNIQUE (numero)` |
| `tarif_devis` | `uk_tarif_devis_reference` | unique | `UNIQUE (reference_devis)` |
| `vehicule` | `ck_vehicule_date_acquisition` | contrôle | `CHECK (((date_acquisition IS NULL) OR (((date_mise_circulation IS NULL) OR (date_acquisition >= date_mise_circulation)) AND (date_acquisition <= (date_creation)::date))))` |
| `vehicule` | `ck_vehicule_energie` | contrôle | `CHECK (((energie) IN [ESSENCE, DIESEL, HYBRIDE, ELECTRIQUE, GPL])))` |
| `vehicule` | `ck_vehicule_immatriculation` | contrôle | `CHECK (((immatriculation IS NULL) OR ((immatriculation) ~ '^[A-Z]{2}-[0-9]{3}-[A-Z]{2}$')))` |
| `vehicule` | `ck_vehicule_km_annuel` | contrôle | `CHECK (((km_annuel IS NULL) OR (km_annuel >= 0)))` |
| `vehicule` | `ck_vehicule_mode_achat` | contrôle | `CHECK (((mode_achat) IN [COMPTANT, CREDIT, LOA, LLD])))` |
| `vehicule` | `ck_vehicule_source` | contrôle | `CHECK (((source) IN [PLAQUE, MARQUE])))` |
| `vehicule` | `ck_vehicule_stationnement` | contrôle | `CHECK (((stationnement) IN [GARAGE_FERME, PARKING_PRIVE, RUE])))` |
| `vehicule` | `ck_vehicule_titulaire` | contrôle | `CHECK (((titulaire_carte_grise) IN [SOUSCRIPTEUR, CONJOINT, ENFANT, SOCIETE_LOCATION, AUTRE_PERSONNE])))` |
| `vehicule` | `ck_vehicule_usage` | contrôle | `CHECK (((usage) IN [PRIVE, TRAJET_TRAVAIL, PROFESSIONNEL])))` |

## Base `moteurtarifaire` (backend `moteurtarifaire`)

### `calcul_tarif`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `id_tarif` | uuid | oui |  |  |
| `version_grille` | texte(50) | oui |  |  |
| `tarifable` | booléen | oui |  |  |
| `donnees_recues` | JSON | oui |  |  |
| `resultat` | JSON | oui |  |  |
| `date_calcul` | horodatage UTC | oui |  |  |
| `date_validite` | date |  |  |  |

### `grille_tarifaire`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `version` | texte(50) | oui |  |  |
| `date_debut` | date | oui |  |  |
| `date_fin` | date |  |  |  |

### `parametre_tarifaire`

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `grille_id` | entier | oui |  | → grille_tarifaire |
| `categorie` | texte(50) | oui |  |  |
| `cle` | texte(50) | oui |  |  |
| `valeur` | texte(255) | oui |  |  |

### `ref_formule` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `grille_id` | entier | oui |  | → grille_tarifaire |
| `code` | texte(10) | oui |  |  |
| `libelle` | texte(60) | oui |  |  |
| `franchise` | décimal(10,2) |  |  |  |
| `prix_mensuel_base` | décimal(10,2) | oui |  |  |
| `ordre` | entier | oui |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `ref_option` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `grille_id` | entier | oui |  | → grille_tarifaire |
| `code` | texte(30) | oui |  |  |
| `libelle` | texte(100) | oui |  |  |
| `prix_mensuel` | décimal(10,2) | oui |  |  |
| `produit` | texte(12) | oui |  |  |
| `obligatoire` | booléen | oui | false |  |
| `formules_compatibles` | liste de textes | oui | '{}'[] |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `ref_tarif` *(nouvelle, T-05)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `grille_id` | entier | oui |  | → grille_tarifaire |
| `critere` | texte(20) | oui |  |  |
| `borne_min` | décimal(10,2) |  |  |  |
| `borne_max` | décimal(10,2) |  |  |  |
| `valeur` | texte(30) |  |  |  |
| `coefficient` | décimal(6,3) | oui |  |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### Contraintes (uniques et contrôles) — Base `moteurtarifaire` (backend `moteurtarifaire`)

| Table | Contrainte | Nature | Définition |
|---|---|---|---|
| `calcul_tarif` | `calcul_tarif_id_tarif_key` | unique | `UNIQUE (id_tarif)` |
| `grille_tarifaire` | `grille_tarifaire_dates` | contrôle | `CHECK (((date_fin IS NULL) OR (date_fin >= date_debut)))` |
| `grille_tarifaire` | `grille_tarifaire_version_key` | unique | `UNIQUE (version)` |
| `parametre_tarifaire` | `parametre_tarifaire_unique` | unique | `UNIQUE (grille_id, categorie, cle)` |
| `ref_formule` | `ref_formule_code` | contrôle | `CHECK (((code) IN [TIERS, INTER, TR])))` |
| `ref_formule` | `ref_formule_unique` | unique | `UNIQUE (grille_id, code)` |
| `ref_option` | `ref_option_produit` | contrôle | `CHECK (((produit) IN [HABITATION, AUTO])))` |
| `ref_option` | `ref_option_unique` | unique | `UNIQUE (grille_id, code)` |
| `ref_tarif` | `ref_tarif_bornes` | contrôle | `CHECK (((borne_min IS NULL) OR (borne_max IS NULL) OR (borne_max >= borne_min)))` |
| `ref_tarif` | `ref_tarif_coefficient` | contrôle | `CHECK ((coefficient > (0)))` |
| `ref_tarif` | `ref_tarif_critere` | contrôle | `CHECK (((critere) IN [AGE, BONUS_MALUS, USAGE, VALEUR_VEHICULE])))` |
