import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/match_test_helpers.dart';

void main() {
  group('MatchController - events', () {
    test('rightSideTeamId is opposite of leftSideTeamId', () {
      final controller = createSinglesController(leftSideTeamId: 'teamA');
      addTearDown(controller.dispose);

      expect(controller.rightSideTeamId, 'teamB');
    });

    test('left and right scores follow court sides', () {
      final controller = createSinglesController(leftSideTeamId: 'teamB');
      addTearDown(controller.dispose);

      controller.addPoint('teamA');

      expect(controller.leftScore, 0);
      expect(controller.rightScore, 1);
    });

    test('left and right sets follow court sides', () {
      final controller = createSinglesController(leftSideTeamId: 'teamB');
      addTearDown(controller.dispose);

      controller.teamASets = 1;
      controller.teamBSets = 0;

      expect(controller.leftSets, 0);
      expect(controller.rightSets, 1);
    });

    test('left and right team names are generated', () {
      final controller = createDoublesController(leftSideTeamId: 'teamA');
      addTearDown(controller.dispose);

      expect(controller.leftTeamName, 'Alice / Anna');
      expect(controller.rightTeamName, 'Charlie / Chloe');
    });

    test('controls are locked outside playing phase', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      expect(controller.controlsLocked, isFalse);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      expect(controller.phase, MatchPhase.midSetIntervalReady);

      expect(controller.controlsLocked, isTrue);
    });

    test('interval ready getters detect mid-set interval', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      expect(controller.phase, MatchPhase.midSetIntervalReady);

      expect(controller.intervalIsReady, isTrue);
      expect(controller.intervalIsRunning, isFalse);
      expect(controller.isMidSetInterval, isTrue);
      expect(controller.isBetweenSetsInterval, isFalse);
    });

    test('between sets getters detect between sets interval', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');

      expect(controller.intervalIsReady, isTrue);
      expect(controller.isBetweenSetsInterval, isTrue);
      expect(controller.isMidSetInterval, isFalse);
    });

    test('left and right serving getters are correct', () {
      final controller = createSinglesController(
        leftSideTeamId: 'teamA',
        servingTeamId: 'teamA',
      );
      addTearDown(controller.dispose);

      expect(controller.leftTeamIsServing, isTrue);
      expect(controller.rightTeamIsServing, isFalse);
    });

    test('getTeamIdForPlayer detects both teams', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      expect(controller.getTeamIdForPlayer('Alice'), 'teamA');
      expect(controller.getTeamIdForPlayer('Charlie'), 'teamB');
      expect(controller.getTeamIdForPlayer('Nobody'), isNull);
    });

    test('exposes display getters for doubles match', () {
      final controller = createDoublesController(
        leftSideTeamId: 'teamA',
        servingTeamId: 'teamA',
      );

      addTearDown(controller.dispose);

      expect(controller.leftTeamName, 'Alice / Anna');
      expect(controller.rightTeamName, 'Charlie / Chloe');

      expect(controller.leftPlayers, [
        controller.teamA.topPlayer,
        controller.teamA.bottomPlayer,
      ]);

      expect(controller.rightPlayers, [
        controller.teamB.topPlayer,
        controller.teamB.bottomPlayer,
      ]);

      expect(controller.leftSets, 0);
      expect(controller.rightSets, 0);

      expect(controller.teamAIsOnLeft, isTrue);

      expect(controller.leftTeamIsServing, isTrue);
      expect(controller.rightTeamIsServing, isFalse);

      expect(controller.canLeavePage, isTrue);
    });

    test('serving getters follow court side', () {
      final controller = createSinglesController(
        leftSideTeamId: 'teamA',
        servingTeamId: 'teamB',
        serverName: 'Bob',
        receiverName: 'Alice',
      );

      addTearDown(controller.dispose);

      expect(controller.leftTeamIsServing, isFalse);
      expect(controller.rightTeamIsServing, isTrue);
    });

    test('controls are locked during interval', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      expect(controller.controlsLocked, isFalse);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      expect(controller.controlsLocked, isTrue);
    });

    test('cannot leave page during interval', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      expect(controller.canLeavePage, isFalse);
    });

    test('resumeIntervalIfNeeded does nothing while not running', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.resumeIntervalIfNeeded();

      expect(controller.phase, MatchPhase.playing);
    });

    test('resume interval completes if end date is missing', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      controller.startCurrentInterval();

      expect(controller.phase, MatchPhase.midSetIntervalRunning);

      controller.intervalEndsAt = null;

      controller.resumeIntervalIfNeeded();

      expect(controller.phase, MatchPhase.playing);
      expect(controller.intervalRemaining, Duration.zero);
    });

    test('resume interval completes if end date has passed', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      controller.startCurrentInterval();

      controller.intervalEndsAt = DateTime.now().subtract(
        const Duration(seconds: 1),
      );

      controller.resumeIntervalIfNeeded();

      expect(controller.phase, MatchPhase.playing);
      expect(controller.intervalRemaining, Duration.zero);
    });

    test('resume interval restores remaining duration', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      controller.startCurrentInterval();

      controller.intervalEndsAt = DateTime.now().add(
        const Duration(seconds: 40),
      );

      controller.resumeIntervalIfNeeded();

      expect(controller.phase, MatchPhase.midSetIntervalRunning);

      expect(controller.intervalRemaining.inSeconds, inInclusiveRange(38, 40));
    });

    test('resume interval restores 20 second announcement flag', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      controller.startCurrentInterval();

      controller.intervalEndsAt = DateTime.now().add(
        const Duration(seconds: 15),
      );

      controller.resumeIntervalIfNeeded();

      expect(controller.intervalTwentySecondsReached, isTrue);
    });
  });
}
