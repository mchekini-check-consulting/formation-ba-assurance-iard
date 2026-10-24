# microservice1

Microservice backend NestJS — squelette minimal ("Hello World"), sans
module métier. À compléter.

## Commandes

```bash
npm install
npm run start:dev      # nécessite la base locale : docker compose up -d
```

## Contrôles (identiques à ceux lancés par la CI)

```bash
make lint
make test-unit
make test-integration   # Docker requis (PostgreSQL jetable)
```
