import 'package:badminton_umpire/features/match/data/services/score_event_sync.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/header/score_sync_problem_message.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

/// Explains why the points of the match are not sent, what to do, and that
/// they are kept on the tablet. The code is shown for the organizer.
///
/// [onRetry], when given, adds a button that sends the points again, e.g.
/// once the organizer has started the match.
Future<void> showScoreSyncProblemDialog({
  required BuildContext context,
  required ScoreSyncProblem problem,
  VoidCallback? onRetry,
}) {
  return showDialog<void>(
    context: context,
    builder: (dialogContext) {
      final l10n = AppLocalizations.of(dialogContext)!;
      final theme = Theme.of(dialogContext);

      return AlertDialog(
        icon: Icon(Icons.error_outline, color: theme.colorScheme.error),
        title: Text(l10n.scoreSyncProblemTitle),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(scoreSyncProblemMessage(l10n, problem)),
            const SizedBox(height: 12),
            Text(l10n.scoreSyncProblemKept),
            const SizedBox(height: 12),
            Text(
              l10n.scoreSyncProblemCode(problem.code),
              style: theme.textTheme.bodySmall?.copyWith(
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(l10n.scoreSyncProblemClose),
          ),
          if (onRetry != null)
            FilledButton(
              onPressed: () {
                Navigator.of(dialogContext).pop();
                onRetry();
              },
              child: Text(l10n.scoreSyncRetry),
            ),
        ],
      );
    },
  );
}
