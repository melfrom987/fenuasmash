class AppSettings {
  final String languageCode;
  final bool showUmpireAnnouncements;

  const AppSettings({
    this.languageCode = 'fr',
    this.showUmpireAnnouncements = true,
  });

  AppSettings copyWith({String? languageCode, bool? showUmpireAnnouncements}) {
    return AppSettings(
      languageCode: languageCode ?? this.languageCode,
      showUmpireAnnouncements:
          showUmpireAnnouncements ?? this.showUmpireAnnouncements,
    );
  }
}
