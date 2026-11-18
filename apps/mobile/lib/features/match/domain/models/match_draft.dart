import 'package:badminton_umpire/features/match/domain/models/match_config.dart';
import 'package:badminton_umpire/features/match/domain/models/match_event.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:badminton_umpire/features/match/domain/models/match_snapshot.dart';
import 'package:badminton_umpire/features/match/domain/models/team_court_state.dart';
import 'package:flutter/material.dart';

class MatchDraft {
  final String matchId;

  /// Configuration fixe du match.
  final MatchConfig config;

  /// État courant du score.
  final int teamAScore;
  final int teamBScore;

  final int teamASets;
  final int teamBSets;

  /// Service / réception.
  final String servingTeamId;
  final String serverName;
  final String receiverName;

  /// Placement des équipes sur le terrain.
  final String leftSideTeamId;

  final TeamCourtState teamA;
  final TeamCourtState teamB;

  /// Intervalles.
  final bool midSetIntervalTaken;
  final String? lastSetWinnerTeamId;

  /// Sets terminés.
  final List<List<int>> previousSets;

  /// Phase actuelle.
  final MatchPhase phase;
  final Duration intervalRemaining;

  /// Undo / événements.
  final List<MatchSnapshot> history;
  final List<MatchEvent> events;

  /// Chronométrage du match.
  final bool matchStarted;
  final DateTime? matchStartedAt;
  final DateTime? matchFinishedAt;

  /// Échauffement.
  final bool warmupStarted;
  final DateTime? warmupEndsAt;

  /// Volants.
  final int shuttleCount;

  /// Couleurs des joueurs.
  final Map<String, Color> playerColors;

  /// Date de sauvegarde.
  final DateTime savedAt;

  const MatchDraft({
    required this.matchId,
    required this.config,
    required this.teamAScore,
    required this.teamBScore,
    required this.teamASets,
    required this.teamBSets,
    required this.servingTeamId,
    required this.serverName,
    required this.receiverName,
    required this.leftSideTeamId,
    required this.teamA,
    required this.teamB,
    required this.midSetIntervalTaken,
    required this.lastSetWinnerTeamId,
    required this.previousSets,
    required this.phase,
    required this.intervalRemaining,
    required this.history,
    required this.events,
    required this.matchStarted,
    required this.matchStartedAt,
    required this.matchFinishedAt,
    required this.warmupStarted,
    required this.warmupEndsAt,
    required this.shuttleCount,
    required this.playerColors,
    required this.savedAt,
  });

  // ---------------------------------------------------------------------------
  // Compatibility getters
  //
  // Ils permettent au reste de l'application de continuer temporairement à
  // utiliser draft.tournamentName, draft.teamAPlayers, etc.
  // La véritable source de vérité reste MatchConfig.
  // ---------------------------------------------------------------------------

  String get tournamentName => config.tournamentName;

  String get courtNumber => config.courtNumber;

  String get category => config.category;

  String? get ageCategory => config.ageCategory;

  String? get poolGroup => config.poolGroup;

  String? get tournamentPhase => config.tournamentPhase;

  String get umpireName => config.umpireName;

  String? get serviceJudgeName => config.serviceJudgeName;

  bool get isDoubles => config.isDoubles;

  List<String> get teamAPlayers => config.teamAPlayers;

  List<String> get teamBPlayers => config.teamBPlayers;

