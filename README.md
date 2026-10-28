# FenuaSmash

Gestion et suivi de compétitions de badminton : administration des tournois, arbitrage sur tablette et diffusion des scores en temps réel. Projet Epitech 2026–2027, conçu pour la Polynésie française.

## Structure du dépôt

| Dossier | Contenu | Techno |
|---|---|---|
| `apps/web` | Administration et affichage public | React / TypeScript |
| `apps/api` | API, modèle de données, temps réel | Spring Boot 4 / Java 25 (Maven) |
| `apps/mobile` | Application d'arbitrage | Flutter |
| `contracts` | Contrat d'API partagé (OpenAPI) | — |
| `docs` | Documentation technique, installation, utilisateur | — |

## Démarrage rapide

Prérequis : Git, Docker, JDK 25 (optionnel avec Docker), Node.js (LTS), Flutter.

```bash
git clone git@github.com:fenuasmash/fenuasmash.git
cd fenuasmash
make env      # crée .env depuis .env.example
make db       # base de données PostgreSQL
make stack    # ou : stack complète (base + API + web) en conteneurs
```

Chaque application a ensuite son propre README pour la lancer.

**Pour lancer le projet et comprendre la CI/CD :** [DevOps](docs/devops.md) · [Docker](docs/docker.md) · [CI/CD](docs/ci-cd.md) · [Serveur](docs/installation.md)

## CI/CD

| Workflow | Déclencheur | Rôle |
|---|---|---|
| `CI` | PR, push sur `dev`/`main` | Lint, types, tests, build par app, images Docker, test de fumée |
| `CD` | push sur `main`, tag `v*` | Images sur GHCR, déploiement, release APK |
| `Titre de PR` | PR | Format Conventional Commits |

Choix justifiés dans [docs/adr/0002-docker-ci-cd.md](docs/adr/0002-docker-ci-cd.md).

## Contribuer

Lire [CONTRIBUTING.md](CONTRIBUTING.md) avant la première branche.

## Équipe

| Membre | Rôle |
|---|---|
| Mélissa Mi You | Interface web et administration |
| Raphael Vaz | À compléter |
| Emeric Somnard | À compléter |
