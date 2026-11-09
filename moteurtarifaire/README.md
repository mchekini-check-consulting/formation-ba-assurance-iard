# moteurtarifaire-api

Microservice du moteur tarifaire du projet Assurance IARD : Spring Boot 4, Java 21, PostgreSQL.
Même structure que `assurance-iard-api/` : `resource` (HTTP) → `service` (règles métier) → `repository` (données),
plus `model`, `dto`, `mapper`, `exception`, `config`, `utils`
(package racine `net.checkconsulting.moteurtarifaire.api`).

## Lancer en local

```bash
docker compose up -d postgres         # à la racine du dépôt : serveur PostgreSQL commun, port 5432, base moteurtarifaire
SERVER_PORT=8081 ./mvnw spring-boot:run
```

- API : http://localhost:8081/hello
- Santé : http://localhost:8081/actuator/health
- Documentation de l'API : http://localhost:8081/swagger-ui.html

Sur la plateforme : aucune adresse publique. Seul `assurance-iard-api` l'appelle, sur `http://moteurtarifaire:80`
(port 8080 dans l'image, base injectée par `SPRING_DATASOURCE_*`).

## Base de données

Schéma uniquement par migrations Flyway dans `src/main/resources/db/migration/`
(`V1__...sql`, appliquées au démarrage). Ne jamais modifier une migration déjà sur `main`.

## Qualité (mêmes contrôles qu'en CI)

| Commande                | Rôle |
|-------------------------|------|
| `make lint`             | analyse PMD |
| `make test-unit`        | tests + couverture JaCoCo (Docker requis) |
| `make test-integration` | tests d'intégration `*IT` (Docker requis) |

Sans `make` (Windows) : lancer les commandes `./mvnw ...` du `Makefile`. Couverture minimale : 80 %.

## API de tarification

- `POST /v1/tarifs/habitation` : calcule le tarif des 3 formules et, si demandé, le détail et la formule conseillée.
- `GET /v1/tarifs/habitation/{idTarif}` : reprend un tarif à son prix d'origine pendant sa durée de validité.
- Contrat complet : `/swagger-ui.html`.

## Changer un tarif

Aucune valeur tarifaire n'est dans le code. Tout est en base, dans deux tables :

- `grille_tarifaire` : une ligne par version, avec `date_debut` et `date_fin` (incluses ; `date_fin` vide = toujours en vigueur) ;
- `parametre_tarifaire` : les valeurs de chaque version (`categorie`, `cle`, `valeur`).

Pour changer un tarif, écrire une nouvelle migration Flyway, ou le même SQL dans un futur outil d'administration :

```sql
-- 1. Fermer la version en cours
update grille_tarifaire set date_fin = date '2027-06-30' where version = '2026.1';

-- 2. Créer la nouvelle version
insert into grille_tarifaire (version, date_debut) values ('2027.1', date '2027-07-01');

-- 3. Copier tous les paramètres
insert into parametre_tarifaire (grille_id, categorie, cle, valeur)
select nouvelle.id, p.categorie, p.cle, p.valeur
from parametre_tarifaire p
join grille_tarifaire ancienne on ancienne.id = p.grille_id and ancienne.version = '2026.1'
cross join grille_tarifaire nouvelle
where nouvelle.version = '2027.1';

-- 4. Changer ce qui évolue
update parametre_tarifaire set valeur = '11.50'
where categorie = 'prix-base' and cle = 'CONFORT'
  and grille_id = (select id from grille_tarifaire where version = '2027.1');
```

- Les zones (`categorie = zone`) doivent déclarer tous les départements : un code postal sans ligne correspondante est refusé.
- Ne jamais modifier les paramètres d'une version déjà utilisée : les calculs passés y font référence.
- Une grille est relue à chaque calcul : une écriture directe en base s'applique sans redémarrer.
- Une grille incomplète ou des périodes qui se chevauchent empêchent le service de démarrer. Si cela arrive pendant qu'il tourne, le calcul répond 503 au lieu de donner un prix faux.

Les tests utilisent leur propre grille (`JeuxDeDonnees` dans `src/test/`) : ils ne changent pas avec le tarif.
