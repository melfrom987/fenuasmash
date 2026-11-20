import 'dart:async';

import 'package:badminton_umpire/features/match/domain/models/match_draft.dart';
import 'package:badminton_umpire/features/match/domain/models/match_event.dart';
import 'package:badminton_umpire/core/utils/uuid.dart';
import 'package:badminton_umpire/features/match/domain/models/match_snapshot.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:badminton_umpire/features/match/domain/rules/badminton_rules.dart';
import 'package:flutter/foundation.dart';

import '../../domain/models/match_phase.dart';
import '../../domain/models/team_court_state.dart';

part 'match_controller_positions.dart';
part 'match_controller_scoring.dart';
part 'match_controller_intervals.dart';
part 'match_controller_events.dart';
part 'match_controller_history.dart';

class MatchController extends ChangeNotifier {
  final bool isDoubles;

  final List<String> teamAPlayers;
  final List<String> teamBPlayers;

  late TeamCourtState teamA;
  late TeamCourtState teamB;

  String leftSideTeamId;
  String courtNumber;

  int teamAScore = 0;
  int teamBScore = 0;

  int teamASets = 0;
  int teamBSets = 0;

  String servingTeamId;
  String serverName;
  String receiverName;

  MatchPhase phase = MatchPhase.playing;

  Duration intervalRemaining = Duration.zero;
  Timer? _intervalTimer;
  DateTime? intervalEndsAt;

  bool midSetIntervalTaken = false;

  String? lastSetWinnerTeamId;

  final List<List<int>> previousSets = [];
  final List<MatchSnapshot> _history = [];

  final List<MatchEvent> _events = [];

  /// Points and undos, in order, to send to the backend.
  final List<ScoreEvent> _scoreEvents = [];

  /// Creates the unique id of each score event.
  final String Function() _newEventId;

  /// Current time, replaceable in tests.
  final DateTime Function() _clock;

  List<MatchSnapshot> get history => List.unmodifiable(_history);

  List<MatchEvent> get events => List.unmodifiable(_events);

  List<ScoreEvent> get scoreEvents => List.unmodifiable(_scoreEvents);

  bool intervalTwentySecondsReached = false;

  List<String> get nextSetServerCandidates {
    if (lastSetWinnerTeamId == 'teamA') {
      return teamAPlayers;
    }

    if (lastSetWinnerTeamId == 'teamB') {
      return teamBPlayers;
    }

    return const [];
  }

  List<String> get nextSetReceiverCandidates {
    if (lastSetWinnerTeamId == 'teamA') {
      return teamBPlayers;
    }

    if (lastSetWinnerTeamId == 'teamB') {
      return teamAPlayers;
    }

    return const [];
  }

  MatchController({
    required this.isDoubles,
    required this.teamAPlayers,
    required this.teamBPlayers,
    required this.leftSideTeamId,
    required this.servingTeamId,
    required this.serverName,
    required this.receiverName,
    required this.courtNumber,
    String Function()? newEventId,
    DateTime Function()? clock,
  }) : _newEventId = newEventId ?? generateUuidV4,
       _clock = clock ?? DateTime.now {
    _initializePositions();
  }

  MatchController.fromDraft(
    MatchDraft draft, {
    String Function()? newEventId,
    DateTime Function()? clock,
  }) : _newEventId = newEventId ?? generateUuidV4,
       _clock = clock ?? DateTime.now,
       isDoubles = draft.config.isDoubles,
       teamAPlayers = List<String>.from(draft.config.teamAPlayers),
       teamBPlayers = List<String>.from(draft.config.teamBPlayers),
       leftSideTeamId = draft.leftSideTeamId,
       courtNumber = draft.config.courtNumber,
       servingTeamId = draft.servingTeamId,
       serverName = draft.serverName,
       receiverName = draft.receiverName {
    teamAScore = draft.teamAScore;
    teamBScore = draft.teamBScore;

    teamASets = draft.teamASets;
    teamBSets = draft.teamBSets;

    teamA = draft.teamA.copy();
    teamB = draft.teamB.copy();

    phase = draft.phase;
    intervalRemaining = draft.intervalRemaining;

    midSetIntervalTaken = draft.midSetIntervalTaken;
    lastSetWinnerTeamId = draft.lastSetWinnerTeamId;

    previousSets.addAll(draft.previousSets.map((set) => List<int>.from(set)));

    _history.addAll(draft.history);
    _events.addAll(draft.events);
    _scoreEvents.addAll(draft.scoreEvents);
  }

