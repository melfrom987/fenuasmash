# Localization and Settings

## Localization

The application uses Flutter's generated localization system with ARB source files:

```text
lib/l10n/app_en.arb
lib/l10n/app_fr.arb
```

Generated Dart localization files are also present under `lib/l10n/`.

UI code obtains localized strings through:

```dart
final l10n = AppLocalizations.of(context)!;
```

and then uses keys such as:

```dart
Text(l10n.settings)
```

## Adding a translation

1. Add the same key to both `app_en.arb` and `app_fr.arb`.
2. Use the generated getter from `AppLocalizations`.
3. Regenerate localization files if required.
4. Run analysis.

```bash
flutter gen-l10n
dart format .
flutter analyze
```

Avoid hard-coded user-facing French or English text inside widgets once the UI wording is stable.

## Removing translations

`flutter analyze` does not report unused ARB keys. Before deleting a key, search the whole `lib/` tree for its generated getter usage.

Generated `app_localizations*.dart` files should not be counted as real usages when checking whether an ARB key is referenced by application code.

## Global settings

`AppSettings` currently contains:

- `languageCode`;
- `showUmpireAnnouncements`.

`AppSettingsService` persists settings through `SharedPreferences`.

`AppSettingsController` exposes the current settings through `ChangeNotifier` and saves changes immediately.

A single global instance is exported from `core/settings/settings.dart`.

## Root application reaction

`BadmintonUmpireApp` listens to `appSettingsController` with `ListenableBuilder`. Changing the language updates the `MaterialApp.locale`, causing localized widgets to rebuild with the selected language.

## Umpire announcements

The announcement preference controls whether phrases intended to be spoken by the umpire are displayed. It should not change badminton rules or block required match transitions.

For example, disabling announcements may skip an announcement dialog, but it must not skip:

- warm-up timing;
- interval timing;
- service/receiver setup;
- scoring transitions;
- required haptic or state-management behavior.

## What belongs in settings?

Application-wide preferences belong here. Match-specific state does not.

Good examples:

- language;
- announcement visibility;
- future accessibility preferences.

Poor examples:

- current server;
- current score;
- player colors for one match;
- current court side.
