# Contrat d'API

`openapi.yaml` (à venir) décrit toutes les routes de l'API : entrées, sorties, erreurs. C'est la **source de vérité unique** entre le backend, le web et le mobile.

- Le **web** génère ses types TypeScript depuis ce fichier.
- Le **mobile** génère son client Dart depuis ce fichier.
- Le **backend** implémente exactement ce qui est décrit.

## Règles

1. On modifie le contrat **avant** le code.
2. Toute PR qui touche `contracts/` est relue par les trois membres.
3. Les dates sont toujours en UTC, au format ISO 8601. L'affichage se fait en heure de Tahiti (`Pacific/Tahiti`).
4. Les identifiants sont des UUID.
