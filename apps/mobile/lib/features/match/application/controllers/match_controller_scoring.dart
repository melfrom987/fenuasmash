// ---------------------------------------------------------------------------
// AJOUT D’UN POINT
// ---------------------------------------------------------------------------
part of 'match_controller.dart';

extension MatchControllerScoring on MatchController {
  void addPointToLeftTeam() {
    addPoint(leftSideTeamId);
  }

  void addPointToRightTeam() {
    addPoint(rightSideTeamId);
  }

  void addPoint(String winningTeamId) {
    if (phase != MatchPhase.playing) {
      return;
    }

    _saveSnapshot();

    final servingTeamWon = winningTeamId == servingTeamId;

    _incrementScore(winningTeamId);

    if (servingTeamWon) {
      _handleServingTeamWon();
    } else {
      _handleReceivingTeamWon(winningTeamId);
    }

    if (_hasWonCurrentSet(winningTeamId)) {
      _finishCurrentSet(winningTeamId);
      return;
    }

    if (!midSetIntervalTaken &&
        scoreOf(winningTeamId) == BadmintonRules.midSetIntervalScore) {
      midSetIntervalTaken = true;

      if (previousSets.length == 2) {
        _swapEnds();
      }

      phase = MatchPhase.midSetIntervalReady;
      intervalRemaining = const Duration(minutes: 1);

      notifyMatchChanged();
      return;
    }

    notifyMatchChanged();
  }

  void _handleServingTeamWon() {
    final servingTeam = _teamById(servingTeamId);

    if (isDoubles) {
      servingTeam.swapPlayers();
    }

    receiverName = _diagonalPlayerOfServer();
  }

  void _handleReceivingTeamWon(String winningTeamId) {
    // Aucune paire ne permute lors du changement de service.
    servingTeamId = winningTeamId;

    serverName = _playerInRequiredServicePosition(teamId: servingTeamId);

    receiverName = _diagonalPlayerOfServer();
  }

  // ---------------------------------------------------------------------------
  // RÈGLES DU SET
  // ---------------------------------------------------------------------------

  bool _hasWonCurrentSet(String teamId) {
    final score = scoreOf(teamId);
    final opponentScore = scoreOf(_oppositeTeamId(teamId));

    final hasNormalVictory =
        score >= BadmintonRules.pointsToWinSet &&
        score - opponentScore >= BadmintonRules.pointsDifferenceToWin;

    final hasReachedMaximum = score == BadmintonRules.maxPointsPerSet;

    return hasNormalVictory || hasReachedMaximum;
  }

  void _finishCurrentSet(String winnerTeamId) {
    final finishedTeamAScore = teamAScore;
    final finishedTeamBScore = teamBScore;

    previousSets.add([finishedTeamAScore, finishedTeamBScore]);

    lastSetWinnerTeamId = winnerTeamId;

    if (winnerTeamId == 'teamA') {
      teamASets++;
    } else {
      teamBSets++;
    }

    if (teamASets == 2 || teamBSets == 2) {
      phase = MatchPhase.finished;
      notifyMatchChanged();
      return;
    }

    // Les équipes changent automatiquement de côté.
    _swapEnds();

    teamAScore = 0;
    teamBScore = 0;

    midSetIntervalTaken = false;

    phase = MatchPhase.betweenSetsIntervalReady;
    intervalRemaining = const Duration(minutes: 2);

    notifyMatchChanged();
  }

  // ---------------------------------------------------------------------------
  // SCORE ET ÉQUIPES
  // ---------------------------------------------------------------------------

  void _incrementScore(String teamId) {
    if (teamId == 'teamA') {
      teamAScore++;
    } else {
      teamBScore++;
    }
  }

  bool _wouldWinCurrentSetOnNextPoint(String teamId) {
    final score = scoreOf(teamId);
    final opponentScore = scoreOf(_oppositeTeamId(teamId));

    final nextScore = score + 1;

    final normalVictory = nextScore >= 15 && nextScore - opponentScore >= 2;

    final maximumVictory = nextScore == 21;

    return normalVictory || maximumVictory;
  }

  bool _canDisplayImportantPoint(String teamId) {
    final score = teamId == 'teamA' ? teamAScore : teamBScore;

    return score == 14 || score >= 20;
  }

  bool isSetPoint(String teamId) {
    final teamIsServing = servingTeamId == teamId;

    if (!teamIsServing) {
      return false;
    }

    return _wouldWinCurrentSetOnNextPoint(teamId);
  }

  bool hasSetPoint(String teamId) {
    return isSetPoint(teamId) && _canDisplayImportantPoint(teamId);
  }

  bool hasMatchPoint(String teamId) {
    final setsWon = teamId == 'teamA' ? teamASets : teamBSets;

    return setsWon == 1 && hasSetPoint(teamId);
  }

  bool get leftHasSetPoint {
    return hasSetPoint(leftSideTeamId) && !hasMatchPoint(leftSideTeamId);
  }

  bool get rightHasSetPoint {
    return hasSetPoint(rightSideTeamId) && !hasMatchPoint(rightSideTeamId);
  }

  bool get leftHasMatchPoint {
    return hasMatchPoint(leftSideTeamId);
  }

  bool get rightHasMatchPoint {
    return hasMatchPoint(rightSideTeamId);
  }

  bool get leftHasImportantPoint {
    return isSetPoint(leftSideTeamId);
  }

  bool get rightHasImportantPoint {
    return isSetPoint(rightSideTeamId);
  }
}
