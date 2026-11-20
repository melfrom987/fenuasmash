import 'package:badminton_umpire/l10n/app_localizations.dart';

enum MatchCategory {
  menSingles,
  womenSingles,
  menDoubles,
  womenDoubles,
  mixedDoubles,
}

extension MatchCategoryExtension on MatchCategory {
  bool get isDoubles {
    switch (this) {
      case MatchCategory.menSingles:
      case MatchCategory.womenSingles:
        return false;

      case MatchCategory.menDoubles:
      case MatchCategory.womenDoubles:
      case MatchCategory.mixedDoubles:
        return true;
    }
  }

  String localizedLabel(AppLocalizations l10n) {
    switch (this) {
      case MatchCategory.menSingles:
        return l10n.menSingles;

      case MatchCategory.womenSingles:
        return l10n.womenSingles;

      case MatchCategory.menDoubles:
        return l10n.menDoubles;

      case MatchCategory.womenDoubles:
        return l10n.womenDoubles;

      case MatchCategory.mixedDoubles:
        return l10n.mixedDoubles;
    }
  }
}
