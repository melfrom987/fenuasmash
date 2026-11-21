enum MatchEventType { warning, yellowCard, redCard, blackCard, retirement }

class MatchEvent {
  final MatchEventType type;
  final String playerName;
  final String teamId;
  final int setNumber;
  final int leftScore;
  final int rightScore;
  final DateTime timestamp;

  const MatchEvent({
    required this.type,
    required this.playerName,
    required this.teamId,
    required this.setNumber,
    required this.leftScore,
    required this.rightScore,
    required this.timestamp,
  });

  Map<String, dynamic> toJson() {
    return {
      'type': type.name,
      'playerName': playerName,
      'teamId': teamId,
      'setNumber': setNumber,
      'leftScore': leftScore,
      'rightScore': rightScore,
      'timestamp': timestamp.toIso8601String(),
    };
  }

  factory MatchEvent.fromJson(Map<String, dynamic> json) {
    return MatchEvent(
      type: MatchEventType.values.byName(json['type'] as String),
      playerName: json['playerName'] as String,
      teamId: json['teamId'] as String,
      setNumber: json['setNumber'] as int,
      leftScore: json['leftScore'] as int,
      rightScore: json['rightScore'] as int,
      timestamp: DateTime.parse(json['timestamp'] as String),
    );
  }
}
