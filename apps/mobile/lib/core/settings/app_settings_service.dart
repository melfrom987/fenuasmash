import 'package:shared_preferences/shared_preferences.dart';

import 'app_settings.dart';

class AppSettingsService {
  static const _languageKey = 'language';
  static const _showUmpireAnnouncementsKey = 'show_umpire_announcements';

  Future<AppSettings> load() async {
    final preferences = await SharedPreferences.getInstance();

    return AppSettings(
      languageCode: preferences.getString(_languageKey) ?? 'fr',
      showUmpireAnnouncements:
          preferences.getBool(_showUmpireAnnouncementsKey) ?? true,
    );
  }

  Future<void> saveLanguage(String languageCode) async {
    final preferences = await SharedPreferences.getInstance();

    await preferences.setString(_languageKey, languageCode);
  }

  Future<void> saveShowUmpireAnnouncements(bool value) async {
    final preferences = await SharedPreferences.getInstance();

    await preferences.setBool(_showUmpireAnnouncementsKey, value);
  }
}
