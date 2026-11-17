import 'package:badminton_umpire/core/settings/settings.dart';
import 'package:badminton_umpire/features/home/presentation/home_page.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class BadmintonUmpireApp extends StatelessWidget {
  const BadmintonUmpireApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: appSettingsController,
      builder: (context, child) {
        return MaterialApp(
          debugShowCheckedModeBanner: false,

          locale: appSettingsController.locale,

          localizationsDelegates: AppLocalizations.localizationsDelegates,

          supportedLocales: AppLocalizations.supportedLocales,

          onGenerateTitle: (context) {
            return AppLocalizations.of(context)!.appTitle;
          },

          builder: (context, child) {
            final mediaQuery = MediaQuery.of(context);

            final clampedTextScaler = mediaQuery.textScaler.clamp(
              minScaleFactor: 1.0,
              maxScaleFactor: 1.20,
            );

            return MediaQuery(
              data: mediaQuery.copyWith(textScaler: clampedTextScaler),
              child: child!,
            );
          },

          home: const HomePage(),
        );
      },
    );
  }
}