  String get rightSideTeamId {
    return leftSideTeamId == 'teamA' ? 'teamB' : 'teamA';
  }

  TeamCourtState get leftTeam {
    return leftSideTeamId == 'teamA' ? teamA : teamB;
  }

  TeamCourtState get rightTeam {
    return leftSideTeamId == 'teamA' ? teamB : teamA;
  }

  int get leftScore {
    return leftSideTeamId == 'teamA' ? teamAScore : teamBScore;
  }

  int get rightScore {
    return leftSideTeamId == 'teamA' ? teamBScore : teamAScore;
  }

  String get leftTeamName => leftTeam.players.join(' / ');

  String get rightTeamName => rightTeam.players.join(' / ');

  bool get controlsLocked => phase != MatchPhase.playing;

  bool get canUndo {
    return _history.isNotEmpty &&
        (phase == MatchPhase.playing ||
            phase == MatchPhase.midSetIntervalReady ||
            phase == MatchPhase.midSetIntervalRunning ||
            phase == MatchPhase.betweenSetsIntervalReady ||
            phase == MatchPhase.betweenSetsIntervalRunning ||
            phase == MatchPhase.finished);
  }

  bool get intervalIsReady {
    return phase == MatchPhase.midSetIntervalReady ||
        phase == MatchPhase.betweenSetsIntervalReady;
  }

  bool get intervalIsRunning {
    return phase == MatchPhase.midSetIntervalRunning ||
        phase == MatchPhase.betweenSetsIntervalRunning;
  }

  bool get isMidSetInterval {
    return phase == MatchPhase.midSetIntervalReady ||
        phase == MatchPhase.midSetIntervalRunning;
  }

  bool get isBetweenSetsInterval {
    return phase == MatchPhase.betweenSetsIntervalReady ||
        phase == MatchPhase.betweenSetsIntervalRunning;
  }

  bool get matchIsFinished => phase == MatchPhase.finished;

  List<String> get leftPlayers {
    return _playersDisplayedForTeam(leftTeam);
  }

  List<String> get rightPlayers {
    return _playersDisplayedForTeam(rightTeam);
  }

  List<String> _playersDisplayedForTeam(TeamCourtState team) {
    if (isDoubles) {
      return [team.topPlayer, team.bottomPlayer];
    }

    final playerName = team.players.first;

    final servingTeamIsOnLeft = servingTeamId == leftSideTeamId;

    final serverIsTop = _serverMustBeTop(
      teamIsOnLeft: servingTeamIsOnLeft,
      score: scoreOf(servingTeamId),
    );

    final teamIsServing = team.teamId == servingTeamId;

    final playerIsTop = teamIsServing ? serverIsTop : !serverIsTop;

    return playerIsTop ? [playerName, ''] : ['', playerName];
  }

  int get leftSets {
    return leftSideTeamId == 'teamA' ? teamASets : teamBSets;
  }

  int get rightSets {
    return leftSideTeamId == 'teamA' ? teamBSets : teamASets;
  }

  bool get teamAIsOnLeft {
    return leftSideTeamId == 'teamA';
  }

  bool get canLeavePage {
    return phase == MatchPhase.playing || phase == MatchPhase.finished;
  }

  String? getTeamIdForPlayer(String playerName) {
    if (teamA.players.contains(playerName)) {
      return 'teamA';
    }

    if (teamB.players.contains(playerName)) {
      return 'teamB';
    }

    return null;
  }

  @override
  void dispose() {
    _intervalTimer?.cancel();
    super.dispose();
  }

  bool get leftTeamIsServing {
    return servingTeamId == leftSideTeamId;
  }

  bool get rightTeamIsServing {
    return servingTeamId == rightSideTeamId;
  }

  void notifyMatchChanged() {
    notifyListeners();
  }

  TeamCourtState _teamById(String teamId) {
    return teamId == 'teamA' ? teamA : teamB;
  }

  TeamCourtState _oppositeTeam(String teamId) {
    return teamId == 'teamA' ? teamB : teamA;
  }

  String _oppositeTeamId(String teamId) {
    return teamId == 'teamA' ? 'teamB' : 'teamA';
  }

  int scoreOf(String teamId) {
    return teamId == 'teamA' ? teamAScore : teamBScore;
  }

  /// Records a point or an undo, numbered after the previous events.
  void _recordScoreEvent(ScoreEventType type, {MatchSide? side}) {
    _scoreEvents.add(
      ScoreEvent(
        id: _newEventId(),
        sequence: _scoreEvents.length + 1,
        type: type,
        side: side,
        occurredAt: _clock(),
      ),
    );
  }
}
