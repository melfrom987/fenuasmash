import 'package:flutter/material.dart';

import 'app_settings.dart';
import 'app_settings_service.dart';

class AppSettingsController extends ChangeNotifier {
  final AppSettingsService _service;

  AppSettingsController({AppSettingsService? service})
    : _service = service ?? AppSettingsService();

  AppSettings _settings = const AppSettings();

  AppSettings get settings => _settings;

  Locale get locale => Locale(_settings.languageCode);

  String get languageCode => _settings.languageCode;

  bool get showUmpireAnnouncements => _settings.showUmpireAnnouncements;

  Future<void> load() async {
    _settings = await _service.load();
    notifyListeners();
  }

  Future<void> setLanguage(String languageCode) async {
    if (_settings.languageCode == languageCode) {
      return;
    }

    _settings = _settings.copyWith(languageCode: languageCode);

    notifyListeners();

    await _service.saveLanguage(languageCode);
  }

  Future<void> setShowUmpireAnnouncements(bool value) async {
    if (_settings.showUmpireAnnouncements == value) {
      return;
    }

    _settings = _settings.copyWith(showUmpireAnnouncements: value);

    notifyListeners();

    await _service.saveShowUmpireAnnouncements(value);
  }
}
