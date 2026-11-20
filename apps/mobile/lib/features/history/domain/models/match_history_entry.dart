class MatchHistoryEntry {
  final String id;

  final String tournamentName;
  final String courtNumber;
  final String category;

  final String? ageCategory;
  final String? poolGroup;
  final String? tournamentPhase;

  final List<String> teamAPlayers;
  final List<String> teamBPlayers;

  final int teamASets;
  final int teamBSets;
  final List<List<int>> sets;

  final String winnerName;

  final String umpireName;
  final String? serviceJudgeName;

  final Duration duration;
  final DateTime playedAt;

  final DateTime? startedAt;
  final DateTime? finishedAt;

  const MatchHistoryEntry({
    required this.id,
    required this.tournamentName,
    required this.courtNumber,
    required this.category,
    this.ageCategory,
    this.poolGroup,
    this.tournamentPhase,
    required this.teamAPlayers,
    required this.teamBPlayers,
    required this.teamASets,
    required this.teamBSets,
    required this.sets,
    required this.winnerName,
    required this.umpireName,
    this.serviceJudgeName,
    required this.duration,
    required this.playedAt,
    this.startedAt,
    this.finishedAt,
  });

  Map<String, dynamic> toJson() {
    return {
      'id': id,

      'tournamentName': tournamentName,
      'courtNumber': courtNumber,
      'category': category,

      'ageCategory': ageCategory,
      'poolGroup': poolGroup,
      'tournamentPhase': tournamentPhase,

      'teamAPlayers': teamAPlayers,
      'teamBPlayers': teamBPlayers,

      'teamASets': teamASets,
      'teamBSets': teamBSets,
      'sets': sets,

      'winnerName': winnerName,

      'umpireName': umpireName,
      'serviceJudgeName': serviceJudgeName,

      'durationSeconds': duration.inSeconds,
      'playedAt': playedAt.toIso8601String(),

      'startedAt': startedAt?.toIso8601String(),

      'finishedAt': finishedAt?.toIso8601String(),
    };
  }

  factory MatchHistoryEntry.fromJson(Map<String, dynamic> json) {
    return MatchHistoryEntry(
      id: json['id'] as String,

      tournamentName: json['tournamentName'] as String,

      courtNumber: json['courtNumber'] as String,

      category: json['category'] as String,

      // Nullable pour rester compatible
      // avec les anciens historiques.
      ageCategory: json['ageCategory'] as String?,

      poolGroup: json['poolGroup'] as String?,

      tournamentPhase: json['tournamentPhase'] as String?,

      teamAPlayers: List<String>.from(json['teamAPlayers'] as List),

      teamBPlayers: List<String>.from(json['teamBPlayers'] as List),

      teamASets: json['teamASets'] as int,

      teamBSets: json['teamBSets'] as int,

      sets: (json['sets'] as List)
          .map((set) => List<int>.from(set as List))
          .toList(),

      winnerName: json['winnerName'] as String,

      umpireName: json['umpireName'] as String,

      serviceJudgeName: json['serviceJudgeName'] as String?,

      duration: Duration(seconds: json['durationSeconds'] as int),

      playedAt: DateTime.parse(json['playedAt'] as String),

      startedAt: json['startedAt'] != null
          ? DateTime.parse(json['startedAt'] as String)
          : null,

      finishedAt: json['finishedAt'] != null
          ? DateTime.parse(json['finishedAt'] as String)
          : null,
    );
  }
}
