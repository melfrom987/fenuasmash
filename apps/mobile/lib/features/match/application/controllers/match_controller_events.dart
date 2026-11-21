part of 'match_controller.dart';

extension MatchControllerEvents on MatchController {
  void addDisciplinaryEvent({
    required String playerName,
    required MatchEventType type,
  }) {
    final teamId = getTeamIdForPlayer(playerName);

    if (teamId == null) {
      return;
    }

    _events.add(
      MatchEvent(
        type: type,
        playerName: playerName,
        teamId: teamId,
        setNumber: previousSets.length + 1,
        leftScore: leftScore,
        rightScore: rightScore,
        timestamp: DateTime.now(),
      ),
    );

    notifyMatchChanged();
  }

  void retirePlayer(String playerName) {
    if (matchIsFinished) {
      return;
    }

    final retiringTeamId = getTeamIdForPlayer(playerName);

    if (retiringTeamId == null) {
      return;
    }

    _events.add(
      MatchEvent(
        type: MatchEventType.retirement,
        playerName: playerName,
        teamId: retiringTeamId,
        setNumber: previousSets.length + 1,
        leftScore: leftScore,
        rightScore: rightScore,
        timestamp: DateTime.now(),
      ),
    );

    final winnerTeamId = _oppositeTeamId(retiringTeamId);

    _finishMatchByRetirement(winnerTeamId);
  }

  void _finishMatchByRetirement(String winnerTeamId) {
    lastSetWinnerTeamId = winnerTeamId;

    if (winnerTeamId == 'teamA') {
      teamASets = BadmintonRules.setsToWinMatch;

      if (teamBSets >= BadmintonRules.setsToWinMatch) {
        teamBSets = BadmintonRules.setsToWinMatch - 1;
      }
    } else {
      teamBSets = BadmintonRules.setsToWinMatch;

      if (teamASets >= BadmintonRules.setsToWinMatch) {
        teamASets = BadmintonRules.setsToWinMatch - 1;
      }
    }

    phase = MatchPhase.finished;

    notifyMatchChanged();
  }
}
