import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class UmpireAnnouncementCard extends StatelessWidget {
  final String announcement;
  final IconData icon;

  const UmpireAnnouncementCard({
    super.key,
    required this.announcement,
    this.icon = Icons.record_voice_over_outlined,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      width: double.infinity,
      padding: EdgeInsets.all(responsive.paddingM),
      decoration: BoxDecoration(
        color: colorScheme.primary.withValues(alpha: 0.07),
        borderRadius: BorderRadius.circular(responsive.radiusS),
        border: Border.all(color: colorScheme.primary.withValues(alpha: 0.20)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, size: responsive.iconM, color: colorScheme.primary),
              SizedBox(width: responsive.spaceXS),
              Flexible(
                child: Text(
                  l10n.umpireAnnouncement,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: responsive.textM,
                    fontWeight: FontWeight.w900,
                    color: colorScheme.primary,
                  ),
                ),
              ),
            ],
          ),

          SizedBox(height: responsive.spaceS),

          Text(
            '« $announcement »',
            textAlign: TextAlign.center,
            softWrap: true,
            style: TextStyle(
              fontSize: responsive.titleM,
              height: 1.25,
              fontWeight: FontWeight.w900,
              color: Colors.red,
            ),
          ),
        ],
      ),
    );
  }
}
