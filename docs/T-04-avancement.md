# T-04 — Référentiels et calcul du tarif auto (grille AUTO-2026.1)

**Branche :** `feature/us-16-bis-1-signature`  
**Date de mise à jour :** 2026-11-23

---

## Résumé

| Composant | Modèle de données | Code métier | Tests unitaires | Tests d'intégration |
|---|---|---|---|---|
| `moteurtarifaire` | ✅ V6 + V7 | ✅ complet | ✅ 286 OK | ✅ 36 OK — couverture 98,9 % |
| `assurance-iard-api` | ✅ données recette | ✅ complet | ✅ 495 OK | ✅ 69 OK — couverture 95,8 % |

---

## Ce qui est fait

### moteurtarifaire

#### Modèle de données (migrations)
- **V6__structure_grille_auto.sql** (nouveau) : colonne `produit NOT NULL` sur `grille_tarifaire` ; tables `ref_formule`, `ref_option`, `ref_tarif` avec FK → `grille_tarifaire` (ON DELETE RESTRICT, comportement par défaut — aucun CASCADE ajouté).
- **V7__donnees_auto_2026_1.sql** (nouveau) : grille AUTO-2026.1 complète (3 formules TIERS / TIERS_COMPLET / INTERMEDIAIRE, 3 options, 7 coefficients par formule, durée de validité 30 jours).

#### Code métier (production)
- **VersionGrille** : champ `produit` ajouté.
- **RefFormule, RefOption** : champs enrichis (garantiesIncluses, nonCouvert, partRc, soumisBonus, categorieTaxe…).
- **RefFormuleRepository, RefOptionRepository, RefTarifRepository** : méthodes `findByGrilleId`.
- **VersionGrilleRepository** : méthode `findByProduitAndDateDebutLessThanEqualAndDateFinGreaterThanEqual`.
- **GrilleTarifaireService** (habitation) : filtre désormais sur `produit = HABITATION` pour ne pas capturer la grille auto.
- **GrilleTarifaireAutoService** (nouveau) : charge la grille AUTO active, assemble le DTO, vérifie la cohérence au démarrage.
- **GrilleTarifaireAutoAssembleur** (nouveau) : construit `GrilleTarifaireAuto` depuis les entités JPA.
- **GrilleTarifaireAuto** (nouveau DTO record) : version, formules, options, coefficients par critère, durée de validité.
- **TarificateurAuto** (nouveau service) : calcule les trois prix (TIERS / TR / INTER), applique les 7 coefficients (âge permis, bonus-malus, usage, valeur véhicule, km annuels, stationnement, surprime novice), conseille une formule, enregistre le calcul.
- **TarifAutoResource** : `POST /v1/tarifs/auto`, validation des champs, réponse structurée.
- **TarifAutoRequest / TarifAutoResponse** (DTOs) : immatriculation ou anneePermis, bonusMalus (0.50–3.50), kmAnnuels, usage, stationnement, formule choisie, options.
- **FormuleAuto, UsageAuto, StationnementAuto** (nouvelles énumérations).
- **ControleGrillesAuDemarrage** : vérifie désormais les deux grilles (habitation + auto) au démarrage de l'application.

#### Tests unitaires (tous verts avant la correction FK)
- **GrilleTarifaireAutoServiceTest** (nouveau — 6 tests) : chargement, grille introuvable, deux grilles en même temps, vérification.
- **TarificateurAutoTest** (nouveau — 10 tests) : calcul nominal, surprime novice présent/absent, formule conseillée, détail, dateValidite.
- **TarifAutoResourceTest** (nouveau — 9 tests) : appel valide, 6 champs obligatoires manquants, bonusMalus hors limites, enum usage inconnu, grille indisponible.
- **ControleGrillesAuDemarrageTest** : 2 tests supplémentaires pour la grille auto.
- **ReferentielsAutoTest** : 3 tests supplémentaires couvrant les nouveaux champs.
- **GrilleTarifaireServiceTest** : mis à jour pour le filtre produit.

