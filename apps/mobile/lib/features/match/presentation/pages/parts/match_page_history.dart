part of '../match_page.dart';

extension _MatchPageHistory on _MatchPageState {
  Future<void> _removeMatchFromHistory() async {
    await _historyService.deleteMatch(_matchId);
  }

  Future<void> _saveMatchHistoryIfNeeded() async {
    if (_historySaved) {
      return;
    }

    if (matchController.phase != MatchPhase.finished) {
      return;
    }

    _historySaved = true;

    final config = widget.config;

    final winnerName = matchController.teamASets > matchController.teamBSets
        ? config.teamAPlayers.join(' / ')
        : config.teamBPlayers.join(' / ');

    final entry = MatchHistoryEntry(
      id: _matchId,

      tournamentName: config.tournamentName,
      courtNumber: config.courtNumber,
      category: config.category,
      ageCategory: config.ageCategory,
      poolGroup: config.poolGroup,
      tournamentPhase: config.tournamentPhase,

      teamAPlayers: config.teamAPlayers,
      teamBPlayers: config.teamBPlayers,

      teamASets: matchController.teamASets,
      teamBSets: matchController.teamBSets,

      sets: matchController.previousSets
          .map((set) => List<int>.from(set))
          .toList(),

      winnerName: winnerName,

      umpireName: config.umpireName,
      serviceJudgeName: config.serviceJudgeName,

      duration: matchDuration,

      playedAt: _matchFinishedAt ?? DateTime.now(),
      startedAt: _matchStartedAt,
      finishedAt: _matchFinishedAt,
    );

    await _historyService.addMatch(entry);
  }
}
