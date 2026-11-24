import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_sync.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/dialogs/score_sync_problem_dialog.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/header/score_sync_problem_message.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

/// Small badge telling the umpire whether the points reached the server:
/// up to date, waiting, offline (points kept on the tablet) or blocked.
///
/// On narrow screens it only shows its icon and the number of waiting
/// points; the full text stays in the tooltip and for screen readers.
///
/// When the sending is blocked, the tooltip tells why, and a tap opens a
/// dialog that explains it and can send the points again.
class ScoreSyncIndicator extends StatelessWidget {
  /// Below this screen width, the badge uses its short form.
  static const double compactWidth = 900;

  final ScoreSyncStatus status;

  /// Events not confirmed by the server yet.
  final int pendingCount;

  /// Why the server refuses the points, when [status] is blocked.
  final ScoreSyncProblem? problem;

  /// Sends the points again, from the dialog of a blocked sending.
  final VoidCallback? onRetry;

  const ScoreSyncIndicator({
    super.key,
    required this.status,
    required this.pendingCount,
    this.problem,
    this.onRetry,
  });

  /// The problem shown is the one of [matchId] when the server refuses its
  /// points, otherwise the first one: points of a previous match can be the
  /// blocked ones.
  factory ScoreSyncIndicator.fromReport(
    ScoreSyncReport report, {
    Key? key,
    String? matchId,
    VoidCallback? onRetry,
  }) {
    final problems = report.problems;
    return ScoreSyncIndicator(
      key: key,
      status: report.status,
      pendingCount: report.pendingCount,
      problem: problems[matchId] ?? problems.values.firstOrNull,
      onRetry: onRetry,
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
        switch (problem) {
          final problem? => scoreSyncProblemMessage(l10n, problem),
          null => l10n.scoreSyncBlockedTooltip,
        },
      ),
    };

    final shortLabel = switch (status) {
      ScoreSyncStatus.pending || ScoreSyncStatus.offline => '$pendingCount',
      ScoreSyncStatus.upToDate || ScoreSyncStatus.blocked => null,
    };
    final shownLabel = compact ? shortLabel : label;
    final shownProblem = status == ScoreSyncStatus.blocked ? problem : null;
    final VoidCallback? openProblem = shownProblem == null
        ? null
        : () => showScoreSyncProblemDialog(
            context: context,
            problem: shownProblem,
            onRetry: onRetry,
          );

    return Tooltip(
      message: tooltip,
      child: Semantics(
        label: '$label. $tooltip',
        button: openProblem != null,
        onTap: openProblem,
        excludeSemantics: true,
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: openProblem,
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
      ),
    );
  }
}
