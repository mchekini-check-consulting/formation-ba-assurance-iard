# formation-ba-assurance-iard

Monorepo contenant le backend NestJS et l'application frontend React.

```
backend/                  # API NestJS
  src/
    app.controller.ts      # GET "/" -> "Hello World!"
    app.service.ts
    main.ts
frontend/                 # application React (Vite)
```

## Versions

### Backend

| Composant   | Version |
|-------------|---------|
| Node.js     | 25.8.1  |
| npm         | 11.11.0 |
| NestJS      | 12.0    |
| TypeScript  | 6.0     |

### Frontend

| Composant    | Version |
|--------------|---------|
| Node.js      | 25.8.1  |
| npm          | 11.11.0 |
| React        | 19.2    |
| Vite         | 8.3     |

## Lancer en local

### Backend

```bash
cd backend
npm install
npm run start
```

Disponible sur http://localhost:3000/

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Disponible sur http://localhost:5173/
