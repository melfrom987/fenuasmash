part of '../match_page.dart';

extension _MatchPageDraft on _MatchPageState {
  Future<void> _saveTemporaryMatch() async {
    if (_savingDraft) {
      _draftSavePending = true;
      return;
    }

    _savingDraft = true;

    try {
      do {
        _draftSavePending = false;

        final draft = MatchDraft(
          matchId: _matchId,
          config: widget.config,

          teamAScore: matchController.teamAScore,
          teamBScore: matchController.teamBScore,
          teamASets: matchController.teamASets,
          teamBSets: matchController.teamBSets,

          servingTeamId: matchController.servingTeamId,
          serverName: matchController.serverName,
          receiverName: matchController.receiverName,
          leftSideTeamId: matchController.leftSideTeamId,

          teamA: matchController.teamA.copy(),
          teamB: matchController.teamB.copy(),

          midSetIntervalTaken: matchController.midSetIntervalTaken,
          lastSetWinnerTeamId: matchController.lastSetWinnerTeamId,

          previousSets: matchController.previousSets
              .map((set) => List<int>.from(set))
              .toList(),

          phase: matchController.phase,
          intervalRemaining: matchController.intervalRemaining,

          history: List<MatchSnapshot>.from(matchController.history),
          events: List<MatchEvent>.from(matchController.events),
          scoreEvents: List<ScoreEvent>.from(matchController.scoreEvents),

          matchStarted: _matchStarted,
          matchStartedAt: _matchStartedAt,
          matchFinishedAt: _matchFinishedAt,

          warmupStarted: _warmupStarted,
          warmupEndsAt: _warmupEndsAt,

          shuttleCount: shuttleCount,
          playerColors: Map<String, Color>.from(playerColors),

          savedAt: DateTime.now(),
        );

        await _draftService.save(draft);
        await _queueScoreEvents();
      } while (_draftSavePending);
    } finally {
      _savingDraft = false;
    }
  }

  /// Puts the new points and undos of a match called to the court in the
  /// queue of events to send, under its server id. They stay there after the
  /// current match is cleared, until the backend confirms them. A failure is
  /// retried with the next save, as queuing is idempotent.
  ///
  /// A match created on the tablet is unknown to the server: its events are
  /// not queued.
  Future<void> _queueScoreEvents() async {
    final serverMatchId = widget.config.serverMatchId;
    if (serverMatchId == null) {
      return;
    }
    try {
      await _scoreEventOutbox.enqueue(
        serverMatchId,
        matchController.scoreEvents,
      );
    } catch (error) {
      debugPrint('Could not queue score events: $error');
      return;
    }
    final scoreSync = _scoreSync;
    if (scoreSync != null) {
      unawaited(scoreSync.syncNow());
    }
  }

  /// Badge showing whether the points reached the server, or null for a
  /// match created on the tablet.
  Widget? _buildSyncIndicator() {
    final scoreSync = _scoreSync;
    if (scoreSync == null) {
      return null;
    }
    return ValueListenableBuilder<ScoreSyncReport?>(
      valueListenable: scoreSync.lastReport,
      builder: (context, report, _) {
        if (report == null) {
          return const SizedBox.shrink();
        }
        return ScoreSyncIndicator.fromReport(report);
      },
    );
  }

  void _scheduleTemporarySave() {
    _draftDebounceTimer?.cancel();

    _draftDebounceTimer = Timer(const Duration(milliseconds: 400), () {
      unawaited(_saveTemporaryMatch());
    });
  }
}
