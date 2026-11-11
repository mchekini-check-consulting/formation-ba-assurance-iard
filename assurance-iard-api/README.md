# assurance-iard-api

API du projet Assurance IARD : Spring Boot 4, Java 21, PostgreSQL.

## Structure

```
src/main/java/net/checkconsulting/assuranceiard/api/
  AssuranceIardApplication.java   # point d'entrée
  resource/      # contrôleurs REST (routes HTTP)
  service/       # règles métier
  repository/    # accès aux données (Spring Data JPA)
  model/         # entités JPA (tables)
  dto/           # objets échangés avec le frontend
  mapper/        # conversions entité <-> DTO (MapStruct)
  exception/     # erreurs métier et gestion globale des erreurs
  config/        # configuration Spring
  securite/      # contrôle des accès selon le profil (US-04)
  utils/         # outils communs
src/main/resources/
  application.yaml   # configuration (base locale par défaut)
  db/migration/      # migrations Flyway : V<n>__<description>.sql
src/test/java/...    # tests (Testcontainers : PostgreSQL 16 jetable, Docker requis)
```

Couches : `resource` (HTTP) → `service` (règles métier) → `repository` (données).

## Lancer en local

```bash
docker compose up -d postgres   # depuis la racine, après avoir créé .env (POSTGRES_USER, POSTGRES_PASSWORD)
./mvnw spring-boot:run
```

- API : http://localhost:8080/
- Santé : http://localhost:8080/actuator/health
- Documentation de l'API : http://localhost:8080/swagger-ui.html
- Code de la double authentification (US-06) : envoyé par e-mail, à lire dans
  Mailpit (http://localhost:8025, `docker compose up -d mailpit` à la racine).
  Le code de test `123456` est éteint par défaut (`CODE_TEST_CONNEXION_ACTIF=true`
  le rallume ; jamais en preprod ni en prod).
- Code de signature de contrat : code de test `123456`, activé par
  `CODE_BOUCHON_ACTIF=true` (déjà positionné par `./mvnw spring-boot:run` ; à
  ajouter à la configuration de lancement dans IntelliJ).
- Lien de réinitialisation du mot de passe (US-07) : envoyé par e-mail, à lire dans
  Mailpit (http://localhost:8025). Il n'est jamais écrit dans les logs. Valable 24 heures
  par défaut ; pour essayer un lien expiré, lancer avec `LIEN_REINITIALISATION_VALIDITE=2m`.
  Sur dev et qualif : https://mailpit.dev.check-consulting.net (filtre `tag:<env>-formation-ba-assurance-iard`).
- Comptes de recette (US-04), créés au démarrage si `DONNEES_TEST_ACTIVES=true` (déjà positionné par
  `./mvnw spring-boot:run`, docker compose, dev et qualif ; jamais en preprod ni en prod). Mot de passe commun
  `Recette2026` (`DONNEES_TEST_MOT_DE_PASSE`), code de connexion dans Mailpit :
  `recette.assure1@assureasy.fr` et `recette.assure2@assureasy.fr` (assurés, chacun un devis, un contrat et un
  document), `recette.gestionnaire@assureasy.fr`, `recette.admin@assureasy.fr`.

## Contrôle des accès (US-04)

Chaque route déclare ce qu'elle demande, contrôlé avant l'appel par `securite/ControleAccesAspect` :

```java
@Acces(rubrique = Rubrique.CONTRATS, droit = Droit.CONSULTER,
        ressource = TypeRessource.CONTRAT, identifiant = "referenceDevis")
```

- Le droit est relu dans le tableau de l'US-02 (table `permission`) à chaque requête : un droit
  retiré par l'administrateur est refusé dès la requête suivante.
- Un client (Prospect, Assuré) n'accède qu'à ses ressources ; le back-office (`backOffice = true`)
  est réservé aux profils Gestionnaire et Administrateur.
- Une route sans `@Acces` ni `@AccesPublic` est refusée (`RoutesDeclareesTest` le vérifie).
- Chaque refus répond « Accès non autorisé » (403) et est enregistré dans la table `acces_refuse`
  (écran administrateur « Journal des accès refusés », route `GET /admin/acces-refuses`).
- Nouvelle ressource (ex. les sinistres) : ajouter sa valeur à `TypeRessource`, une classe
  `RessourceProtegee` qui retrouve son propriétaire (modèle : `ProprietairesContrat`), puis annoter ses routes.
- IBAN : seuls ses 4 derniers caractères sortent du serveur (`ibanFin`), pour tous les profils.

## Base de données

- En local : `jdbc:postgresql://localhost:5432/assurance_iard`, `postgres` / `postgres`
  (`application.yaml`, base de `docker-compose.yaml`).
- Sur la plateforme : variables `SPRING_DATASOURCE_*` injectées, elles
  remplacent automatiquement les valeurs locales.
- Schéma **uniquement par migrations Flyway** dans `src/main/resources/db/migration/`,
  appliquées au démarrage (`ddl-auto: validate` : Hibernate vérifie, ne modifie jamais).
  Nom : `V1__creer_table_item.sql`, `V2__...` (deux underscores).
  Ne jamais modifier une migration déjà sur `main` : en ajouter une nouvelle.

## Qualité (mêmes contrôles qu'en CI)

| Commande                | Rôle |
|-------------------------|------|
| `make lint`             | analyse PMD |
| `make test-unit`        | tests + couverture JaCoCo (`target/site/jacoco/jacoco.xml`) |
| `make test-integration` | tests d'intégration (classes `*IT`) |

Sans `make` (Windows) : lancer les commandes `./mvnw ...` écrites dans le `Makefile`.
Couverture minimale : 80 % (SonarQube).

## Image Docker

`Dockerfile` : compilation Maven, puis image distroless Java 21 non-root,
port 8080 (`containerPort` dans `helm.yaml`).
