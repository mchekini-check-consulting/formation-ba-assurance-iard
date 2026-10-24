---
name: deploy
description: Gère tout le cycle de mise en ligne pour un Business Analyst (BA) qui ne maîtrise pas git — vérifie lint/gitleaks/tests/quality gate Sonar en local, commit, push, attend que les contrôles GitHub passent au vert, merge la PR vers main (avec confirmation), puis suit le pipeline (build image, promotion Kargo, synchronisation ArgoCD) jusqu'au déploiement effectif sur dev. À utiliser quand le BA dit qu'il a terminé son développement et veut déployer / mettre en ligne / passer en dev.
---

# Déployer sur dev

Public : un Business Analyst, pas un développeur. Expliquer chaque étape en
langage simple (pas de jargon git brut non expliqué). Ne jamais merger ou
forcer un push sans confirmation explicite à l'étape prévue pour ça.

Rappel du pipeline réel de ce dépôt (cf. `ci-template/README.md` et
`chart-base/kargo-pipelines/README.md`) : merge sur `main` → build de l'image
et push vers ECR (`ci.yaml`) → Kargo détecte la nouvelle image et la promeut
automatiquement vers la Stage `dev` (il écrit le tag dans `helm.yaml` et
pousse un commit sur `main`) → ArgoCD synchronise l'Application `dev-<repo>`.
Le déploiement sur dev est donc **automatique après le merge** : le rôle de
ce skill est de sécuriser tout ce qui précède le merge, puis de vérifier que
la suite se déroule bien.

## Étapes

### 1. Vérifier qu'il y a bien quelque chose à déployer

- `git branch --show-current`. Si c'est `main` : s'arrêter, expliquer qu'il
  n'y a pas de fonctionnalité en cours et qu'il faut d'abord utiliser le
  skill `nouvelle-fonctionnalite`.
- Repérer les dossiers de composants modifiés (dossiers contenant un
  `Dockerfile` touchés par le diff avec `main`) :
  `git diff --name-only origin/main...HEAD` + `git status --porcelain`
  (fichiers non committés inclus).

### 2. Contrôles locaux — avant tout commit/push

Pour chaque composant modifié, dans l'ordre, en s'arrêtant au premier échec :

1. `make lint`
2. `make test-unit`
3. `make test-integration`
4. Si la commande `gitleaks` est disponible (`command -v gitleaks`) :
   `gitleaks detect --source . -v --no-banner`. Si absent, ne pas bloquer —
   dire simplement que GitHub le vérifiera à la prochaine étape.

En cas d'échec : traduire l'erreur en langage clair (quel fichier, quel test,
quelle règle), s'arrêter et proposer de l'aide pour corriger. Ne jamais
committer/pousser du code dont les contrôles locaux échouent.

Note sur le quality gate SonarQube : il ne peut pas être vérifié fiablement
en local (nécessite le serveur SonarQube). Le dire au BA : "le dernier
contrôle (qualité du code via SonarQube) se fera automatiquement une fois le
code envoyé sur GitHub, je le surveillerai."

### 3. Commit et push

- Si rien n'est committé : demander au BA une courte description de ce qu'il
  a fait (sert de message de commit ; à défaut, utiliser le nom de la
  fonctionnalité donné au skill `nouvelle-fonctionnalite`).
- `git add -A`
- `git commit -m "<message>"`
- `git push -u origin <branche-courante>`
  - Si le push échoue parce que la branche distante a avancé (quelqu'un/
    quelque chose a poussé depuis) : `git fetch origin` puis tenter
    `git rebase origin/<branche-courante>`.
    - Rebase propre → `git push --force-with-lease`.
    - Conflit de rebase : **ne jamais** demander au BA de résoudre des
      marqueurs `<<<<<<<` à la main. `git rebase --abort`, puis expliquer
      simplement quel(s) fichier(s) posent un conflit et proposer un choix
      binaire par fichier ("garder votre version" / "garder celle qui est
      déjà sur la branche"), appliquer ce choix via
      `git checkout --ours|--theirs -- <fichier>` puis `git rebase --continue`.
      Si le BA ne sait pas trancher, s'arrêter et demander de l'aide plutôt
      que de deviner.

### 4. S'assurer qu'une PR existe

- `gh pr view <branche-courante>` ; si aucune PR : la créer
  (`gh pr create --base main --head <branche-courante> --title ... --body ...`).
- Si une PR existe en brouillon : `gh pr ready <branche-courante>`.

### 5. Attendre que tout soit vert sur GitHub

- `gh pr checks <branche-courante> --watch`
- Un contrôle rouge (lint, gitleaks, quality gate Sonar, tests) : récupérer le
  détail (`gh run view --log-failed`), traduire en langage simple ce qui a
  échoué, s'arrêter. Ne pas relancer en boucle à l'aveugle.

### 6. Merge — confirmation obligatoire

Une fois tout vert et aucun conflit en attente :

- Résumer au BA en une phrase ce qui va se passer : "Tout est vérifié ✅. Si
  vous confirmez, votre travail va être intégré à la version principale, ce
  qui déclenchera automatiquement sa mise en ligne sur dev. La branche
  `<nom>` sera ensuite supprimée. Je confirme ?"
- **Attendre une confirmation explicite** avant de merger. Ne jamais merger
  automatiquement.
- Sur confirmation : `gh pr merge <branche-courante> --merge --delete-branch`
  (merge classique, pas de squash : convention déjà utilisée dans l'historique
  de ce dépôt — commits "Merge pull request #N").
- `git switch main && git pull --ff-only`

### 7. Suivre le déploiement jusqu'à dev (meilleur effort)

- Build de l'image : `gh run list --branch main --limit 1` puis
  `gh run watch <id>` pour `ci.yaml`.
- Promotion Kargo vers la Stage dev : si `kubectl` est configuré sur le
  cluster, surveiller les ressources Kargo du Project concerné
  (`kubectl get promotions,stages -n <namespace-du-projet>`) jusqu'à ce que
  la promotion vers `dev` soit terminée. Si pas d'accès cluster depuis cette
  session, le dire simplement et indiquer que Kargo fait ça automatiquement
  en quelques minutes (scrute ECR périodiquement).
- Synchronisation ArgoCD : si accessible (`argocd` CLI ou `kubectl`),
  vérifier que l'Application `dev-<repo>` est `Synced` + `Healthy`. Sinon,
  l'indiquer au BA avec l'URL de l'UI ArgoCD dev pour vérifier lui-même.
- Si une étape échoue (build, promotion bloquée, synchro en échec) : remonter
  la cause en langage clair, ne pas tenter de réparer seul sans en parler au
  BA (ou à son encadrant technique) si ça touche à l'infrastructure.

### 8. Conclure

Rapport final en langage simple : ce qui a été mis en ligne, l'URL de
l'environnement dev, et le statut (ok / en cours / à surveiller).

## Ce que ce skill ne fait jamais

- Ne merge jamais sans confirmation explicite à l'étape 6.
- Ne résout jamais un conflit en modifiant le contenu du code à la place du
  BA — seulement des choix "garder ma version / garder l'autre" par fichier,
  validés par le BA.
- Ne force jamais un push sans qu'un rebase propre ait précédé.
- Ne relance jamais une CI en échec en boucle sans comprendre la cause.
