# assurance-iard-api

API backend du projet Assurance IARD : Spring Boot 4, Java 21, PostgreSQL.

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
- Code de test de la double authentification (RG4) : `123456`, activé par
  `CODE_BOUCHON_ACTIF=true` (déjà positionné par `./mvnw spring-boot:run` ; à
  ajouter à la configuration de lancement dans IntelliJ).
- Lien de réinitialisation du mot de passe : aucun e-mail réel, la même variable
  l'écrit dans la console du backend (ligne `Bouchon : lien de réinitialisation`).
  Sur dev et qualif, il se lit dans les logs du pod backend, via ArgoCD.

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
