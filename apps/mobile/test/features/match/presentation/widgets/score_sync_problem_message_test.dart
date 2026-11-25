import 'package:badminton_umpire/features/match/data/services/score_event_sync.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/header/score_sync_problem_message.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  final l10n = lookupAppLocalizations(const Locale('fr'));

  String message(String code) =>
      scoreSyncProblemMessage(l10n, ScoreSyncProblem.rejected(code));

  test('tells to pair again when the tablet is not paired', () {
    for (final code in ['TABLET_REVOKED', 'UNAUTHORIZED', 'HTTP_401']) {
      expect(message(code), l10n.scoreSyncProblemNotPaired, reason: code);
    }
    expect(message('NOT_PAIRED'), l10n.scoreSyncProblemNotPaired);
  });

  test('tells to have the match started', () {
    expect(
      message('MATCH_NOT_IN_PROGRESS'),
      l10n.scoreSyncProblemNotInProgress,
    );
  });

  test('tells when the match left the court of the tablet', () {
    for (final code in ['MATCH_NOT_ON_TABLET_COURT', 'FORBIDDEN', 'HTTP_403']) {
      expect(message(code), l10n.scoreSyncProblemWrongCourt, reason: code);
    }
  });

  test('tells when the server does not know the match', () {
    for (final code in ['MATCH_NOT_FOUND', 'HTTP_404']) {
      expect(message(code), l10n.scoreSyncProblemUnknownMatch, reason: code);
    }
  });

  test('tells when the tablet and the server disagree on the points', () {
    for (final code in [
      'SEQUENCE_ALREADY_USED',
      'EVENT_ID_CONFLICT',
      'NOTHING_CONFIRMED',
    ]) {
      expect(message(code), l10n.scoreSyncProblemOutOfStep, reason: code);
    }
    expect(
      scoreSyncProblemMessage(l10n, const ScoreSyncProblem.sequenceGap(4)),
      l10n.scoreSyncProblemOutOfStep,
    );
  });

  test('tells when a point is refused', () {
    for (final code in ['INVALID_SCORE_EVENT', 'VALIDATION_FAILED']) {
      expect(message(code), l10n.scoreSyncProblemInvalidPoint, reason: code);
    }
  });

  test('falls back to a general message for other codes', () {
    expect(message('HTTP_418'), l10n.scoreSyncProblemOther);
  });
}
