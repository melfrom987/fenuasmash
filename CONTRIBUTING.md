# Contribuer à FenuaSmash

## Règle d'or

On ne pousse jamais directement sur `main`. Tout passe par une issue, une branche et une pull request relue par au moins une autre personne.

## 1. Une issue d'abord

Chaque tâche a une issue dans GitHub Projects, avec un label de périmètre (`web`, `api`, `mobile`, `infra`, `docs`) et un label de type (`feature`, `bug`, `chore`).

## 2. Nommer sa branche

```
<type>/<numéro-issue>-<description-courte>
```

| Type | Usage |
|---|---|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction de bug |
| `chore` | Configuration, dépendances, outillage |
| `docs` | Documentation |
| `refactor` | Réorganisation sans changement de comportement |
| `test` | Ajout ou correction de tests |

Exemples : `feat/12-court-board`, `fix/27-score-sync`, `chore/3-web-init`.

```bash
git switch main
git pull
git switch -c feat/12-court-board
```

## 3. Messages de commit

Format [Conventional Commits](https://www.conventionalcommits.org/fr/), avec le périmètre entre parenthèses :

```
feat(web): add court board page
fix(api): reject events from revoked devices
chore(mobile): upgrade flutter dependencies
docs: add installation guide
```

## 4. Pull request

- Titre au même format qu'un commit.
- `Closes #12` dans la description pour fermer l'issue automatiquement.
- Remplir le modèle de PR.
- Au moins une relecture approuvée avant de merger.
- Merge en **squash** : un commit propre par PR sur `main`.

## 5. Garder sa branche à jour

```bash
git switch main
git pull
git switch feat/12-court-board
git rebase main
git push --force-with-lease
```

## Contrat d'API

Toute modification de l'API passe d'abord par `contracts/openapi.yaml`, dans une PR relue par le web **et** le mobile. Voir [contracts/README.md](contracts/README.md).
