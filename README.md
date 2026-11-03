# formation-ba-assurance-iard

Monorepo du projet Assurance IARD : une API Spring Boot et une application
Angular, déployées sur la plateforme (dev, qualif, preprod, prod).

```
backend/                  # API Spring Boot (assurance-iard-api)       -> backend/README.md
frontend/                 # application Angular (assurance-iard-web)    -> frontend/README.md
docker-compose.yaml       # base PostgreSQL + backend, pour le poste local
helm.yaml                 # applications déployées (lu par la plateforme)
gateway.yaml              # routage du gateway vers les backends, CORS
```

## Architecture

```
navigateur ──> https://frontend.formation-ba-assurance-iard.<env>.check-consulting.net          (écran)
navigateur ──> https://gateway.formation-ba-assurance-iard.<env>.check-consulting.net/backend/... (API)
                 └─ gateway : vérifie l'origine (CORS), retire /backend ──> backend
```

- Le navigateur n'appelle **que le gateway** : le backend n'a pas d'adresse publique.
- Le frontend calcule l'adresse du gateway depuis l'adresse de la page
  (`apiBase`, `frontend/src/app/core/config/api.ts`) : même image dans les
  4 environnements, aucune URL écrite en dur.
- En local, pas de gateway : le serveur de dev Angular relaie `/api/...`
  vers le backend du poste (`frontend/proxy.conf.json`).

## Versions

### Backend

| Composant    | Version |
|--------------|---------|
| Java         | 21      |
| Spring Boot  | 4.1     |
| Maven        | 3.10 (wrapper `mvnw`) |
| PostgreSQL   | 16      |
| Flyway       | 12      |
| MapStruct    | 1.6     |

### Frontend

| Composant    | Version |
|--------------|---------|
| Node.js      | 24      |
| npm          | 11      |
| Angular      | 22.2    |
| TypeScript   | 6.0     |
| Vitest       | 5       |

## Lancer en local

Prérequis : Docker, Java 21, Node.js 24.

### 1. Base de données

Depuis la racine du projet, la première fois : copier `.env.example` en `.env` et y mettre
l'utilisateur et le mot de passe de `backend/src/main/resources/application.yaml`
(le fichier `.env` reste sur le poste, il n'est jamais commité).

```bash
docker compose up -d postgres
```

PostgreSQL sur `localhost:5432`, base `assurance_iard`, utilisateur `postgres` / `postgres`.

### 2. Backend

```bash
cd backend
./mvnw spring-boot:run
```

Disponible sur http://localhost:8080/ (santé : `/actuator/health`,
documentation de l'API : `/swagger-ui.html`).

Variante tout-en-Docker (base + backend) : `docker compose up -d --build`.

### 3. Frontend

```bash
cd frontend
npm install
npm start
```

Disponible sur http://localhost:4200/ ; les appels `/api/...` sont relayés
vers le backend sur le port 8080.

### Tout arrêter

```bash
docker compose down
```

## Qualité (mêmes contrôles qu'en CI)

Dans chaque composant : `make lint`, `make test-unit`, `make test-integration`.
Couverture minimale : 80 % (quality gate SonarQube).

## Déploiement

Merge sur `main` → CI (image taguée avec le sha court du commit) → Kargo met à
jour `helm.yaml` (dev automatique) → ArgoCD. Qualif, preprod, prod : promotion
dans Kargo.
