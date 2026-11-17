import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/core/settings/settings.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return ListenableBuilder(
      listenable: appSettingsController,
      builder: (context, child) {
        final l10n = AppLocalizations.of(context)!;

        return Scaffold(
          backgroundColor: const Color(0xFFF5F5F8),
          appBar: AppBar(
            backgroundColor: const Color(0xFFF5F5F8),
            title: Text(
              l10n.settings,
              style: const TextStyle(fontWeight: FontWeight.w900),
            ),
          ),
          body: SafeArea(
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 700),
                child: ListView(
                  padding: EdgeInsets.all(responsive.paddingXL),
                  children: [
                    _SettingsSectionTitle(title: l10n.language),
                    SizedBox(height: responsive.spaceS),
                    _LanguageSettingCard(),
                    SizedBox(height: responsive.spaceL),
                    _SettingsSectionTitle(title: l10n.matchSettings),
                    SizedBox(height: responsive.spaceS),
                    _AnnouncementSettingCard(),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

class _SettingsSectionTitle extends StatelessWidget {
  final String title;

  const _SettingsSectionTitle({required this.title});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Text(
      title,
      style: TextStyle(
        fontSize: responsive.titleS,
        fontWeight: FontWeight.w900,
        color: Colors.grey.shade800,
      ),
    );
  }
}

class _LanguageSettingCard extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: responsive.paddingL,
        vertical: responsive.paddingS,
      ),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(responsive.radiusL),
        border: Border.all(color: const Color(0xFFE3E3EA)),
      ),
      child: Row(
        children: [
          Icon(
            Icons.language,
            color: Theme.of(context).colorScheme.primary,
            size: responsive.iconM,
          ),
          SizedBox(width: responsive.spaceM),
          Expanded(
            child: Text(
              l10n.language,
              style: TextStyle(
                fontSize: responsive.textM,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          DropdownButton<String>(
            value: appSettingsController.languageCode,
            underline: const SizedBox.shrink(),
            items: [
              DropdownMenuItem(value: 'fr', child: Text('🇫🇷 ${l10n.french}')),
              DropdownMenuItem(
                value: 'en',
                child: Text('🇬🇧 ${l10n.english}'),
              ),
            ],
            onChanged: (languageCode) {
              if (languageCode == null) {
                return;
              }

              appSettingsController.setLanguage(languageCode);
            },
          ),
        ],
      ),
    );
  }
}

class _AnnouncementSettingCard extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(responsive.radiusL),
        border: Border.all(color: const Color(0xFFE3E3EA)),
      ),
      child: SwitchListTile(
        secondary: Icon(
          Icons.record_voice_over_outlined,
          color: Theme.of(context).colorScheme.primary,
          size: responsive.iconM,
        ),
        title: Text(
          l10n.umpireAnnouncements,
          style: TextStyle(
            fontSize: responsive.textM,
            fontWeight: FontWeight.w800,
          ),
        ),
        subtitle: Text(
          l10n.umpireAnnouncementsDescription,
          style: TextStyle(
            fontSize: responsive.textS,
            color: Colors.grey.shade700,
          ),
        ),
        value: appSettingsController.showUmpireAnnouncements,
        onChanged: (value) {
          appSettingsController.setShowUmpireAnnouncements(value);
        },
      ),
    );
  }
}
