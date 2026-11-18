class TeamCourtState {
  final String teamId;
  final List<String> players;

  String topPlayer;
  String bottomPlayer;

  TeamCourtState({
    required this.teamId,
    required this.players,
    required this.topPlayer,
    required this.bottomPlayer,
  });

  bool get isDoubles => players.length == 2;

  void swapPlayers() {
    if (!isDoubles) {
      return;
    }

    final previousTopPlayer = topPlayer;
    topPlayer = bottomPlayer;
    bottomPlayer = previousTopPlayer;
  }

  TeamCourtState copy() {
    return TeamCourtState(
      teamId: teamId,
      players: List<String>.from(players),
      topPlayer: topPlayer,
      bottomPlayer: bottomPlayer,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'teamId': teamId,
      'players': players,
      'topPlayer': topPlayer,
      'bottomPlayer': bottomPlayer,
    };
  }

  factory TeamCourtState.fromJson(Map<String, dynamic> json) {
    return TeamCourtState(
      teamId: json['teamId'] as String,
      players: List<String>.from(json['players'] as List),
      topPlayer: json['topPlayer'] as String,
      bottomPlayer: json['bottomPlayer'] as String,
    );
  }
}
