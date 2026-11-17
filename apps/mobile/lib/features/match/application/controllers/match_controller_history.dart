part of 'match_controller.dart';

extension MatchControllerHistory on MatchController {
  void _saveSnapshot() {
    _history.add(
      MatchSnapshot(
        teamAScore: teamAScore,
        teamBScore: teamBScore,
        teamASets: teamASets,
        teamBSets: teamBSets,
        servingTeamId: servingTeamId,
        serverName: serverName,
        receiverName: receiverName,
        leftSideTeamId: leftSideTeamId,
        midSetIntervalTaken: midSetIntervalTaken,
        lastSetWinnerTeamId: lastSetWinnerTeamId,
        previousSets: previousSets.map((set) => List<int>.from(set)).toList(),
        phase: phase,
        intervalRemaining: intervalRemaining,
        teamA: teamA.copy(),
        teamB: teamB.copy(),
        intervalEndsAt: intervalEndsAt,
      ),
    );
  }

  void undoLastPoint() {
    if (!canUndo) {
      return;
    }

    _intervalTimer?.cancel();
    _intervalTimer = null;

    final snapshot = _history.removeLast();
    _recordScoreEvent(ScoreEventType.undo);

    // Score
    teamAScore = snapshot.teamAScore;
    teamBScore = snapshot.teamBScore;
    teamASets = snapshot.teamASets;
    teamBSets = snapshot.teamBSets;

    // Service
    servingTeamId = snapshot.servingTeamId;
    serverName = snapshot.serverName;
    receiverName = snapshot.receiverName;

    // Position
    leftSideTeamId = snapshot.leftSideTeamId;
    teamA = snapshot.teamA.copy();
    teamB = snapshot.teamB.copy();

    // Match state
    midSetIntervalTaken = snapshot.midSetIntervalTaken;
    lastSetWinnerTeamId = snapshot.lastSetWinnerTeamId;
    phase = snapshot.phase;

    previousSets
      ..clear()
      ..addAll(snapshot.previousSets.map((set) => List<int>.from(set)));

    // Interval
    intervalRemaining = snapshot.intervalRemaining;
    intervalEndsAt = snapshot.intervalEndsAt;

    notifyMatchChanged();

    if (intervalIsRunning) {
      resumeIntervalIfNeeded();
    }
  }
}
