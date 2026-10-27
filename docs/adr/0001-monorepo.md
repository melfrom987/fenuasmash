# ADR 0001 — Un seul dépôt pour les trois applications

- Statut : accepté
- Date : 2026-10-27

## Contexte

Le système comporte trois applications (web, API, mobile) qui partagent un contrat d'API et doivent rester cohérentes.

## Décision

Un monorepo : `apps/web`, `apps/api`, `apps/mobile`, et un dossier `contracts/` pour le contrat OpenAPI.

## Conséquences

- Une modification du contrat et du code associé peut se faire dans la même PR.
- Une seule configuration Docker et CI pour l'ensemble.
- La CI doit filtrer par dossier pour ne lancer que les vérifications utiles.
