import 'package:badminton_umpire/features/match/domain/models/match_config.dart';
import 'package:badminton_umpire/features/match/domain/models/match_draft.dart';
import 'package:badminton_umpire/features/match/domain/models/match_event.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:badminton_umpire/features/match/domain/models/match_snapshot.dart';
import 'package:badminton_umpire/features/match/domain/models/team_court_state.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('MatchDraft JSON', () {
    MatchConfig createConfig() {
      return const MatchConfig(
        isDoubles: true,
        teamAPlayers: ['Alice', 'Anna'],
        teamBPlayers: ['Bob', 'Ben'],
        tournamentName: 'Tahiti Open',
        courtNumber: '3',
        category: 'Mixed Doubles',
        ageCategory: 'Senior',
        poolGroup: 'Pool B',
        tournamentPhase: 'Pool match',
        umpireName: 'Melissa',
        serviceJudgeName: 'John',
      );
    }

    TeamCourtState createTeamA() {
      return TeamCourtState(
        teamId: 'teamA',
        players: const ['Alice', 'Anna'],
        topPlayer: 'Alice',
        bottomPlayer: 'Anna',
      );
    }

    TeamCourtState createTeamB() {
      return TeamCourtState(
        teamId: 'teamB',
        players: const ['Bob', 'Ben'],
        topPlayer: 'Bob',
        bottomPlayer: 'Ben',
      );
    }

    MatchSnapshot createSnapshot() {
      return MatchSnapshot(
        teamAScore: 8,
        teamBScore: 6,
        teamASets: 1,
        teamBSets: 0,
        servingTeamId: 'teamA',
        serverName: 'Alice',
        receiverName: 'Bob',
        leftSideTeamId: 'teamA',
        midSetIntervalTaken: true,
        lastSetWinnerTeamId: 'teamA',
        previousSets: const [
          [15, 11],
        ],
        phase: MatchPhase.midSetIntervalRunning,
        intervalRemaining: const Duration(seconds: 37),
        intervalEndsAt: DateTime(2026, 9, 6, 17, 30),
        teamA: createTeamA(),
        teamB: createTeamB(),
      );
    }

    MatchDraft createDraft() {
      return MatchDraft(
        matchId: 'match-123',
        config: createConfig(),

        teamAScore: 8,
        teamBScore: 6,
        teamASets: 1,
        teamBSets: 0,

        servingTeamId: 'teamA',
        serverName: 'Alice',
        receiverName: 'Bob',

        leftSideTeamId: 'teamA',

        teamA: createTeamA(),
        teamB: createTeamB(),

        midSetIntervalTaken: true,
        lastSetWinnerTeamId: 'teamA',

        previousSets: const [
          [15, 11],
        ],

        phase: MatchPhase.midSetIntervalRunning,
        intervalRemaining: const Duration(seconds: 37),

        history: [createSnapshot()],

        events: [
          MatchEvent(
            type: MatchEventType.warning,
            playerName: 'Alice',
            teamId: 'teamA',
            setNumber: 2,
            leftScore: 8,
            rightScore: 6,
            timestamp: DateTime(2026, 9, 6, 17, 20),
          ),
        ],

        matchStarted: true,
        matchStartedAt: DateTime(2026, 9, 6, 17, 0),
        matchFinishedAt: null,

        warmupStarted: true,
        warmupEndsAt: DateTime(2026, 9, 6, 17, 2),

        shuttleCount: 4,

        playerColors: const {
          'Alice': Colors.red,
          'Anna': Colors.blue,
          'Bob': Colors.green,
          'Ben': Colors.orange,
        },

        savedAt: DateTime(2026, 9, 6, 17, 25),
      );
    }

    test('serializes and restores complete draft', () {
      final original = createDraft();

      final restored = MatchDraft.fromJson(original.toJson());

      expect(restored.matchId, 'match-123');

      expect(restored.config.tournamentName, 'Tahiti Open');

      expect(restored.config.teamAPlayers, ['Alice', 'Anna']);

      expect(restored.config.teamBPlayers, ['Bob', 'Ben']);

      expect(restored.config.ageCategory, 'Senior');
      expect(restored.config.poolGroup, 'Pool B');
      expect(restored.config.tournamentPhase, 'Pool match');

      expect(restored.teamAScore, 8);
      expect(restored.teamBScore, 6);

      expect(restored.teamASets, 1);
      expect(restored.teamBSets, 0);

      expect(restored.servingTeamId, 'teamA');
      expect(restored.serverName, 'Alice');
      expect(restored.receiverName, 'Bob');

      expect(restored.leftSideTeamId, 'teamA');

      expect(restored.midSetIntervalTaken, isTrue);

      expect(restored.previousSets, [
        [15, 11],
      ]);

      expect(restored.phase, MatchPhase.midSetIntervalRunning);

      expect(restored.intervalRemaining, const Duration(seconds: 37));

      expect(restored.history.length, 1);
      expect(restored.events.length, 1);

      expect(restored.matchStarted, isTrue);

      expect(restored.matchStartedAt, DateTime(2026, 9, 6, 17, 0));

      expect(restored.matchFinishedAt, isNull);

      expect(restored.warmupStarted, isTrue);

      expect(restored.warmupEndsAt, DateTime(2026, 9, 6, 17, 2));

      expect(restored.shuttleCount, 4);

      expect(restored.playerColors['Alice']?.toARGB32(), Colors.red.toARGB32());

      expect(restored.savedAt, DateTime(2026, 9, 6, 17, 25));
    });

    test('compatibility getters read from config', () {
      final draft = createDraft();

      expect(draft.tournamentName, 'Tahiti Open');
      expect(draft.courtNumber, '3');
      expect(draft.category, 'Mixed Doubles');
      expect(draft.ageCategory, 'Senior');
      expect(draft.poolGroup, 'Pool B');
      expect(draft.tournamentPhase, 'Pool match');
      expect(draft.umpireName, 'Melissa');
      expect(draft.serviceJudgeName, 'John');

      expect(draft.isDoubles, isTrue);

      expect(draft.teamAPlayers, ['Alice', 'Anna']);

      expect(draft.teamBPlayers, ['Bob', 'Ben']);
    });

    test('version 2 stores config object', () {
      final json = createDraft().toJson();

      expect(json['version'], 2);
      expect(json['config'], isA<Map<String, dynamic>>());

      expect((json['config'] as Map)['tournamentName'], 'Tahiti Open');
    });

    test('restores nullable optional values', () {
      final draft = createDraft();

      final json = draft.toJson();

      final config = Map<String, dynamic>.from(json['config'] as Map);

      config['ageCategory'] = null;
      config['poolGroup'] = null;
      config['tournamentPhase'] = null;
      config['serviceJudgeName'] = null;

      json['config'] = config;

      final restored = MatchDraft.fromJson(json);

      expect(restored.ageCategory, isNull);
      expect(restored.poolGroup, isNull);
      expect(restored.tournamentPhase, isNull);
      expect(restored.serviceJudgeName, isNull);
    });

    test('restores empty history and events', () {
      final json = createDraft().toJson();

      json['history'] = [];
      json['events'] = [];

      final restored = MatchDraft.fromJson(json);

      expect(restored.history, isEmpty);
      expect(restored.events, isEmpty);
    });

    test('uses shuttle count 1 if absent', () {
      final json = createDraft().toJson();

      json.remove('shuttleCount');

      final restored = MatchDraft.fromJson(json);

      expect(restored.shuttleCount, 1);
    });

    test('uses false for missing matchStarted', () {
      final json = createDraft().toJson();

      json.remove('matchStarted');

      final restored = MatchDraft.fromJson(json);

      expect(restored.matchStarted, isFalse);
    });

    test('uses false for missing warmupStarted', () {
      final json = createDraft().toJson();

      json.remove('warmupStarted');

      final restored = MatchDraft.fromJson(json);

      expect(restored.warmupStarted, isFalse);
    });

    test('restores legacy version 1 draft', () {
      final current = createDraft().toJson();

      final legacyJson = <String, dynamic>{
        'version': 1,

        'matchId': current['matchId'],

        'tournamentName': 'Legacy Tournament',
        'courtNumber': '5',
        'category': 'Women Singles',
        'umpireName': 'Legacy Umpire',
        'serviceJudgeName': null,

        'isDoubles': false,

        'teamAPlayers': ['Alice'],
        'teamBPlayers': ['Bob'],

        'ageCategory': 'Senior',
        'poolGroup': 'Pool A',
        'tournamentPhase': 'Pool match',

        'teamAScore': current['teamAScore'],
        'teamBScore': current['teamBScore'],

        'teamASets': current['teamASets'],
        'teamBSets': current['teamBSets'],

        'servingTeamId': current['servingTeamId'],
        'serverName': 'Alice',
        'receiverName': 'Bob',
        'leftSideTeamId': current['leftSideTeamId'],

        'teamA': TeamCourtState(
          teamId: 'teamA',
          players: const ['Alice'],
          topPlayer: 'Alice',
          bottomPlayer: '',
        ).toJson(),

        'teamB': TeamCourtState(
          teamId: 'teamB',
          players: const ['Bob'],
          topPlayer: 'Bob',
          bottomPlayer: '',
        ).toJson(),

        'midSetIntervalTaken': current['midSetIntervalTaken'],

        'lastSetWinnerTeamId': current['lastSetWinnerTeamId'],

        'previousSets': current['previousSets'],

        'phase': current['phase'],

        'intervalRemainingSeconds': current['intervalRemainingSeconds'],

        'history': [],
        'events': [],

        'matchStarted': true,

        'matchStartedAt': DateTime(2026, 9, 6, 17, 0).toIso8601String(),

        'matchFinishedAt': null,

        'warmupStarted': true,

        'warmupEndsAt': DateTime(2026, 9, 6, 17, 2).toIso8601String(),

        'shuttleCount': 2,

        'playerColors': {
          'Alice': Colors.red.toARGB32(),
          'Bob': Colors.blue.toARGB32(),
        },

        'savedAt': DateTime(2026, 9, 6, 17, 5).toIso8601String(),
      };

      final restored = MatchDraft.fromJson(legacyJson);

      expect(restored.config.tournamentName, 'Legacy Tournament');

      expect(restored.config.courtNumber, '5');

      expect(restored.config.category, 'Women Singles');

      expect(restored.config.isDoubles, isFalse);

      expect(restored.config.teamAPlayers, ['Alice']);

      expect(restored.config.teamBPlayers, ['Bob']);

      expect(restored.config.ageCategory, 'Senior');

      expect(restored.config.poolGroup, 'Pool A');

      expect(restored.config.tournamentPhase, 'Pool match');
    });
  });
}