  Map<String, dynamic> toJson() {
    return {
      // Nouvelle structure.
      'version': 2,

      'matchId': matchId,

      'config': config.toJson(),

      'teamAScore': teamAScore,
      'teamBScore': teamBScore,

      'teamASets': teamASets,
      'teamBSets': teamBSets,

      'servingTeamId': servingTeamId,
      'serverName': serverName,
      'receiverName': receiverName,

      'leftSideTeamId': leftSideTeamId,

      'teamA': teamA.toJson(),
      'teamB': teamB.toJson(),

      'midSetIntervalTaken': midSetIntervalTaken,
      'lastSetWinnerTeamId': lastSetWinnerTeamId,

      'previousSets': previousSets,

      'phase': phase.name,

      'intervalRemainingSeconds': intervalRemaining.inSeconds,

      'history': history.map((snapshot) => snapshot.toJson()).toList(),

      'events': events.map((event) => event.toJson()).toList(),

      'matchStarted': matchStarted,

      'matchStartedAt': matchStartedAt?.toIso8601String(),

      'matchFinishedAt': matchFinishedAt?.toIso8601String(),

      'warmupStarted': warmupStarted,

      'warmupEndsAt': warmupEndsAt?.toIso8601String(),

      'shuttleCount': shuttleCount,

      'playerColors': playerColors.map(
        (player, color) => MapEntry(player, color.toARGB32()),
      ),

      'savedAt': savedAt.toIso8601String(),
    };
  }

  factory MatchDraft.fromJson(Map<String, dynamic> json) {
    final configJson = json['config'];

    // -------------------------------------------------------------------------
    // Migration
    //
    // version 2 :
    // {
    //   "config": {...}
    // }
    //
    // version 1 :
    // les informations étaient directement dans MatchDraft.
    // -------------------------------------------------------------------------

    final MatchConfig config;

    if (configJson is Map) {
      config = MatchConfig.fromJson(Map<String, dynamic>.from(configJson));
    } else {
      config = MatchConfig(
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

    return MatchDraft(
      matchId: json['matchId'] as String,

      config: config,

      teamAScore: json['teamAScore'] as int,

      teamBScore: json['teamBScore'] as int,

      teamASets: json['teamASets'] as int,

      teamBSets: json['teamBSets'] as int,

      servingTeamId: json['servingTeamId'] as String,

      serverName: json['serverName'] as String,

      receiverName: json['receiverName'] as String,

      leftSideTeamId: json['leftSideTeamId'] as String,

      teamA: TeamCourtState.fromJson(
        Map<String, dynamic>.from(json['teamA'] as Map),
      ),

      teamB: TeamCourtState.fromJson(
        Map<String, dynamic>.from(json['teamB'] as Map),
      ),

      midSetIntervalTaken: json['midSetIntervalTaken'] as bool,

      lastSetWinnerTeamId: json['lastSetWinnerTeamId'] as String?,

      previousSets: (json['previousSets'] as List)
          .map((set) => List<int>.from(set as List))
          .toList(),

      phase: MatchPhase.values.byName(json['phase'] as String),

      intervalRemaining: Duration(
        seconds: json['intervalRemainingSeconds'] as int? ?? 0,
      ),

      history: (json['history'] as List? ?? [])
          .map(
            (snapshot) => MatchSnapshot.fromJson(
              Map<String, dynamic>.from(snapshot as Map),
            ),
          )
          .toList(),

      events: (json['events'] as List? ?? [])
          .map(
            (event) =>
                MatchEvent.fromJson(Map<String, dynamic>.from(event as Map)),
          )
          .toList(),

      matchStarted: json['matchStarted'] as bool? ?? false,

      matchStartedAt: json['matchStartedAt'] != null
          ? DateTime.parse(json['matchStartedAt'] as String)
          : null,

      matchFinishedAt: json['matchFinishedAt'] != null
          ? DateTime.parse(json['matchFinishedAt'] as String)
          : null,

      warmupStarted: json['warmupStarted'] as bool? ?? false,

      warmupEndsAt: json['warmupEndsAt'] != null
          ? DateTime.parse(json['warmupEndsAt'] as String)
          : null,

      shuttleCount: json['shuttleCount'] as int? ?? 1,

      playerColors: {
        for (final entry in Map<String, dynamic>.from(
          json['playerColors'] as Map? ?? {},
        ).entries)
          entry.key: Color(entry.value as int),
      },

      savedAt: DateTime.parse(json['savedAt'] as String),
    );
  }
}
