# FenuaSmash

Gestion et suivi de compétitions de badminton : administration des tournois, arbitrage sur tablette et diffusion des scores en temps réel. Projet Epitech 2026–2027, conçu pour la Polynésie française.

## Structure du dépôt

| Dossier | Contenu | Techno |
|---|---|---|
| `apps/web` | Administration et affichage public | React / TypeScript |
| `apps/api` | API, modèle de données, temps réel | Node.js / TypeScript |
| `apps/mobile` | Application d'arbitrage | Flutter |
| `contracts` | Contrat d'API partagé (OpenAPI) | — |
| `docs` | Documentation technique, installation, utilisateur | — |

## Démarrage rapide

Prérequis : Git, Docker, Node.js (LTS), Flutter.

```bash
git clone git@github.com:fenuasmash/fenuasmash.git
cd fenuasmash
cp .env.example .env
docker compose up -d        # base de données PostgreSQL
```

Chaque application a ensuite son propre README pour la lancer.

## Contribuer

Lire [CONTRIBUTING.md](CONTRIBUTING.md) avant la première branche.

## Équipe

| Membre | Rôle |
|---|---|
| Mélissa Mi You | Interface web et administration |
| Raphael Vaz | À compléter |
| Emeric Somnard | À compléter |
