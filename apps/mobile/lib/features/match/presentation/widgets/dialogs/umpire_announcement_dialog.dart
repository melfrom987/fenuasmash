import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

Future<bool> showUmpireAnnouncementDialog({
  required BuildContext context,
  required String announcement,
  String? buttonLabel,
  IconData icon = Icons.record_voice_over_outlined,
  bool dismissible = false,
  bool showButton = true,
  Duration? autoCloseDuration,
}) async {
  final result = await showDialog<bool>(
    context: context,
    barrierDismissible: dismissible,
    builder: (dialogContext) {
      final l10n = AppLocalizations.of(dialogContext)!;
      final responsive = Responsive.of(dialogContext);
      final colorScheme = Theme.of(dialogContext).colorScheme;

      if (autoCloseDuration != null) {
        Future.delayed(autoCloseDuration, () {
          if (dialogContext.mounted) {
            Navigator.of(dialogContext).pop(true);
          }
        });
      }

      return Dialog(
        insetPadding: EdgeInsets.symmetric(
          horizontal: responsive.spaceM,
          vertical: responsive.spaceS,
        ),
        child: ConstrainedBox(
          constraints: BoxConstraints(maxWidth: responsive.dialogWidthL),
          child: Padding(
            padding: EdgeInsets.all(responsive.paddingM),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: double.infinity,
                  padding: EdgeInsets.all(responsive.paddingM),
                  decoration: BoxDecoration(
                    color: colorScheme.primary.withValues(alpha: 0.07),
                    borderRadius: BorderRadius.circular(responsive.radiusS),
                    border: Border.all(
                      color: colorScheme.primary.withValues(alpha: 0.20),
                    ),
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            icon,
                            size: responsive.iconM,
                            color: colorScheme.primary,
                          ),
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

                      SizedBox(height: responsive.spaceM),

                      Text(
                        '« $announcement »',
                        textAlign: TextAlign.center,
                        softWrap: true,
                        style: TextStyle(
                          fontSize: responsive.textM,
                          height: 1.3,
                          fontWeight: FontWeight.w800,
                          color: Colors.red,
                        ),
                      ),
                    ],
                  ),
                ),

                if (showButton) ...[
                  SizedBox(height: responsive.spaceM),
                  SizedBox(
                    width: double.infinity,
                    height: responsive.buttonHeightM,
                    child: FilledButton.icon(
                      onPressed: () {
                        Navigator.of(dialogContext).pop(true);
                      },
                      icon: Icon(Icons.check, size: responsive.iconM),
                      label: Text(buttonLabel ?? l10n.confirm),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      );
    },
  );

  return result ?? false;
}
