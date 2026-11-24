import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/core/utils/formatters.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/banners/umpire_announcement_card.dart';
import 'package:flutter/material.dart';

class CountdownBanner extends StatelessWidget {
  final IconData icon;
  final String title;

  final Duration remaining;
  final bool started;

  final String startLabel;
  final String skipLabel;

  final String? announcement;

  final VoidCallback onStart;
  final VoidCallback onSkip;

  const CountdownBanner({
    super.key,
    required this.icon,
    required this.title,
    required this.remaining,
    required this.started,
    required this.startLabel,
    required this.skipLabel,
    required this.onStart,
    required this.onSkip,
    this.announcement,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final colorScheme = Theme.of(context).colorScheme;
    final isUrgent =
        started && remaining.inSeconds <= 20 && remaining.inSeconds > 0;

    return Material(
      elevation: 3,
      borderRadius: BorderRadius.circular(responsive.radiusM),
      child: Container(
        constraints: BoxConstraints(maxWidth: responsive.dialogWidthL),
        padding: EdgeInsets.symmetric(
          horizontal: responsive.paddingM,
          vertical: responsive.paddingS,
        ),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(responsive.radiusM),
          border: Border.all(
            color: colorScheme.primary.withValues(alpha: 0.25),
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  icon,
                  size: responsive.iconM,
                  color: isUrgent ? Colors.red : colorScheme.primary,
                ),

                SizedBox(width: responsive.spaceS),

                Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontSize: responsive.textXS,
                        fontWeight: FontWeight.w700,
                        color: Colors.grey.shade600,
                      ),
                    ),

                    Text(
                      AppFormatters.interval(remaining),
                      style: TextStyle(
                        fontSize: responsive.titleM,
                        fontWeight: FontWeight.w900,
                        color: isUrgent ? Colors.red : Colors.black87,
                      ),
                    ),
                  ],
                ),

                SizedBox(width: responsive.spaceM),

                if (!started) ...[
                  FilledButton.icon(
                    onPressed: onStart,
                    icon: Icon(Icons.play_arrow, size: responsive.iconS),
                    label: Text(startLabel),
                  ),

                  SizedBox(width: responsive.spaceS),
                ],

                TextButton(onPressed: onSkip, child: Text(skipLabel)),
              ],
            ),

            if (announcement != null) ...[
              SizedBox(height: responsive.spaceS),

              UmpireAnnouncementCard(announcement: announcement!),
            ],
          ],
        ),
      ),
    );
  }
}
