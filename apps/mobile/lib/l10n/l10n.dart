import 'package:flutter/widgets.dart';

import 'app_localizations.dart';

export 'app_localizations.dart';

/// Shortcut to the translations: `Text(context.l10n.matchesTitle)`.
extension AppLocalizationsContext on BuildContext {
  AppLocalizations get l10n => AppLocalizations.of(this);
}

/// French, the language of French Polynesia, when the device language is not
/// supported.
const fallbackLocale = Locale('fr');

/// Picks the first device language that the app supports, by language only
/// (`fr_PF` uses French), or French when there is none.
Locale resolveLocale(List<Locale>? deviceLocales, Iterable<Locale> supported) {
  for (final locale in deviceLocales ?? const <Locale>[]) {
    for (final candidate in supported) {
      if (candidate.languageCode == locale.languageCode) {
        return candidate;
      }
    }
  }
  return fallbackLocale;
}
