# Dictionnaire de données — Assur'Easy

Schéma après les tickets **T-05** (IA-236, sprint 2) et **T-03** (IA-238, documents PDF : `V28`) : migrations `V1` à `V28`
(dont le correctif `V22.1`) d'`assurance-iard-api`
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
                     ├─< document (versionné) <─ facture, avenant, preuve_signature ; document ── document_contenu (PDF, T-03)
                     ├─< echeance, facture
                     ├─< avenant ─< avenant_ligne
                     ├─< preuve_signature, declaration_fva, resiliation_hamon
                     └─< sinistre ─< piece_jointe ; sinistre ─< document
document sans contrat : conditions générales (reference = HABITATION / AUTO), IPID (reference = formule)
ref_vehicule ─< ref_plaque_test                       (référentiels, données T-04)
grille_tarifaire ─< ref_formule, ref_option, ref_tarif   (base moteurtarifaire, données T-04)
```

## Règles portées par la base

| Règle | Où |
|---|---|
| Uniques : `contrat.numero`, `facture.numero`, `sinistre.numero`, `mandat_sepa.rum`, `ref_plaque_test.immatriculation` ; `avenant (contrat_id, numero)` ; `devis_auto.devis_id` | contraintes `UNIQUE` |
| Une seule version EN_VIGUEUR par (contrat, type, référence) de document (T-03 : plusieurs factures et avenants par contrat ; une version en vigueur des conditions générales par produit) ; les documents sans contrat ni référence (sinistre, devis) ne sont pas concernés ; un seul avenant EN_ATTENTE par contrat | index uniques partiels `uk_document_en_vigueur`, `uk_avenant_en_attente` |
| PDF d'un document : un seul par document, entre 1 octet et 5 Mo, taille annoncée = taille réelle | `uk_document_contenu_document`, `CHECK` sur `document_contenu` |
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
- **Documents PDF (T-03)** : l'IBAN n'y figure que masqué (« •••• 4821 ») ; aucune donnée de santé ; les journaux ne
  gardent que le type, l'identifiant technique du contrat et la version.

## Reprise des données du sprint 1 (V21 à V23, V27)

- **V22.1 — correction de données invalides** (et non changement de statut des contrats existants) : la valeur
  `SIGNE`, écrite par une ancienne branche jamais fusionnée (`feature/moteurtarifaire`), devient `ACTIF` si la date
  d'effet est aujourd'hui ou passée, `EN_ATTENTE_EFFET` sinon. Tout autre statut hors des 10 valeurs autorisées
  arrête la migration avec la liste des valeurs trouvées, sans rien modifier.
- Contrats existants : produit HABITATION, **statut inchangé**, client, formule, mandat, devis et jour de prélèvement
  retrouvés à partir du dossier, une ligne d'historique (auteur SYSTEME), conditions particulières en version 1
  EN_VIGUEUR (`stocke` = faux : le PDF n'a jamais été stocké).
- Devis existants : produit HABITATION ; statut TRANSFORME si un contrat existe, EXPIRE si la validité est dépassée,
  sinon EN_COURS ; formule conseillée reprise de la recommandation.
- Mandats existants : client repris du dossier, statut ACTIF.
- **T-03** : V28 ne modifie aucune ligne existante (`reference` vide). Les PDF des conditions particulières EN_VIGUEUR
  non stockées (V27, données de recette) sont produits par la tâche de reprise de l'application (toutes les 5 minutes),
  à leur version actuelle ; une version REMPLACE jamais stockée n'est pas reconstituée.

## Annulation

Un script par migration : `assurance-iard-api/db/annulation/U19__…sql` à `U28__…sql` et
`moteurtarifaire/db/annulation/U5__…sql`, à exécuter **à la main, dans l'ordre inverse** (`psql -f`). Ils ne sont
jamais lancés par Flyway. Chacun retire aussi sa ligne de `flyway_schema_history`, pour pouvoir rejouer la migration.
`U22_1__…sql` se place entre U23 et U22 ; il ne retire que sa ligne de `flyway_schema_history` (la correction des
statuts `SIGNE` n'est pas défaite : l'ancienne valeur, invalide, n'est conservée nulle part).
U21, U22 et U23 s'arrêtent sans rien modifier s'il existe des données qui ne tiennent pas dans le schéma du sprint 1
(contrats ou devis auto, mandats sans dossier, franchise non entière) : les supprimer d'abord, en connaissance de cause.
U28 supprime les PDF stockés en base (les documents concernés repassent à `stocke` = faux) et les conditions
générales provisoires ; il s'arrête sans rien modifier si un contrat a plusieurs documents EN_VIGUEUR du même type
(plusieurs factures ou avenants).

## Données de test

`assurance-iard-api/src/main/resources/db/donnees-test/donnees_recette_t05.sql`, chargé au démarrage par
`DonneesRecetteSprint2`, juste après les comptes de recette d'US-04 (`DonneesRecette`), seulement si
`DONNEES_TEST_ACTIVES=true` (local, dev, qualif ; jamais en preprod ni en prod). Mot de passe commun : celui des
comptes de recette d'US-04 (`DONNEES_TEST_MOT_DE_PASSE`). Rejouable sans doublon.

| Compte | Contenu |
|---|---|
| `recette.assure1@assureasy.fr` (US-04) | contrat habitation `AE-RECETTE-1` ACTIF : conditions particulières V1 REMPLACE et V2 EN_VIGUEUR, 2 échéances payées et 2 à venir, facture `FAC-RECETTE-0001`, sinistre `SIN-RECETTE-0001` DECLARE. US-22 : effet un an avant le 26 du mois courant, 3 échéances payées et 1 à venir |
| `recette.auto@assureasy.fr` (T-05) | 10 contrats auto `AE-RECETTE-A01` à `A10`, un par statut, chacun avec véhicule, conducteur et option RC ; mandat `AE-RUM-RECETTE-A` ; 4 devis auto (VEHICULE_RENSEIGNE, BESOINS_RENSEIGNES, FORMULE_CHOISIE, RECAP_VALIDE) ; contrat habitation `AE-RECETTE-H01` avec un avenant EN_ATTENTE ; un sinistre BROUILLON |
| `recette.prospect@assureasy.fr` (T-05) | prospect sans aucun contrat |
| `recette.assure2`, `recette.gestionnaire`, `recette.admin` (US-04) | inchangés |

## Hypothèses non validées par la PO

Taille maximale des pièces jointes 5 Mo (contrôlée en base) ; format de facture `FAC-AAAA-NNNNNN` (non contrôlé en
base) ; franchises 400 € (Intermédiaire) et 500 € (Tous risques) (données T-04) ; conducteur de 17 ans en conduite
accompagnée non géré. T-03 : taille maximale d'un PDF 5 Mo ; mentions de l'assureur, garanties par formule,
exclusions et conditions générales **provisoires et fictives** (`application.yaml`, `assurance.documents`), à
remplacer par les éléments du PO. US-22 : taux de TSCA provisoires (`assurance.facturation` : habitation 9 %, auto
18 %, protection juridique 13,4 %, assistance 9 %).

## Points pour le sprint 2

- ~~**US-18** : « Mes contrats » et le contrôle d'accès retrouvent encore les contrats par le dossier.~~ Fait en
  US-18 : contrats rattachés au client par `contrat.compte_id` (Mes contrats, tableau de bord, contrôle d'accès,
  habitation et auto). Aucune migration : seule la lecture par le code a changé.
- ~~L'entité `DevisEnregistre` ne lit que les devis HABITATION (`@SQLRestriction`).~~ Filtre levé en US-18 : l'entité
  lit les deux produits (champs logement, formule et prix facultatifs côté code). Mes devis, la reprise, la
  suppression, le PDF et la purge RGPD restent limités à l'habitation par leurs requêtes
  (`DevisEnregistreRepository`). Le tableau de bord affiche les devis en cours des deux produits.
- **Purge RGPD des devis auto** : pas encore faite (lignes liées `devis_auto`, `devis_option`) ; à traiter avec les
  écrans de devis auto (AC4 auto d'US-18 reporté au backlog).
- L'entité `Contrat` lit en US-18 les colonnes T-05 `compte_id`, `produit`, `formule`, `date_fin`, `vehicule_id`,
  **en lecture seule** : la souscription habitation du sprint 1 enregistre ses contrats comme avant, et le
  déclencheur `tg_contrat_completer` les complète. À rendre modifiables avec la souscription auto (US-33).
- Lien entre les justificatifs d'US-04 (`justificatif`) et la nouvelle table `document` : à faire.
- ~~**T-03** : les contrats créés après V27 n'ont pas de ligne « conditions particulières ».~~ US-21 : la signature
  habitation crée la V1 (motif « Signature du contrat ») ; la régénération crée la version suivante (motif
  « Régénération à la demande du client »), la précédente passe à REMPLACE. Aucune migration. Reste à faire : la
  signature auto (US-33). Les contrats habitation signés entre V27 et US-21 obtiennent leur V1 à la première
  régénération.
- ~~L'écran `devis-contrat` teste une valeur de statut `SIGNE`.~~ Retiré en US-18 (10 statuts de RG3).
- **Données de test** : le contrat auto `AE-RECETTE-A10` est au statut `RENONCE`, alors que la renonciation ne
  s'applique pas à l'assurance auto obligatoire (art. L112-2-1 du Code des assurances). Affiché tel quel par US-18,
  à corriger dans le jeu de données de test (à valider PO).
- ~~**Factures** : aucun modèle ne produit encore le PDF d'une facture.~~ US-22 : modèle « Facture » (type `FACTURE`,
  `document.reference` = numéro de facture ; la facture de test `FAC-RECETTE-0001`, sans référence, est retrouvée
  par `facture.document_id` et stockée par la reprise de T-03). Aucune migration.
- **US-22, échéancier** : aucun traitement ne crée encore les lignes `echeance`. Les échéances enregistrées (données de
  test) priment ; sinon l'échéancier est calculé depuis le contrat (premier prélèvement de RG3/RG4 d'US-14, puis
  chaque mois, ou chaque année en paiement annuel, jusqu'à la fin de l'année d'assurance en cours). Statut « Payée »
  si la date est passée, « À venir » sinon : simulation de prototype, **à valider PO** (pas de système de paiement).
  Recette : les mois manquants des années d'assurance des échéances enregistrées sont complétés par le calcul (jour
  de prélèvement du contrat) : une année de cotisation vaut ses 12 mensualités. Aucune échéance avant la date d'effet
  (une échéance enregistrée avant est ignorée) : l'échéancier et les années commencent à la date d'effet.
- **US-22, données de test d'Alice** (`donnees_recette_us22.sql`, lancé après T-05, `DONNEES_TEST_ACTIVES` seulement ;
  les scripts déjà sur `main` ne sont pas modifiés) : date d'effet de `AE-RECETTE-1` un an plus tôt, le 26 du mois
  courant au chargement (ex. 26/11/2025), et échéance du 10 de ce mois passée à PAYEE. L'année d'assurance qui se
  termine le 25 est entièrement réglée (facture annuelle testable) ; `FAC-RECETTE-0001` couvre toujours l'échéance du
  10 du mois précédent. Pas de migration : ce sont des données de test, jamais chargées en preprod ni en prod.
- **US-22, recette** : facture d'une échéance seulement si elle est payée (l'avis d'échéance des échéances à venir
  viendra avec IA-245) ; facture annuelle seulement quand toutes les échéances de l'année d'assurance sont payées (elle
  vaut alors la cotisation annuelle). Une facture d'au plus un mois qui couvre la date d'une échéance (ex.
  `FAC-RECETTE-0001`) en tient lieu : pas de seconde facture. Une facture annuelle ne couvre pas une échéance mensuelle.
- **US-22, factures** : une facture par contrat et par période (`periode_debut`, `periode_fin`), contrôlée par le code
  (verrou sur le contrat) et non par la base ; numéro `FAC-AAAA-NNNNNN` = année de génération + compteur de la table
  `facture` (jamais réutilisé, mais pas remis à zéro chaque année et sans garantie d'absence de trou). HT et TSCA
  calculés depuis le TTC, par garantie, avec les taux **provisoires** de `assurance.facturation` (`application.yaml`).

---

*Tableaux ci-dessous générés depuis la base après migration (V27 et V5), complétés à la main pour V28 (T-03).*

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
| `reference` *(T-03)* | texte(40) |  |  | n° de facture ou d'avenant, produit (conditions générales), formule (IPID) |
| `version` | entier | oui | 1 |  |
| `statut` | texte(12) | oui | 'EN_VIGUEUR' |  |
| `motif` | texte(100) |  |  |  |
| `cle_stockage` | texte(255) |  |  | T-03 : `base:document_contenu/<id du document>` |
| `empreinte` | texte(64) |  |  | T-03 : SHA-256 du PDF, vérifiée au téléchargement |
| `stocke` | booléen | oui | false |  |
| `date_creation` | horodatage UTC | oui | now() |  |
| `date_modification` | horodatage UTC | oui | now() |  |

### `document_contenu` *(nouvelle, T-03)*

| Colonne | Type | Obligatoire | Défaut / calcul | Lien |
|---|---|---|---|---|
| `id` | entier | oui | clé (auto) |  |
| `document_id` | entier | oui |  | → document (un contenu par document) |
| `contenu` | binaire (bytea) | oui |  | le PDF |
| `taille_octets` | entier | oui |  | 1 octet à 5 Mo |
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
| `iban_masque` | texte(34) |  | calculé : `CASE WHEN iban_fin IS NULL THEN NULL ELSE '•••• ' \|\| iban_fin END` (4 derniers caractères seulement) |  |
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

| Table | Contrainte | Nature | Règle en français | Définition SQL |
|---|---|---|---|---|
| `avenant` | `ck_avenant_numero` | contrôle | Le numéro d'avenant commence à 1. | `CHECK ((numero >= 1))` |
| `avenant` | `ck_avenant_statut` | contrôle | Statut : EN_ATTENTE, ACCEPTE ou ABANDONNE. | `CHECK (((statut) = ANY ((ARRAY['EN_ATTENTE', 'ACCEPTE', 'ABANDONNE']))))` |
| `avenant` | `uk_avenant_en_attente` | index unique partiel | Un seul avenant EN_ATTENTE par contrat. | `UNIQUE (contrat_id) WHERE ((statut) = 'EN_ATTENTE')` |
| `avenant` | `uk_avenant_numero` | unique | Un numéro d'avenant n'est utilisé qu'une fois par contrat. | `UNIQUE (contrat_id, numero)` |
| `avenant_ligne` | `ck_avenant_ligne_action` | contrôle | Action : AJOUT ou RETRAIT. | `CHECK (((action) = ANY ((ARRAY['AJOUT', 'RETRAIT']))))` |
| `code_signature_contrat` | `uk_code_signature_contrat` | unique | Pas de doublon sur : contrat_id. | `UNIQUE (contrat_id)` |
| `code_verification` | `uk_code_verification_jeton` | unique | Pas de doublon sur : jeton_hash. | `UNIQUE (jeton_hash)` |
| `compte` | `uk_compte_email` | unique | Pas de doublon sur : email. | `UNIQUE (email)` |
| `conducteur` | `ck_conducteur_annees_assurance` | contrôle | Le nombre d'années d'assurance n'est jamais négatif. | `CHECK (((annees_assurance IS NULL) OR (annees_assurance >= 0)))` |
| `conducteur` | `ck_conducteur_bonus_malus` | contrôle | Le coefficient bonus-malus est compris entre 0,50 et 3,50. | `CHECK (((coefficient_bonus_malus >= 0.50) AND (coefficient_bonus_malus <= 3.50)))` |
| `conducteur` | `ck_conducteur_nb_sinistres` | contrôle | Le nombre de sinistres sur 3 ans n'est jamais négatif. | `CHECK (((nb_sinistres_3_ans IS NULL) OR (nb_sinistres_3_ans >= 0)))` |
| `conducteur` | `ck_conducteur_resiliation` | contrôle | Résiliation par l'assureur : AUCUNE, NON_PAIEMENT, SINISTRALITE ou FAUSSE_DECLARATION. | `CHECK (((resiliation_assureur) = ANY ((ARRAY['AUCUNE', 'NON_PAIEMENT', 'SINISTRALITE', 'FAUSSE_DECLARATION']))))` |
| `contrat` | `ck_contrat_auto` | contrôle | Un contrat AUTO a un véhicule et un conducteur principal. | `CHECK ((((produit) <> 'AUTO') OR ((vehicule_id IS NOT NULL) AND (conducteur_principal_id IS NOT NULL))))` |
| `contrat` | `ck_contrat_frequence_paiement` | contrôle | Fréquence de paiement : MENSUEL ou ANNUEL. | `CHECK (((frequence_paiement) = ANY ((ARRAY['MENSUEL', 'ANNUEL']))))` |
| `contrat` | `ck_contrat_habitation` | contrôle | Un contrat HABITATION a une adresse, un code postal, une ville, un type de logement et une surface. | `CHECK ((((produit) <> 'HABITATION') OR ((adresse_logement IS NOT NULL) AND (code_postal IS NOT NULL) AND (ville IS NOT NULL) AND (type_logement IS NOT NULL) AND (surface IS NOT NULL))))` |
| `contrat` | `ck_contrat_jour_prelevement` | contrôle | Le jour de prélèvement est le 5, le 10 ou le 15. | `CHECK ((jour_prelevement = ANY (ARRAY[5, 10, 15])))` |
| `contrat` | `ck_contrat_motif_fin` | contrôle | Motif de fin : ECHEANCE, LOI_HAMON, VENTE_VEHICULE, DEMENAGEMENT, NON_PAIEMENT, RENONCIATION ou AUTRE. | `CHECK (((motif_fin) = ANY ((ARRAY['ECHEANCE', 'LOI_HAMON', 'VENTE_VEHICULE', 'DEMENAGEMENT', 'NON_PAIEMENT', 'RENONCIATION', 'AUTRE']))))` |
| `contrat` | `ck_contrat_produit` | contrôle | Produit : HABITATION ou AUTO. | `CHECK (((produit) = ANY ((ARRAY['HABITATION', 'AUTO']))))` |
| `contrat` | `ck_contrat_signe_mandat` | contrôle | Un contrat signé a un mandat SEPA. | `CHECK (((signe_le IS NULL) OR (mandat_sepa_id IS NOT NULL)))` |
| `contrat` | `ck_contrat_statut` | contrôle | Statut : l'une des 10 valeurs de STATUT_CONTRAT (orthographe du projet : EN_ATTENTE_DE_SIGNATURE). | `CHECK (((statut) = ANY ((ARRAY['EN_ATTENTE_DE_SIGNATURE', 'EN_ATTENTE_EFFET', 'ACTIF', 'ACTIF_JUSTIFICATIFS_ATTENDUS', 'AVENANT_EN_ATTENTE', 'IMPAYE', 'SUSPENDU', 'RESILIATION_EN_COURS', 'RESILIE', 'RENONCE']))))` |
| `contrat` | `contrat_numero_key` | unique | Pas de doublon sur : numero. | `UNIQUE (numero)` |
| `contrat_option` | `ck_contrat_option_dates` | contrôle | Une option ne se termine pas avant d'avoir commencé. | `CHECK (((date_fin IS NULL) OR (date_fin >= date_debut)))` |
| `declaration_fva` | `ck_declaration_fva_statut` | contrôle | Statut FVA : A_ENVOYER, DECLAREE, REJETEE ou DIFFEREE. | `CHECK (((statut) = ANY ((ARRAY['A_ENVOYER', 'DECLAREE', 'REJETEE', 'DIFFEREE']))))` |
| `devis_auto` | `ck_devis_auto_ancien_assureur` | contrôle | Assuré depuis plus d'un an : ancien assureur et ancien numéro de contrat obligatoires. | `CHECK (((NOT assure_plus_un_an) OR ((ancien_assureur IS NOT NULL) AND (ancien_numero_contrat IS NOT NULL))))` |
| `devis_auto` | `ck_devis_auto_date_effet` | contrôle | Date d'effet souhaitée entre le lendemain et 90 jours après la création du devis. | `CHECK (((date_effet_souhaitee IS NULL) OR ((date_effet_souhaitee >= ((date_creation)::date + 1)) AND (date_effet_souhaitee <= ((date_creation)::date + 90)))))` |
| `devis_auto` | `uk_devis_auto_devis` | unique | Pas de doublon sur : devis_id. | `UNIQUE (devis_id)` |
| `devis_enregistre` | `ck_devis_enregistre_habitation` | contrôle | Un devis HABITATION a sa référence, son logement, sa formule, ses options, sa franchise et son prix. | `CHECK ((((produit) <> 'HABITATION') OR ((reference IS NOT NULL) AND (type_logement IS NOT NULL) AND (adresse IS NOT NULL) AND (code_postal IS NOT NULL) AND (ville IS NOT NULL) AND (surface IS NOT NULL) AND (formule IS NOT NULL) AND (options IS NOT NULL) AND (franchise IS NOT NULL) AND (prix_mensuel IS NOT NULL))))` |
| `devis_enregistre` | `ck_devis_enregistre_produit` | contrôle | Produit : HABITATION ou AUTO. | `CHECK (((produit) = ANY ((ARRAY['HABITATION', 'AUTO']))))` |
| `devis_enregistre` | `ck_devis_enregistre_statut_devis` | contrôle | Statut du devis : l'une des 8 valeurs de STATUT_DEVIS. | `CHECK (((statut_devis) = ANY ((ARRAY['EN_COURS', 'VEHICULE_RENSEIGNE', 'BESOINS_RENSEIGNES', 'FORMULE_CHOISIE', 'RECAP_VALIDE', 'TRANSFORME', 'REFUSE_EN_LIGNE', 'EXPIRE']))))` |
| `devis_enregistre` | `uk_devis_enregistre_compte_reference` | unique | Pas de doublon sur : compte_id, reference. | `UNIQUE (compte_id, reference)` |
| `devis_option` | `uk_devis_option` | unique | Pas de doublon sur : devis_id, code_option. | `UNIQUE (devis_id, code_option)` |
| `document` | `ck_document_statut` | contrôle | Statut du document : EN_VIGUEUR ou REMPLACE. | `CHECK (((statut) = ANY ((ARRAY['EN_VIGUEUR', 'REMPLACE']))))` |
| `document` | `ck_document_stocke` | contrôle | Un document stocké a une clé de stockage. | `CHECK (((NOT stocke) OR (cle_stockage IS NOT NULL)))` |
| `document` | `ck_document_type` | contrôle | Type : l'une des 12 valeurs de TYPE_DOCUMENT. | `CHECK (((type) = ANY ((ARRAY['CONDITIONS_PARTICULIERES', 'CONDITIONS_GENERALES', 'IPID', 'FICHE_CONSEIL', 'MANDAT_SEPA', 'ECHEANCIER', 'FACTURE', 'AVENANT', 'MEMO_VEHICULE', 'ATTESTATION', 'DEVIS', 'DECLARATION_SINISTRE']))))` |
| `document` | `ck_document_version` | contrôle | La version commence à 1. | `CHECK ((version >= 1))` |
| `document` | `uk_document_en_vigueur` | index unique partiel | T-03 : une seule version EN_VIGUEUR par contrat, type et référence ; documents sans contrat concernés seulement s'ils ont une référence. | `UNIQUE (COALESCE(contrat_id, 0), type, COALESCE(reference, '')) WHERE ((statut) = 'EN_VIGUEUR' AND (contrat_id IS NOT NULL OR reference IS NOT NULL))` |
| `document_contenu` | `ck_document_contenu_taille` | contrôle | Un PDF fait entre 1 octet et 5 Mo. | `CHECK (((taille_octets >= 1) AND (taille_octets <= 5242880)))` |
| `document_contenu` | `ck_document_contenu_taille_reelle` | contrôle | La taille annoncée est la taille réelle du PDF. | `CHECK ((taille_octets = octet_length(contenu)))` |
| `document_contenu` | `uk_document_contenu_document` | unique | Un seul contenu par document. | `UNIQUE (document_id)` |
| `dossier_souscription` | `uk_dossier_souscription_devis` | unique | Pas de doublon sur : compte_id, reference_devis. | `UNIQUE (compte_id, reference_devis)` |
| `echeance` | `ck_echeance_montant_ttc` | contrôle | Montant TTC = montant HT + taxe. | `CHECK ((montant_ttc = (montant_ht + taxe)))` |
| `echeance` | `ck_echeance_statut` | contrôle | Statut de l'échéance : A_VENIR, PAYEE ou IMPAYEE. | `CHECK (((statut) = ANY ((ARRAY['A_VENIR', 'PAYEE', 'IMPAYEE']))))` |
| `facture` | `ck_facture_montant_ttc` | contrôle | Montant TTC = montant HT + taxe. | `CHECK ((montant_ttc = (montant_ht + taxe)))` |
| `facture` | `ck_facture_periode` | contrôle | La période facturée ne se termine pas avant d'avoir commencé. | `CHECK ((periode_fin >= periode_debut))` |
| `facture` | `uk_facture_numero` | unique | Pas de doublon sur : numero. | `UNIQUE (numero)` |
| `historique_statut_contrat` | `ck_historique_statut_apres` | contrôle | Nouveau statut : l'une des 10 valeurs de STATUT_CONTRAT. | `CHECK (((statut_apres) = ANY ((ARRAY['EN_ATTENTE_DE_SIGNATURE', 'EN_ATTENTE_EFFET', 'ACTIF', 'ACTIF_JUSTIFICATIFS_ATTENDUS', 'AVENANT_EN_ATTENTE', 'IMPAYE', 'SUSPENDU', 'RESILIATION_EN_COURS', 'RESILIE', 'RENONCE']))))` |
| `historique_statut_contrat` | `ck_historique_statut_auteur` | contrôle | Auteur du changement : CLIENT, SYSTEME ou GESTIONNAIRE. | `CHECK (((auteur) = ANY ((ARRAY['CLIENT', 'SYSTEME', 'GESTIONNAIRE']))))` |
| `historique_statut_contrat` | `ck_historique_statut_avant` | contrôle | Ancien statut (vide à la création) : l'une des 10 valeurs de STATUT_CONTRAT. | `CHECK (((statut_avant) = ANY ((ARRAY['EN_ATTENTE_DE_SIGNATURE', 'EN_ATTENTE_EFFET', 'ACTIF', 'ACTIF_JUSTIFICATIFS_ATTENDUS', 'AVENANT_EN_ATTENTE', 'IMPAYE', 'SUSPENDU', 'RESILIATION_EN_COURS', 'RESILIE', 'RENONCE']))))` |
| `justificatif` | `uk_justificatif_categorie` | unique | Pas de doublon sur : dossier_id, categorie. | `UNIQUE (dossier_id, categorie)` |
| `lien_reinitialisation` | `uk_lien_reinitialisation_jeton` | unique | Pas de doublon sur : jeton_hash. | `UNIQUE (jeton_hash)` |
| `mandat_sepa` | `ck_mandat_sepa_statut` | contrôle | Statut du mandat : ACTIF ou REVOQUE. | `CHECK (((statut) = ANY ((ARRAY['ACTIF', 'REVOQUE']))))` |
| `mandat_sepa` | `uk_mandat_sepa_dossier` | unique | Pas de doublon sur : dossier_id. | `UNIQUE (dossier_id)` |
| `mandat_sepa` | `uk_mandat_sepa_rum` | unique | Pas de doublon sur : rum. | `UNIQUE (rum)` |
| `permission` | `uk_permission` | unique | Pas de doublon sur : profil, rubrique, droit. | `UNIQUE (profil, rubrique, droit)` |
| `piece_jointe` | `ck_piece_jointe_format` | contrôle | Format : PDF, JPG ou PNG. | `CHECK (((format) = ANY ((ARRAY['PDF', 'JPG', 'PNG']))))` |
| `piece_jointe` | `ck_piece_jointe_statut` | contrôle | Statut de la pièce : STOCKEE ou REFUSEE. | `CHECK (((statut) = ANY ((ARRAY['STOCKEE', 'REFUSEE']))))` |
| `piece_jointe` | `ck_piece_jointe_taille` | contrôle | Taille entre 1 octet et 5 Mo (hypothèse non validée par la PO). | `CHECK (((taille_octets >= 1) AND (taille_octets <= 5242880)))` |
| `preuve_signature` | `ck_preuve_signature_nb_essais` | contrôle | Signature obtenue en 1 à 3 essais. | `CHECK (((nb_essais >= 1) AND (nb_essais <= 3)))` |
| `recommandation_devis` | `uk_recommandation_devis_reference` | unique | Pas de doublon sur : reference. | `UNIQUE (reference)` |
| `ref_plaque_test` | `ck_ref_plaque_test_comportement` | contrôle | Comportement simulé : TROUVE, INCONNU, ERREUR ou DELAI. | `CHECK (((comportement) = ANY ((ARRAY['TROUVE', 'INCONNU', 'ERREUR', 'DELAI']))))` |
| `ref_plaque_test` | `uk_ref_plaque_test_immatriculation` | unique | Pas de doublon sur : immatriculation. | `UNIQUE (immatriculation)` |
| `ref_vehicule` | `ck_ref_vehicule_energie` | contrôle | Énergie : ESSENCE, DIESEL, HYBRIDE, ELECTRIQUE ou GPL. | `CHECK (((energie) = ANY ((ARRAY['ESSENCE', 'DIESEL', 'HYBRIDE', 'ELECTRIQUE', 'GPL']))))` |
| `resiliation_hamon` | `ck_resiliation_hamon_nb_essais` | contrôle | Le nombre d'essais n'est jamais négatif. | `CHECK ((nb_essais >= 0))` |
| `resiliation_hamon` | `ck_resiliation_hamon_statut` | contrôle | Statut : A_ENVOYER, ENVOYEE, ACCEPTEE, INTROUVABLE ou ERREUR. | `CHECK (((statut) = ANY ((ARRAY['A_ENVOYER', 'ENVOYEE', 'ACCEPTEE', 'INTROUVABLE', 'ERREUR']))))` |
| `session_client` | `uk_session_client_jeton` | unique | Pas de doublon sur : jeton_hash. | `UNIQUE (jeton_hash)` |
| `sinistre` | `ck_sinistre_date_survenance` | contrôle | Le sinistre ne survient pas après la création de la déclaration (date du jour contrôlée par l'application). | `CHECK ((date_survenance <= (date_creation)::date))` |
| `sinistre` | `ck_sinistre_declarant` | contrôle | Déclarant : SOUSCRIPTEUR, CONDUCTEUR ou PROCHE. | `CHECK (((declarant) = ANY ((ARRAY['SOUSCRIPTEUR', 'CONDUCTEUR', 'PROCHE']))))` |
| `sinistre` | `ck_sinistre_declare` | contrôle | Un sinistre DECLARE a la déclaration sur l'honneur, un numéro et une date de déclaration. | `CHECK ((((statut) <> 'DECLARE') OR (declaration_honneur AND (numero IS NOT NULL) AND (declare_le IS NOT NULL))))` |
| `sinistre` | `ck_sinistre_statut` | contrôle | Statut : BROUILLON ou DECLARE. | `CHECK (((statut) = ANY ((ARRAY['BROUILLON', 'DECLARE']))))` |
| `sinistre` | `ck_sinistre_type` | contrôle | Type : un type de sinistre auto ou habitation (cohérence avec le produit vérifiée par l'application, US-24). | `CHECK (((type) = ANY ((ARRAY['ACCIDENT_TIERS', 'ACCIDENT_SANS_TIERS', 'VOL', 'BRIS_GLACE', 'INCENDIE', 'CLIMATIQUE_CATNAT', 'AUTRE', 'DEGAT_EAUX', 'VOL_CAMBRIOLAGE', 'RC_TIERS']))))` |
| `sinistre` | `uk_sinistre_numero` | unique | Pas de doublon sur : numero. | `UNIQUE (numero)` |
| `tarif_devis` | `uk_tarif_devis_reference` | unique | Pas de doublon sur : reference_devis. | `UNIQUE (reference_devis)` |
| `vehicule` | `ck_vehicule_date_acquisition` | contrôle | Acquisition pas avant la mise en circulation, ni après la création de la ligne. | `CHECK (((date_acquisition IS NULL) OR (((date_mise_circulation IS NULL) OR (date_acquisition >= date_mise_circulation)) AND (date_acquisition <= (date_creation)::date))))` |
| `vehicule` | `ck_vehicule_energie` | contrôle | Énergie : ESSENCE, DIESEL, HYBRIDE, ELECTRIQUE ou GPL. | `CHECK (((energie) = ANY ((ARRAY['ESSENCE', 'DIESEL', 'HYBRIDE', 'ELECTRIQUE', 'GPL']))))` |
| `vehicule` | `ck_vehicule_immatriculation` | contrôle | Immatriculation au format AA-123-AA si elle est renseignée. | `CHECK (((immatriculation IS NULL) OR ((immatriculation) ~ '^[A-Z]{2}-[0-9]{3}-[A-Z]{2}$')))` |
| `vehicule` | `ck_vehicule_km_annuel` | contrôle | Le kilométrage annuel n'est jamais négatif. | `CHECK (((km_annuel IS NULL) OR (km_annuel >= 0)))` |
| `vehicule` | `ck_vehicule_mode_achat` | contrôle | Mode d'achat : COMPTANT, CREDIT, LOA ou LLD. | `CHECK (((mode_achat) = ANY ((ARRAY['COMPTANT', 'CREDIT', 'LOA', 'LLD']))))` |
| `vehicule` | `ck_vehicule_source` | contrôle | Véhicule trouvé par PLAQUE ou par MARQUE. | `CHECK (((source) = ANY ((ARRAY['PLAQUE', 'MARQUE']))))` |
| `vehicule` | `ck_vehicule_stationnement` | contrôle | Stationnement : GARAGE_FERME, PARKING_PRIVE ou RUE. | `CHECK (((stationnement) = ANY ((ARRAY['GARAGE_FERME', 'PARKING_PRIVE', 'RUE']))))` |
| `vehicule` | `ck_vehicule_titulaire` | contrôle | Titulaire de la carte grise : SOUSCRIPTEUR, CONJOINT, ENFANT, SOCIETE_LOCATION ou AUTRE_PERSONNE. | `CHECK (((titulaire_carte_grise) = ANY ((ARRAY['SOUSCRIPTEUR', 'CONJOINT', 'ENFANT', 'SOCIETE_LOCATION', 'AUTRE_PERSONNE']))))` |
| `vehicule` | `ck_vehicule_usage` | contrôle | Usage : PRIVE, TRAJET_TRAVAIL ou PROFESSIONNEL. | `CHECK (((usage) = ANY ((ARRAY['PRIVE', 'TRAJET_TRAVAIL', 'PROFESSIONNEL']))))` |


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

| Table | Contrainte | Nature | Règle en français | Définition SQL |
|---|---|---|---|---|
| `calcul_tarif` | `calcul_tarif_id_tarif_key` | unique | Pas de doublon sur : id_tarif. | `UNIQUE (id_tarif)` |
| `grille_tarifaire` | `grille_tarifaire_dates` | contrôle | Une version de grille ne se termine pas avant d'avoir commencé. | `CHECK (((date_fin IS NULL) OR (date_fin >= date_debut)))` |
| `grille_tarifaire` | `grille_tarifaire_version_key` | unique | Pas de doublon sur : version. | `UNIQUE (version)` |
| `parametre_tarifaire` | `parametre_tarifaire_unique` | unique | Pas de doublon sur : grille_id, categorie, cle. | `UNIQUE (grille_id, categorie, cle)` |
| `ref_formule` | `ref_formule_code` | contrôle | Code de formule : TIERS, INTER ou TR. | `CHECK (((code) = ANY ((ARRAY['TIERS', 'INTER', 'TR']))))` |
| `ref_formule` | `ref_formule_unique` | unique | Pas de doublon sur : grille_id, code. | `UNIQUE (grille_id, code)` |
| `ref_option` | `ref_option_produit` | contrôle | Produit : HABITATION ou AUTO. | `CHECK (((produit) = ANY ((ARRAY['HABITATION', 'AUTO']))))` |
| `ref_option` | `ref_option_unique` | unique | Pas de doublon sur : grille_id, code. | `UNIQUE (grille_id, code)` |
| `ref_tarif` | `ref_tarif_bornes` | contrôle | La borne maximale n'est pas inférieure à la borne minimale. | `CHECK (((borne_min IS NULL) OR (borne_max IS NULL) OR (borne_max >= borne_min)))` |
| `ref_tarif` | `ref_tarif_coefficient` | contrôle | Le coefficient est strictement positif. | `CHECK ((coefficient > (0)::numeric))` |
| `ref_tarif` | `ref_tarif_critere` | contrôle | Critère : AGE, BONUS_MALUS, USAGE ou VALEUR_VEHICULE. | `CHECK (((critere) = ANY ((ARRAY['AGE', 'BONUS_MALUS', 'USAGE', 'VALEUR_VEHICULE']))))` |
