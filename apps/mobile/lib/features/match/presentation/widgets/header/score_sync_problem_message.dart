import 'package:badminton_umpire/features/match/data/services/http_score_event_api.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_sync.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';

/// Tells the umpire why the server refuses the points of a match, and what to
/// do about it, from the `code` returned by the backend
/// (`sendScoreEvents` in `contracts/openapi.yaml`).
String scoreSyncProblemMessage(
  AppLocalizations l10n,
  ScoreSyncProblem problem,
) {
  return switch (problem.code) {
    'TABLET_REVOKED' ||
    'UNAUTHORIZED' ||
    'HTTP_401' ||
    HttpScoreEventApi.notPaired => l10n.scoreSyncProblemNotPaired,
    'MATCH_NOT_IN_PROGRESS' => l10n.scoreSyncProblemNotInProgress,
    'MATCH_NOT_ON_TABLET_COURT' ||
    'FORBIDDEN' ||
    'HTTP_403' => l10n.scoreSyncProblemWrongCourt,
    'MATCH_NOT_FOUND' || 'HTTP_404' => l10n.scoreSyncProblemUnknownMatch,
    'SEQUENCE_GAP' ||
    'SEQUENCE_ALREADY_USED' ||
    'EVENT_ID_CONFLICT' ||
    'NOTHING_CONFIRMED' => l10n.scoreSyncProblemOutOfStep,
    'INVALID_SCORE_EVENT' ||
    'VALIDATION_FAILED' => l10n.scoreSyncProblemInvalidPoint,
    _ => l10n.scoreSyncProblemOther,
  };
}
