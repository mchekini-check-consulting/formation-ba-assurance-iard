# assurance-iard-web

Application frontend du projet Assurance IARD : Angular 22, TypeScript.

## Structure

```
src/app/
  app.ts, app.html, app.config.ts, app.routes.ts   # composant racine, configuration, routes
  core/
    config/api.ts    # apiBase : adresse des backends
    service/         # services d'appel aux backends (HttpClient)
    model/           # interfaces des données
    interceptors/    # traitements communs des appels HTTP
    guards/          # protection d'accès aux pages
  feature/           # un dossier par écran (ex. feature/items/)
proxy.conf.json      # relais local /api/<backend> -> backend du poste
nginx.conf           # serveur de l'image Docker (port 8080)
```

Préfixe des composants : `iard` (balises `<iard-...>`).

## Appeler un backend

Toujours via `apiBase('<backend>')` (`src/app/core/config/api.ts`), jamais
d'URL écrite en dur :

| Où           | `apiBase('assurance-iard-api')` renvoie |
|--------------|------------------------------|
| En ligne     | `https://gateway.formation-ba-assurance-iard.<env>.check-consulting.net/assurance-iard-api` |
| En local     | `/api/<backend>`, relayé vers le backend du poste par `proxy.conf.json` (`assurance-iard-api` : 8080, `moteurtarifaire` : 8081) |

```ts
@Injectable({ providedIn: 'root' })
export class ItemsService {
  private readonly http = inject(HttpClient);
  private readonly url = `${apiBase('assurance-iard-api')}/items`;
  lister(): Observable<Item[]> { return this.http.get<Item[]>(this.url); }
}
```

## Lancer en local

```bash
npm install
npm start
```

http://localhost:4200/ (rechargement automatique). Les backends appelés doivent
tourner sur le poste (`assurance-iard-api` : port 8080, `moteurtarifaire` : port 8081) pour que
les appels `/api/<backend>/...` répondent.

## Qualité (mêmes contrôles qu'en CI)

| Commande                | Rôle |
|-------------------------|------|
| `make lint`             | analyse oxlint |
| `make test-unit`        | tests Vitest + couverture (`coverage/assurance-iard-web/lcov.info`) |
| `make test-integration` | aucun pour l'instant |

Sans `make` (Windows) : lancer les commandes `npx ...` écrites dans le `Makefile`.
Couverture minimale : 80 % (SonarQube). Un fichier `*.spec.ts` par composant ou service.

## Image Docker

`Dockerfile` : `npm run build`, puis `dist/assurance-iard-web/browser` servi
par nginx non-root sur le port 8080 (`containerPort` dans `helm.yaml`).
