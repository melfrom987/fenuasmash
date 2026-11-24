import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_sync.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

/// Small badge telling the umpire whether the points reached the server:
/// up to date, waiting, offline (points kept on the tablet) or blocked.
///
/// On narrow screens it only shows its icon and the number of waiting
/// points; the full text stays in the tooltip and for screen readers.
class ScoreSyncIndicator extends StatelessWidget {
  /// Below this screen width, the badge uses its short form.
  static const double compactWidth = 900;

  final ScoreSyncStatus status;

  /// Events not confirmed by the server yet.
  final int pendingCount;

  const ScoreSyncIndicator({
    super.key,
    required this.status,
    required this.pendingCount,
  });

  factory ScoreSyncIndicator.fromReport(ScoreSyncReport report, {Key? key}) {
    return ScoreSyncIndicator(
      key: key,
      status: report.status,
      pendingCount: report.pendingCount,
    );
  }

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;
    final colorScheme = Theme.of(context).colorScheme;
    final compact = MediaQuery.sizeOf(context).width < compactWidth;

    final (
      IconData icon,
      Color color,
      String label,
      String tooltip,
    ) = switch (status) {
      ScoreSyncStatus.upToDate => (
        Icons.cloud_done_outlined,
        const Color(0xFF2E7D32),
        l10n.scoreSyncUpToDate,
        l10n.scoreSyncUpToDateTooltip,
      ),
      ScoreSyncStatus.pending => (
        Icons.cloud_upload_outlined,
        colorScheme.primary,
        l10n.scoreSyncPending(pendingCount),
        l10n.scoreSyncPendingTooltip,
      ),
      ScoreSyncStatus.offline => (
        Icons.cloud_off_outlined,
        const Color(0xFFB45309),
        l10n.scoreSyncOffline(pendingCount),
        l10n.scoreSyncOfflineTooltip,
      ),
      ScoreSyncStatus.blocked => (
        Icons.error_outline,
        colorScheme.error,
        l10n.scoreSyncBlocked,
        l10n.scoreSyncBlockedTooltip,
      ),
    };

    final shortLabel = switch (status) {
      ScoreSyncStatus.pending || ScoreSyncStatus.offline => '$pendingCount',
      ScoreSyncStatus.upToDate || ScoreSyncStatus.blocked => null,
    };
    final shownLabel = compact ? shortLabel : label;

    return Tooltip(
      message: tooltip,
      child: Semantics(
        label: '$label. $tooltip',
        excludeSemantics: true,
        child: Container(
          padding: EdgeInsets.symmetric(
            horizontal: responsive.paddingS,
            vertical: responsive.paddingXS,
          ),
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.10),
            borderRadius: BorderRadius.circular(responsive.radiusM),
            border: Border.all(color: color.withValues(alpha: 0.35)),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: responsive.iconS, color: color),
              if (shownLabel != null) ...[
                SizedBox(width: responsive.spaceXS),
                Text(
                  shownLabel,
                  maxLines: 1,
                  style: TextStyle(
                    fontSize: responsive.textS,
                    fontWeight: FontWeight.w700,
                    color: color,
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
