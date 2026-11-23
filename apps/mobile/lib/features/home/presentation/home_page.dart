import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/settings/presentation/pages/settings_page.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F8),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 700),
            child: Padding(
              padding: EdgeInsets.all(responsive.paddingXL),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          l10n.appTitle,
                          style: TextStyle(
                            fontSize: responsive.titleL,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ),
                      IconButton(
                        tooltip: l10n.settings,
                        icon: Icon(
                          Icons.settings_outlined,
                          size: responsive.iconM,
                        ),
                        onPressed: () {
                          Navigator.of(context).push(
                            MaterialPageRoute<void>(
                              builder: (context) {
                                return const SettingsPage();
                              },
                            ),
                          );
                        },
                      ),
                    ],
                  ),
                  const Spacer(),
                  Icon(
                    Symbols.badminton,
                    size: responsive.scale(min: 60, max: 90),
                    color: Theme.of(context).colorScheme.primary,
                  ),
                  SizedBox(height: responsive.spaceL),
                  Text(
                    l10n.appTitle,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: responsive.titleXL,
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  const Spacer(),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
