import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/match_setup/presentation/pages/match_setup_page.dart';
import 'package:badminton_umpire/features/settings/presentation/pages/settings_page.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  Future<void> _openMatchSetup(BuildContext context) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (context) {
          return MatchSetupPage();
        },
      ),
    );
  }

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
                  SizedBox(height: responsive.spaceXL),
                  _HomeActionCard(
                    icon: Icons.add_circle_outline,
                    title: l10n.newMatch,
                    description: l10n.newMatchDescription,
                    onTap: () => _openMatchSetup(context),
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

class _HomeActionCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;
  final VoidCallback onTap;

  const _HomeActionCard({
    required this.icon,
    required this.title,
    required this.description,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(responsive.radiusL),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(responsive.radiusL),
        child: Container(
          padding: EdgeInsets.all(responsive.paddingL),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(responsive.radiusL),
            border: Border.all(color: const Color(0xFFE3E3EA)),
          ),
          child: Row(
            children: [
              Container(
                width: responsive.scale(min: 48, max: 60),
                height: responsive.scale(min: 48, max: 60),
                decoration: BoxDecoration(
                  color: primaryColor.withValues(alpha: 0.10),
                  borderRadius: BorderRadius.circular(responsive.radiusM),
                ),
                child: Icon(icon, color: primaryColor, size: responsive.iconL),
              ),
              SizedBox(width: responsive.spaceM),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontSize: responsive.titleS,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    SizedBox(height: responsive.spaceXXS),
                    Text(
                      description,
                      style: TextStyle(
                        fontSize: responsive.textS,
                        color: Colors.grey.shade700,
                      ),
                    ),
                  ],
                ),
              ),
              SizedBox(width: responsive.spaceS),
              Icon(
                Icons.chevron_right,
                size: responsive.iconM,
                color: Colors.grey.shade500,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
