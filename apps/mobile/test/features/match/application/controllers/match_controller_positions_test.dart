import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/match_test_helpers.dart';

void main() {
  group('MatchController - singles positions', () {
    test('initial server and receiver are valid', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      expect(controller.serverName, 'Alice');
      expect(controller.receiverName, 'Bob');
      expect(controller.servingTeamId, 'teamA');
    });

    test('receiving team becomes serving after winning point', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamB');

      expect(controller.servingTeamId, 'teamB');
      expect(controller.serverName, 'Bob');
      expect(controller.receiverName, 'Alice');
    });

    test('serving team keeps service after winning point', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamA');

      expect(controller.servingTeamId, 'teamA');
      expect(controller.serverName, 'Alice');
      expect(controller.receiverName, 'Bob');
    });

    test('left and right teams reflect court side', () {
      final controller = createSinglesController(leftSideTeamId: 'teamB');

      addTearDown(controller.dispose);

      expect(controller.leftSideTeamId, 'teamB');
      expect(controller.rightSideTeamId, 'teamA');

      expect(controller.leftTeam.players, ['Bob']);
      expect(controller.rightTeam.players, ['Alice']);
    });
  });

  group('MatchController - doubles positions', () {
    test('initial server is placed in correct position', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      expect(controller.teamA.bottomPlayer, 'Alice');
      expect(controller.teamA.topPlayer, 'Anna');

      expect(controller.teamB.topPlayer, 'Charlie');
      expect(controller.teamB.bottomPlayer, 'Chloe');
    });

    test('serving pair swaps positions after winning rally', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamA');

      expect(controller.servingTeamId, 'teamA');

      expect(controller.teamA.topPlayer, 'Alice');
      expect(controller.teamA.bottomPlayer, 'Anna');
    });

    test('receiver changes to diagonal player after serving team wins', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamA');

      expect(controller.receiverName, 'Chloe');
    });

    test('pairs do not swap on service change', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      final aTop = controller.teamA.topPlayer;
      final aBottom = controller.teamA.bottomPlayer;

      final bTop = controller.teamB.topPlayer;
      final bBottom = controller.teamB.bottomPlayer;

      controller.addPoint('teamB');

      expect(controller.teamA.topPlayer, aTop);
      expect(controller.teamA.bottomPlayer, aBottom);

      expect(controller.teamB.topPlayer, bTop);
      expect(controller.teamB.bottomPlayer, bBottom);
    });

    test('receiving team becomes serving team', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamB');

      expect(controller.servingTeamId, 'teamB');
    });

    test('unknown player returns no team', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      expect(controller.getTeamIdForPlayer('Unknown'), isNull);
    });

    test('team is detected for every player', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      expect(controller.getTeamIdForPlayer('Alice'), 'teamA');

      expect(controller.getTeamIdForPlayer('Anna'), 'teamA');

      expect(controller.getTeamIdForPlayer('Charlie'), 'teamB');

      expect(controller.getTeamIdForPlayer('Chloe'), 'teamB');
    });

    test('teams swap ends after first set', () {
      final controller = createSinglesController(leftSideTeamId: 'teamA');

      addTearDown(controller.dispose);

      winSet(controller, 'teamA');

      expect(controller.leftSideTeamId, 'teamB');
    });

    test('teams swap ends at 8 in deciding set', () {
      final controller = createSinglesController(leftSideTeamId: 'teamA');

      addTearDown(controller.dispose);

      // Set 1 : A
      winSet(controller, 'teamA');
      prepareNextSet(controller);

      // Set 2 : B
      winSet(controller, 'teamB');
      prepareNextSet(controller);

      expect(controller.teamASets, 1);
      expect(controller.teamBSets, 1);

      final sideBeforeEight = controller.leftSideTeamId;

      // 7 points
      for (var i = 0; i < 7; i++) {
        controller.addPoint('teamA');
      }

      expect(controller.leftSideTeamId, sideBeforeEight);

      // 8e point
      controller.addPoint('teamA');

      expect(controller.leftSideTeamId, isNot(sideBeforeEight));
    });
  });
}
