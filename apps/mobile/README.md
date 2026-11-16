# FenuaSmash Mobile

Umpire app, built with Flutter for Android tablets and phones. Each tablet is linked to a court and is used to enter the scores of the matches played on it.

## Getting Started

```bash
flutter pub get
flutter run
```

The app calls the API started by `make api` at the repository root. It reaches it at `http://10.0.2.2:8081`, the address of your computer from the Android emulator.

To use another server, pass its root URL, **without `/api`** (the app adds it):

```bash
flutter run --dart-define=API_URL=http://192.168.1.20:8081   # real tablet on the same Wi-Fi
flutter build apk --release --dart-define=API_URL=https://fenuasmash.example
```

Debug builds accept plain HTTP; release builds need HTTPS. The release APK built by CD uses the `MOBILE_API_URL` repository variable.

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

A feature never imports another feature: shared code moves to `core/`. To open another screen, use its path from `core/routing/app_routes.dart` (`context.go(AppRoutes.matches)`); the routes themselves are declared in `app/router.dart`.

## Design

The app follows the [design system](../../docs/design-system.md):

- Colors are in `lib/app/theme/app_colors.dart` (`AppColors.lagon.shade600`…), the same values as the web app. Use the theme (`Theme.of(context).colorScheme`) rather than raw colors when possible.
- Fonts (Outfit for headings, Inter for the rest, weights 400 and 600) are bundled in `assets/fonts` with their licenses, so the app works offline.
- Scores use `AppTextStyles.score(fontSize: …)`, with tabular figures.
- Icons use `AppIcon(HugeIcons.strokeRounded…)`, which applies the design system size and stroke width.

## Further Reading

- [Design system](../../docs/design-system.md)
- [API contract](../../contracts/openapi.yaml)
- [Contributing guidelines](../../CONTRIBUTING.md)