#### Tests d'intégration (correction FK en cours)
- **TarifAutoIT** (nouveau — 7 tests) : nominal 3 formules, conseille TR/INTER, détail, surprime novice (assert comparatif), champ manquant 400, non-régression OpenAPI habitation.
- **GrilleTarifaireIT** et **TarifHabitationIT** : mis à jour pour respecter l'ordre de suppression FK (enfants d'abord : ref_tarif → ref_formule → ref_option → grille_tarifaire).
- **JeuxDeDonnees** : signature `installerGrilleDeReference` étendue (+ RefFormuleRepository, RefOptionRepository, RefTarifRepository) ; `versionDeReference()` pose `produit = HABITATION`.

### assurance-iard-api

- **TarifAutoRequest / TarifAutoResponse** (nouveaux DTOs).
- **MoteurTarifaireClient** : méthode `calculerAuto(DemandeMoteurAuto)` → `POST /v1/tarifs/auto` (Feign).
- **RefPlaqueTestRepository** : méthode `findByImmatriculation`.
- **VehiculeIntrouvableException** (nouvelle exception métier).
- **TarificateurAutoService** (nouveau) : résout le véhicule (par `refVehiculeId` ou par plaque → RefPlaqueTest → RefVehicule), appelle le moteur tarifaire, enrichit la réponse avec marque / modèle / valeur.
- **DonneesRecetteSprint3** (nouveau) : charge `donnees_recette_t04.sql` au démarrage en profil de recette.
- **donnees_recette_t04.sql** (nouveau) : 4 véhicules de référence, 4 plaques de test (AB-123-CD, EF-456-GH, ZZ-999-ZZ → TROUVE ; XX-000-XX → NON_TROUVE).
- **TarificateurAutoServiceTest** (nouveau — 6 tests) : résolution par id, par plaque, plaque introuvable, NON_TROUVE, id inconnu, sans identifiant, info enrichie.

---

## Ce qui reste à faire

Rien : tous les contrôles ont passé le 2026-11-23.

---

## Fichiers créés ou modifiés

### moteurtarifaire — nouveaux fichiers
| Fichier | Nature |
|---|---|
| `src/main/resources/db/migration/V6__structure_grille_auto.sql` | Migration : produit + tables auto |
| `src/main/resources/db/migration/V7__donnees_auto_2026_1.sql` | Migration : données grille AUTO-2026.1 |
| `src/main/java/.../dto/GrilleTarifaireAuto.java` | DTO record grille auto |
| `src/main/java/.../dto/TarifAutoRequest.java` | DTO requête tarif auto |
| `src/main/java/.../dto/TarifAutoResponse.java` | DTO réponse tarif auto |
| `src/main/java/.../enums/FormuleAuto.java` | Enum TIERS / TIERS_COMPLET / INTERMEDIAIRE |
| `src/main/java/.../enums/UsageAuto.java` | Enum PRIVE / PROFESSIONNEL |
| `src/main/java/.../enums/StationnementAuto.java` | Enum GARAGE_FERME / VOIE_PUBLIQUE / … |
| `src/main/java/.../mapper/GrilleTarifaireAutoAssembleur.java` | Assembleur entités → DTO |
| `src/main/java/.../resource/TarifAutoResource.java` | Contrôleur POST /v1/tarifs/auto |
| `src/main/java/.../service/GrilleTarifaireAutoService.java` | Service grille auto |
| `src/main/java/.../service/TarificateurAuto.java` | Service calcul tarif auto |
| `src/test/.../TarifAutoIT.java` | IT : 7 tests bout en bout |
| `src/test/.../resource/TarifAutoResourceTest.java` | Unitaire : 9 tests contrôleur |
| `src/test/.../service/GrilleTarifaireAutoServiceTest.java` | Unitaire : 6 tests service grille |
| `src/test/.../service/TarificateurAutoTest.java` | Unitaire : 10 tests calcul |

