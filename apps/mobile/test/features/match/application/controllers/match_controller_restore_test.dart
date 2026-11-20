import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';
import 'package:badminton_umpire/features/match/domain/models/match_config.dart';
import 'package:badminton_umpire/features/match/domain/models/match_draft.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:badminton_umpire/features/match/domain/models/team_court_state.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('MatchController.fromDraft', () {
    test('restores complete controller state from draft', () {
      final draft = MatchDraft(
        matchId: 'match-1',

        config: const MatchConfig(
          isDoubles: true,
          teamAPlayers: ['Alice', 'Anna'],
          teamBPlayers: ['Bob', 'Ben'],
          tournamentName: 'Open',
          courtNumber: '4',
          category: 'Doubles',
          umpireName: 'Melissa',
        ),

        teamAScore: 11,
        teamBScore: 9,
        teamASets: 1,
        teamBSets: 0,

        servingTeamId: 'teamB',
        serverName: 'Bob',
        receiverName: 'Alice',

        leftSideTeamId: 'teamB',

        teamA: TeamCourtState(
          teamId: 'teamA',
          players: const ['Alice', 'Anna'],
          topPlayer: 'Anna',
          bottomPlayer: 'Alice',
        ),

        teamB: TeamCourtState(
          teamId: 'teamB',
          players: const ['Bob', 'Ben'],
          topPlayer: 'Bob',
          bottomPlayer: 'Ben',
        ),

        midSetIntervalTaken: true,
        lastSetWinnerTeamId: 'teamA',

        previousSets: const [
          [15, 10],
        ],

        phase: MatchPhase.playing,
        intervalRemaining: const Duration(seconds: 25),

        history: const [],
        events: const [],

        matchStarted: true,
        matchStartedAt: DateTime(2026, 9, 6, 17),
        matchFinishedAt: null,

        warmupStarted: true,
        warmupEndsAt: null,

        shuttleCount: 3,

        playerColors: const {'Alice': Colors.red},

        savedAt: DateTime(2026, 9, 6, 17, 20),
      );

      final controller = MatchController.fromDraft(draft);
      addTearDown(controller.dispose);

      expect(controller.isDoubles, isTrue);

      expect(controller.teamAPlayers, ['Alice', 'Anna']);

      expect(controller.teamBPlayers, ['Bob', 'Ben']);

      expect(controller.courtNumber, '4');

      expect(controller.teamAScore, 11);
      expect(controller.teamBScore, 9);

      expect(controller.teamASets, 1);
      expect(controller.teamBSets, 0);

      expect(controller.servingTeamId, 'teamB');
      expect(controller.serverName, 'Bob');
      expect(controller.receiverName, 'Alice');

      expect(controller.leftSideTeamId, 'teamB');

      expect(controller.midSetIntervalTaken, isTrue);

      expect(controller.lastSetWinnerTeamId, 'teamA');

      expect(controller.previousSets, [
        [15, 10],
      ]);

      expect(controller.phase, MatchPhase.playing);

      expect(controller.intervalRemaining, const Duration(seconds: 25));

      expect(controller.teamA.topPlayer, 'Anna');
      expect(controller.teamA.bottomPlayer, 'Alice');

      expect(controller.teamB.topPlayer, 'Bob');
      expect(controller.teamB.bottomPlayer, 'Ben');
    });
  });
}
