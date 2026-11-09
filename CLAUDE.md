# formation-ba-assurance-iard

Dépôt d'un prototype développé par des Business Analysts (BA) en formation,
sans connaissance du développement ni de git. Expliquer en langage simple,
ne jamais faire d'action git risquée (rebase, push forcé, `reset --hard`,
suppression de branche) et passer par les skills de `.claude/skills/`
(README) pour les gestes courants : nouvelle fonctionnalité, nouveau composant
(backend ou frontend), modèle de données, appel d'un backend depuis le frontend, lancement local,
déploiement, vérification du déploiement.

## Architecture

| Composant | Dossier | Stack | Port dans l'image | En local |
|---|---|---|---|---|
| backend(s) `type: backend` | `<name>/` (ex. `backend/`, `moteurtarifaire/`) | Java 21 / Spring Boot 4 / JPA / Flyway (Maven, artifact `assurance-iard-api`) | 8080 | `./mvnw spring-boot:run` (ou IntelliJ), port 8080 pour `backend/`, suivant pour les autres (`SERVER_PORT=<port>`, 8081 pour `moteurtarifaire/`) |
| gateway `type: gateway` | — (fourni par la plateforme) | Traefik, routes dans `gateway.yaml` | 80 | pas de gateway : relais du serveur de dev du frontend |
| frontend `type: frontend` | `frontend/` | Angular 22 / TypeScript (projet `assurance-iard-web`, préfixe `iard`), servi par nginx | 8080 | http://localhost:4200 (`npm start`) |
| health `type: frontend` | `health/` | Vite / TypeScript sans framework + keycloak-js : connexion Keycloak (realm `check-projects-sso`, client `assureasy-frontend-client`) puis appel de `/backend/actuator/health` avec le token (route protégée par le middleware `jwt` de `gateway.yaml`), servi par nginx | 8080 | http://localhost:4201 (`npm start`) |

Base locale : `docker compose up -d postgres` à la racine (`docker-compose.yaml`,
PostgreSQL 16, port 5432) : base `assurance_iard`, et base `moteurtarifaire` pour
`moteurtarifaire/` (même serveur). Identifiants dans `.env`
(`POSTGRES_USER`, `POSTGRES_PASSWORD`, jamais commité), jamais en clair dans `docker-compose.yaml`.

URLs, identiques dans les 4 environnements (`dev`, `qualif`, `preprod`, `prod`) :
- écran : `https://frontend.formation-ba-assurance-iard.<env>.check-consulting.net`
- backends : `https://gateway.formation-ba-assurance-iard.<env>.check-consulting.net/<backend>/...`
  (préfixe retiré par le gateway). Un backend n'a jamais d'adresse publique
  propre ; le navigateur n'appelle que le gateway.

Déploiement : merge sur `main` → CI (image taguée avec le sha court) → Kargo
écrit `tag.dev` dans `helm.yaml` (commit `promote(dev)` sur `main`) → ArgoCD.
Qualif : clic « Promote » dans Kargo. `main` avance donc tout seul : toujours
l'intégrer dans la branche avant d'envoyer (skill `deploy`).

## Conventions

**Backend** (package racine `net.checkconsulting.assuranceiard.api`)
- Couches : `resource` (contrôleurs HTTP, `@RestController`) → `service`
  (règles métier) → `repository` (accès aux données, Spring Data JPA).
  Autres packages : `model` (entités), `dto`, `mapper` (MapStruct,
  `componentModel=spring`), `config`, `exception`, `utils`.
- Logs : une ligne JSON par log sur stdout, niveau `LOG_LEVEL`, champ `layer`
  = `controller` / `service` / `db` sur les logs métier (seuls collectés dans
  le dashboard Grafana « Logs métier »). **Pas encore en place** dans le
  backend Spring Boot : à ajouter (logging structuré JSON) avant de s'appuyer
  sur Grafana. Jamais de données personnelles, d'arguments, de corps de
  requête ni de paramètres SQL dans les logs.
- Base PostgreSQL : variables injectées par la plateforme (`DATABASE_URL`,
  `SPRING_DATASOURCE_*`, `PG*`) ; Spring Boot lit `SPRING_DATASOURCE_*`.
  Valeurs locales par défaut = `backend/src/main/resources/application.yaml`
  (alignées sur `docker-compose.yaml` à la racine).
