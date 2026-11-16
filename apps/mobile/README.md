# FenuaSmash Mobile

Umpire app, built with Flutter for Android tablets and phones. Each tablet is linked to a court and is used to enter the scores of the matches played on it.

## Getting Started

```bash
flutter pub get
flutter run
```

Before pushing, run the same checks as CI:

```bash
dart format .
flutter analyze
flutter test
```

## Project Structure

```
lib/
├── main.dart          # Entry point
├── app/               # App widget, navigation and theme
├── core/              # Code shared by every feature: configuration, API client, common widgets
└── features/          # One folder per feature
    ├── pairing/       # Link the tablet to a court with a code
    ├── matches/       # Matches assigned to the court
    └── scoring/       # Score entry, point by point
test/                  # Tests, mirroring lib/
```

Inside a feature, files are grouped by kind:

| Folder | Content |
|---|---|
| `screens/` | Full-page widgets, one per route |
| `widgets/` | Widgets used only by this feature |
| `data/` | API calls and local storage for this feature |

A feature never imports another feature: shared code moves to `core/`.

## Further Reading

- [Design system](../../docs/design-system.md)
- [API contract](../../contracts/openapi.yaml)
- [Contributing guidelines](../../CONTRIBUTING.md)
