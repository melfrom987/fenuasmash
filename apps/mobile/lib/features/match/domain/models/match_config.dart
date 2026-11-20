class MatchConfig {
  final bool isDoubles;

  final List<String> teamAPlayers;
  final List<String> teamBPlayers;

  final String tournamentName;
  final String courtNumber;

  final String category;
  final String? ageCategory;
  final String? poolGroup;
  final String? tournamentPhase;

  final String umpireName;
  final String? serviceJudgeName;

  const MatchConfig({
    required this.isDoubles,
    required this.teamAPlayers,
    required this.teamBPlayers,
    required this.tournamentName,
    required this.courtNumber,
    required this.category,
    this.ageCategory,
    this.poolGroup,
    this.tournamentPhase,
    required this.umpireName,
    this.serviceJudgeName,
  });

  Map<String, dynamic> toJson() {
    return {
      'isDoubles': isDoubles,
      'teamAPlayers': teamAPlayers,
      'teamBPlayers': teamBPlayers,
      'tournamentName': tournamentName,
      'courtNumber': courtNumber,
      'category': category,
      'ageCategory': ageCategory,
      'poolGroup': poolGroup,
      'tournamentPhase': tournamentPhase,
      'umpireName': umpireName,
      'serviceJudgeName': serviceJudgeName,
    };
  }

  factory MatchConfig.fromJson(Map<String, dynamic> json) {
    return MatchConfig(
      isDoubles: json['isDoubles'] as bool,

      teamAPlayers: List<String>.from(json['teamAPlayers'] as List),

      teamBPlayers: List<String>.from(json['teamBPlayers'] as List),

      tournamentName: json['tournamentName'] as String,

      courtNumber: json['courtNumber'] as String,

      category: json['category'] as String,

      ageCategory: json['ageCategory'] as String?,

      poolGroup: json['poolGroup'] as String?,

      tournamentPhase: json['tournamentPhase'] as String?,

      umpireName: json['umpireName'] as String,

      serviceJudgeName: json['serviceJudgeName'] as String?,
    );
  }
}