### moteurtarifaire — fichiers modifiés
| Fichier | Modification |
|---|---|
| `src/main/java/.../model/VersionGrille.java` | + champ `produit` |
| `src/main/java/.../model/RefFormule.java` | + champs garantiesIncluses, nonCouvert, partRc, soumisBonus |
| `src/main/java/.../model/RefOption.java` | + champs categorieTaxe, soumisBonus |
| `src/main/java/.../repository/VersionGrilleRepository.java` | + findByProduitAndDates |
| `src/main/java/.../repository/RefFormuleRepository.java` | + findByGrilleId |
| `src/main/java/.../repository/RefOptionRepository.java` | + findByGrilleId |
| `src/main/java/.../repository/RefTarifRepository.java` | + findByGrilleId |
| `src/main/java/.../service/GrilleTarifaireService.java` | filtre produit = HABITATION |
| `src/main/java/.../config/ControleGrillesAuDemarrage.java` | + vérification grille auto |
| `src/test/.../JeuxDeDonnees.java` | signature + 3 repos, ordre FK, produit HABITATION |
| `src/test/.../GrilleTarifaireIT.java` | + 3 repos, ordre FK, produit HABITATION |
| `src/test/.../TarifHabitationIT.java` | + 3 repos, ordre FK |
| `src/test/.../config/ControleGrillesAuDemarrageTest.java` | + 2 tests grille auto |
| `src/test/.../model/ReferentielsAutoTest.java` | + 3 tests nouveaux champs |
| `src/test/.../service/GrilleTarifaireServiceTest.java` | adapté filtre produit |

### assurance-iard-api — nouveaux fichiers
| Fichier | Nature |
|---|---|
| `src/main/java/.../dto/TarifAutoRequest.java` | DTO requête |
| `src/main/java/.../dto/TarifAutoResponse.java` | DTO réponse |
| `src/main/java/.../exception/VehiculeIntrouvableException.java` | Exception métier |
| `src/main/java/.../service/TarificateurAutoService.java` | Service résolution + appel moteur |
| `src/main/java/.../service/DonneesRecetteSprint3.java` | Chargement données recette T-04 |
| `src/main/resources/db/donnees-test/donnees_recette_t04.sql` | 4 véhicules + 4 plaques de test |
| `src/test/.../service/TarificateurAutoServiceTest.java` | Unitaire : 6 tests |

### assurance-iard-api — fichiers modifiés
| Fichier | Modification |
|---|---|
| `src/main/java/.../client/MoteurTarifaireClient.java` | + méthode calculerAuto |
| `src/main/java/.../repository/RefPlaqueTestRepository.java` | + findByImmatriculation |

---

## Problème résolu : violation FK dans les tests d'intégration

**Symptôme :** `DataIntegrityViolationException` lors du `@BeforeEach` des tests IT (habitation existants) : `versions.deleteAllInBatch()` échoue parce que `ref_formule` référence `grille_tarifaire` via FK ON DELETE RESTRICT.

**Cause :** La migration V6 a ajouté les FK. Le code de nettoyage des tests supprimait la grille avant ses enfants.

**Mauvaise piste écartée :** Migration V8 ajoutant ON DELETE CASCADE — rejetée (une version de grille ne doit jamais être supprimée ; traçabilité des tarifs des contrats).

**Correction appliquée :** Nettoyage dans le bon ordre dans `JeuxDeDonnees.installerGrilleDeReference` : d'abord `refTarifs.deleteAllInBatch()`, `refFormules.deleteAllInBatch()`, `refOptions.deleteAllInBatch()`, puis `versions.deleteAllInBatch()`. Aucune migration touchée.

---

## Migrations existantes sur main (inchangées)

Les migrations V1 à V5 sont sur `main` et n'ont pas été touchées. Seules V6 et V7 sont nouvelles sur cette branche.