- Schéma : uniquement par migrations Flyway —
  `backend/src/main/resources/db/migration/V<numéro>__<nom>.sql`, appliquées
  au démarrage ; `spring.jpa.hibernate.ddl-auto: validate` (Hibernate vérifie,
  ne modifie jamais). Ne jamais modifier une migration déjà sur `main`.
- Tests : unitaires `*Test.java` (`@WebMvcTest`, Mockito…) par `./mvnw test` ;
  intégration `*IT.java` (Testcontainers, PostgreSQL 16) par failsafe.

**Frontend** (`frontend/src/app/`)
- Organisation : `core/` (`config/`, `service/`, `model/`, `guards/`,
  `interceptors/`), `feature/` (un dossier par écran), `app.*` (composant racine).
- Adresse d'un backend : `apiBase('<backend>')` dans
  `frontend/src/app/core/config/api.ts`, calculée depuis l'adresse de la page.
  Jamais d'URL, d'environnement, de `localhost` ou de nom de dépôt écrits en dur.
- Un service par ressource (`core/service/<nom>.service.ts`, `HttpClient`,
  `providedIn: 'root'`), sur le modèle de `hello.service.ts`.
- En local, `apiBase` renvoie `/api`, relayé vers `http://localhost:8080` par
  `frontend/proxy.conf.json` (préfixe `/api` retiré). Une seule entrée
  aujourd'hui : avec un deuxième backend, passer à `/api/<backend>` (une
  entrée par backend, skill `nouvelle-fonctionnalite`).

**`helm.yaml`** (lu par la plateforme)
- Nouvelle application **à la fin** de `applications:` (Kargo repère les
  applications par leur position).
- `tag` en bloc, une valeur par ligne, sans commentaire en fin de ligne ;
  jamais modifié à la main (écrit par Kargo). Tag vide = composant pas encore
  déployé dans cet environnement (normal après un ajout).
- Valeurs littérales uniquement ; `replicas`, `tag`, `envList[].value` :
  valeur unique ou map par environnement.

**`gateway.yaml`**
- Il fige le routage : chaque backend y a un router `PathPrefix(`/<backend>`)`,
  un middleware `<backend>-strip`, un service `http://<backend>:80`, et le
  middleware `cors` commun (origines = frontends du projet, jamais `*`).
- Exception : `moteurtarifaire` n'a pas de route. Il n'est appelé que par le
  backend, à l'intérieur du cluster (`http://moteurtarifaire:80`, client Feign).
  Il n'a pas d'authentification : ne pas lui donner de route publique.
- `{{ .project }}`, `{{ .env }}`, `{{ .domain }}` disponibles : une seule
  configuration pour les 4 environnements.

**Qualité** (mêmes contrôles qu'en CI, `pr.yaml`)
- Chaque composant : `make lint`, `make test-unit`, `make test-integration`
  (sous Windows sans `make` : lancer les commandes du `Makefile` directement).
  - backend : PMD (`lint`), `./mvnw -B test` (`test-unit`), failsafe (`test-integration`) ;
  - frontend : `npx oxlint src` (`lint`), `npx ng test --watch=false --coverage` (Vitest, `test-unit`).
- Tests de bout en bout : `make e2e-tests`, job `e2e-test` de la CI après les tests
  d'intégration (cible optionnelle, composant sans cette cible ignoré).
  - health : Playwright (`health/e2e/`), application construite servie par
    `vite preview`, Keycloak et gateway simulés (`page.route`) ; en local sans
    télécharger Chromium : `PLAYWRIGHT_CHANNEL=chrome npx playwright test` ;
  - frontend : pas encore de tests e2e (la cible affiche un message).
- Couverture ≥ 80 % (quality gate SonarQube) ; rapport local :
  `backend/target/site/jacoco/jacoco.xml` (JaCoCo) pour le backend,
  `frontend/coverage/assurance-iard-web/lcov.info` pour le frontend.
- Tout nouveau code arrive avec ses tests unitaires.

**Git**
- Une fonctionnalité = une branche `feature/<nom>` partie d'un `main` à jour.
- Commits au nom du BA seul : jamais de ligne `Co-Authored-By: Claude`.
- Jamais de secrets, `.env`, bases locales ni gros fichiers dans un commit.
