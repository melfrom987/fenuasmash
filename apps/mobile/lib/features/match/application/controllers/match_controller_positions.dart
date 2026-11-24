part of 'match_controller.dart';

extension MatchControllerPositions on MatchController {
  void _initializePositions() {
    teamA = _createInitialTeamState(teamId: 'teamA', players: teamAPlayers);

    teamB = _createInitialTeamState(teamId: 'teamB', players: teamBPlayers);

    _placeInitialServerAndReceiver();
  }

  TeamCourtState _createInitialTeamState({
    required String teamId,
    required List<String> players,
  }) {
    final validPlayers = players
        .map((player) => player.trim())
        .where((player) => player.isNotEmpty)
        .toList();

    if (validPlayers.isEmpty) {
      throw StateError('No valid player was provided for $teamId.');
    }

    if (validPlayers.length == 1) {
      return TeamCourtState(
        teamId: teamId,
        players: List<String>.from(validPlayers),
        topPlayer: validPlayers.first,
        bottomPlayer: '',
      );
    }

    return TeamCourtState(
      teamId: teamId,
      players: List<String>.from(validPlayers),
      topPlayer: validPlayers[0],
      bottomPlayer: validPlayers[1],
    );
  }

  void _placeInitialServerAndReceiver() {
    final servingTeam = _teamById(servingTeamId);
    final receivingTeam = _oppositeTeam(servingTeamId);

    if (!isDoubles) {
      serverName = servingTeam.players.first;
      receiverName = receivingTeam.players.first;
      return;
    }

    if (!servingTeam.players.contains(serverName)) {
      serverName = servingTeam.players.first;
    }

    if (!receivingTeam.players.contains(receiverName)) {
      receiverName = receivingTeam.players.first;
    }

    final servingTeamIsOnLeft = servingTeamId == leftSideTeamId;

    final serverMustBeTop = !servingTeamIsOnLeft;

    _placePlayer(
      team: servingTeam,
      playerName: serverName,
      mustBeTop: serverMustBeTop,
    );

    _placePlayer(
      team: receivingTeam,
      playerName: receiverName,
      mustBeTop: !serverMustBeTop,
    );
  }

  void _placePlayer({
    required TeamCourtState team,
    required String playerName,
    required bool mustBeTop,
  }) {
    if (!team.isDoubles) {
      return;
    }

    final validPlayers = team.players
        .where((player) => player.trim().isNotEmpty)
        .toList();

    if (validPlayers.length < 2) {
      return;
    }

    if (!validPlayers.contains(playerName)) {
      return;
    }

    final otherPlayers = validPlayers
        .where((player) => player != playerName)
        .toList();

    if (otherPlayers.isEmpty) {
      return;
    }

    final otherPlayer = otherPlayers.first;

    if (mustBeTop) {
      team.topPlayer = playerName;
      team.bottomPlayer = otherPlayer;
    } else {
      team.topPlayer = otherPlayer;
      team.bottomPlayer = playerName;
    }
  }

  void _swapEnds() {
    leftSideTeamId = leftSideTeamId == 'teamA' ? 'teamB' : 'teamA';
  }

  String _playerInRequiredServicePosition({required String teamId}) {
    final team = _teamById(teamId);

    if (!team.isDoubles) {
      return team.players.first;
    }

    final score = scoreOf(teamId);
    final teamIsOnLeft = teamId == leftSideTeamId;

    final serverMustBeTop = _serverMustBeTop(
      teamIsOnLeft: teamIsOnLeft,
      score: score,
    );

    return serverMustBeTop ? team.topPlayer : team.bottomPlayer;
  }

  bool _serverMustBeTop({required bool teamIsOnLeft, required int score}) {
    final scoreIsEven = score.isEven;

    if (teamIsOnLeft) {
      return !scoreIsEven;
    }

    return scoreIsEven;
  }

  String _diagonalPlayerOfServer() {
    final servingTeam = _teamById(servingTeamId);

    final receivingTeam = _oppositeTeam(servingTeamId);

    if (!isDoubles) {
      return receivingTeam.players.first;
    }

    final serverIsTop = servingTeam.topPlayer == serverName;

    return serverIsTop ? receivingTeam.bottomPlayer : receivingTeam.topPlayer;
  }

  bool get canCorrectPositions {
    return phase == MatchPhase.playing && teamAScore == 0 && teamBScore == 0;
  }

  void correctInitialPositions({
    required String newLeftSideTeamId,
    required String newServerName,
    required String newReceiverName,
  }) {
    if (!canCorrectPositions) {
      return;
    }

    final serverTeamId = getTeamIdForPlayer(newServerName);
    final receiverTeamId = getTeamIdForPlayer(newReceiverName);

    if (serverTeamId == null ||
        receiverTeamId == null ||
        serverTeamId == receiverTeamId) {
      return;
    }

    leftSideTeamId = newLeftSideTeamId;
    servingTeamId = serverTeamId;
    serverName = newServerName;
    receiverName = newReceiverName;

    _placeInitialServerAndReceiver();

    notifyMatchChanged();
  }
}
