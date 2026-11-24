import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:badminton_umpire/features/match/domain/models/team_court_state.dart';

class MatchSnapshot {
  final int teamAScore;
  final int teamBScore;

  final int teamASets;
  final int teamBSets;

  final String servingTeamId;
  final String serverName;
  final String receiverName;

  final String leftSideTeamId;

  final bool midSetIntervalTaken;

  final String? lastSetWinnerTeamId;

  final List<List<int>> previousSets;

  final MatchPhase phase;
  final Duration intervalRemaining;

  final TeamCourtState teamA;
  final TeamCourtState teamB;
  final DateTime? intervalEndsAt;

  const MatchSnapshot({
    required this.teamAScore,
    required this.teamBScore,
    required this.teamASets,
    required this.teamBSets,
    required this.servingTeamId,
    required this.serverName,
    required this.receiverName,
    required this.leftSideTeamId,
    required this.midSetIntervalTaken,
    required this.lastSetWinnerTeamId,
    required this.previousSets,
    required this.phase,
    required this.intervalRemaining,
    required this.teamA,
    required this.teamB,
    required this.intervalEndsAt,
  });

  Map<String, dynamic> toJson() {
    return {
      'teamAScore': teamAScore,
      'teamBScore': teamBScore,
      'teamASets': teamASets,
      'teamBSets': teamBSets,
      'servingTeamId': servingTeamId,
      'serverName': serverName,
      'receiverName': receiverName,
      'leftSideTeamId': leftSideTeamId,
      'midSetIntervalTaken': midSetIntervalTaken,
      'lastSetWinnerTeamId': lastSetWinnerTeamId,
      'previousSets': previousSets,
      'phase': phase.name,
      'intervalRemainingSeconds': intervalRemaining.inSeconds,
      'teamA': teamA.toJson(),
      'teamB': teamB.toJson(),
      'intervalEndsAt': intervalEndsAt?.toIso8601String(),
    };
  }

  factory MatchSnapshot.fromJson(Map<String, dynamic> json) {
    return MatchSnapshot(
      teamAScore: json['teamAScore'] as int,
      teamBScore: json['teamBScore'] as int,
      teamASets: json['teamASets'] as int,
      teamBSets: json['teamBSets'] as int,

      servingTeamId: json['servingTeamId'] as String,
      serverName: json['serverName'] as String,
      receiverName: json['receiverName'] as String,

      leftSideTeamId: json['leftSideTeamId'] as String,

      midSetIntervalTaken: json['midSetIntervalTaken'] as bool,
      lastSetWinnerTeamId: json['lastSetWinnerTeamId'] as String?,

      previousSets: (json['previousSets'] as List)
          .map((set) => List<int>.from(set as List))
          .toList(),

      phase: MatchPhase.values.byName(json['phase'] as String),

      intervalRemaining: Duration(
        seconds: json['intervalRemainingSeconds'] as int? ?? 0,
      ),

      intervalEndsAt: json['intervalEndsAt'] != null
          ? DateTime.parse(json['intervalEndsAt'] as String)
          : null,

      teamA: TeamCourtState.fromJson(
        Map<String, dynamic>.from(json['teamA'] as Map),
      ),

      teamB: TeamCourtState.fromJson(
        Map<String, dynamic>.from(json['teamB'] as Map),
      ),
    );
  }
}
