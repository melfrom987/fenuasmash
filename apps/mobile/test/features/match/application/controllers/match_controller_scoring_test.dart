import 'package:badminton_umpire/features/match/domain/models/match_event.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';

import '../../helpers/match_test_helpers.dart';

void main() {
  group('MatchController - scoring', () {
    test('match starts at 0-0 and 0 sets', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      expect(controller.teamAScore, 0);
      expect(controller.teamBScore, 0);

      expect(controller.teamASets, 0);
      expect(controller.teamBSets, 0);

      expect(controller.previousSets, isEmpty);
    });

    test('adds a point to team A', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamA');

      expect(controller.teamAScore, 1);
      expect(controller.teamBScore, 0);
    });

    test('adds a point to team B', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamB');

      expect(controller.teamAScore, 0);
      expect(controller.teamBScore, 1);
    });

    test('left point goes to team A when team A is left', () {
      final controller = createSinglesController(leftSideTeamId: 'teamA');

      addTearDown(controller.dispose);

      controller.addPointToLeftTeam();

      expect(controller.teamAScore, 1);
      expect(controller.teamBScore, 0);
    });

    test('left point goes to team B when team B is left', () {
      final controller = createSinglesController(leftSideTeamId: 'teamB');

      addTearDown(controller.dispose);

      controller.addPointToLeftTeam();

      expect(controller.teamAScore, 0);
      expect(controller.teamBScore, 1);
    });

    test('right point uses the team currently on the right', () {
      final controller = createSinglesController(leftSideTeamId: 'teamA');

      addTearDown(controller.dispose);

      controller.addPointToRightTeam();

      expect(controller.teamAScore, 0);
      expect(controller.teamBScore, 1);
    });

    test('set does not finish at 14-14', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 14; i++) {
        addTestPoint(controller, 'teamA');
        addTestPoint(controller, 'teamB');
      }

      expect(controller.teamAScore, 14);
      expect(controller.teamBScore, 14);

      expect(controller.teamASets, 0);
      expect(controller.teamBSets, 0);

      expect(controller.phase, MatchPhase.playing);
    });

    test('set does not finish at 15-14', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamB', 14);
      addTestPoints(controller, 'teamA', 15);

      expect(controller.teamAScore, 15);
      expect(controller.teamBScore, 14);

      expect(controller.teamASets, 0);
      expect(controller.phase, MatchPhase.playing);
    });

    test('team A wins at 15-13', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamB', 13);
      addTestPoints(controller, 'teamA', 15);

      expect(controller.teamASets, 1);
      expect(controller.teamBSets, 0);

      expect(controller.previousSets, [
        [15, 13],
      ]);

      expect(controller.phase, MatchPhase.betweenSetsIntervalReady);
    });

    test('team B wins at 15-0', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamB', 15);

      expect(controller.teamASets, 0);
      expect(controller.teamBSets, 1);

      expect(controller.previousSets, [
        [0, 15],
      ]);
    });

    test('set continues at 20-20', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 20; i++) {
        addTestPoint(controller, 'teamA');
        addTestPoint(controller, 'teamB');
      }

      expect(controller.teamAScore, 20);
      expect(controller.teamBScore, 20);

      expect(controller.teamASets, 0);
      expect(controller.teamBSets, 0);

      expect(controller.phase, MatchPhase.playing);
    });

    test('team wins automatically at 21-20', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 20; i++) {
        addTestPoint(controller, 'teamA');
        addTestPoint(controller, 'teamB');
      }

      addTestPoint(controller, 'teamA');

      expect(controller.teamASets, 1);

      expect(controller.previousSets, [
        [21, 20],
      ]);
    });

    test('scores reset after a completed set', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamA', 15);

      expect(controller.teamAScore, 0);
      expect(controller.teamBScore, 0);
    });

    test('winning two sets finishes the match', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');

      expect(controller.teamASets, 1);

      prepareNextSet(controller);

      winSet(controller, 'teamA');

      expect(controller.teamASets, 2);
      expect(controller.teamBSets, 0);
      expect(controller.matchIsFinished, isTrue);
      expect(controller.phase, MatchPhase.finished);
    });

    test('match can go to a third set', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');
      prepareNextSet(controller);

      winSet(controller, 'teamB');
      prepareNextSet(controller);

      expect(controller.teamASets, 1);
      expect(controller.teamBSets, 1);

      expect(controller.previousSets.length, 2);
      expect(controller.phase, MatchPhase.playing);
    });

    test('points cannot be added after match is finished', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');
      prepareNextSet(controller);
      winSet(controller, 'teamA');

      expect(controller.matchIsFinished, isTrue);

      final scoreA = controller.teamAScore;
      final scoreB = controller.teamBScore;

      controller.addPoint('teamA');

      expect(controller.teamAScore, scoreA);
      expect(controller.teamBScore, scoreB);
    });

    test('team has set point when serving at 14-0', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamA', 14);

      expect(controller.servingTeamId, 'teamA');
      expect(controller.hasSetPoint('teamA'), isTrue);
      expect(controller.leftHasSetPoint, isTrue);
    });

    test('receiving team does not show set point', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamA', 14);

      controller.addPoint('teamB');

      expect(controller.servingTeamId, 'teamB');

      expect(controller.hasSetPoint('teamA'), isFalse);
    });

    test('set can finish at 16-14', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamB', 14);
      addTestPoints(controller, 'teamA', 16);

      expect(controller.teamASets, 1);

      expect(controller.previousSets, [
        [16, 14],
      ]);
    });

    test('set can finish at 17-15', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      // 14-14
      for (var i = 0; i < 14; i++) {
        addTestPoint(controller, 'teamA');
        addTestPoint(controller, 'teamB');
      }

      // 15-14
      addTestPoint(controller, 'teamA');

      // 15-15
      addTestPoint(controller, 'teamB');

      // 16-15
      addTestPoint(controller, 'teamA');

      // 17-15
      addTestPoint(controller, 'teamA');

      expect(controller.teamASets, 1);
      expect(controller.teamBSets, 0);

      expect(controller.previousSets, [
        [17, 15],
      ]);
    });

    test('20-19 does not finish set', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 14; i++) {
        addTestPoint(controller, 'teamA');
        addTestPoint(controller, 'teamB');
      }

      for (var i = 0; i < 5; i++) {
        addTestPoint(controller, 'teamA');
        addTestPoint(controller, 'teamB');
      }

      addTestPoint(controller, 'teamA');

      expect(controller.teamAScore, 20);
      expect(controller.teamBScore, 19);

      expect(controller.teamASets, 0);
      expect(controller.teamBSets, 0);

      expect(controller.phase, MatchPhase.playing);
    });

    test('team B can win 21-20', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 20; i++) {
        addTestPoint(controller, 'teamA');
        addTestPoint(controller, 'teamB');
      }

      addTestPoint(controller, 'teamB');

      expect(controller.teamBSets, 1);

      expect(controller.previousSets, [
        [20, 21],
      ]);
    });
    test('correct initial positions changes serving team', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      controller.correctInitialPositions(
        newLeftSideTeamId: 'teamB',
        newServerName: 'Charlie',
        newReceiverName: 'Alice',
      );

      expect(controller.leftSideTeamId, 'teamB');
      expect(controller.servingTeamId, 'teamB');
      expect(controller.serverName, 'Charlie');
      expect(controller.receiverName, 'Alice');
    });

    test('cannot correct initial positions after scoring', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamA');

      final oldSide = controller.leftSideTeamId;
      final oldServer = controller.serverName;

      controller.correctInitialPositions(
        newLeftSideTeamId: 'teamB',
        newServerName: 'Charlie',
        newReceiverName: 'Alice',
      );

      expect(controller.leftSideTeamId, oldSide);
      expect(controller.serverName, oldServer);
    });

    test('invalid same-team server and receiver are rejected', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      final oldServer = controller.serverName;
      final oldReceiver = controller.receiverName;

      controller.correctInitialPositions(
        newLeftSideTeamId: 'teamA',
        newServerName: 'Alice',
        newReceiverName: 'Anna',
      );

      expect(controller.serverName, oldServer);
      expect(controller.receiverName, oldReceiver);
    });
    test('mid-set interval flag resets after set', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamA', 8);

      expect(controller.midSetIntervalTaken, isTrue);

      addTestPoints(controller, 'teamA', 7);

      expect(controller.midSetIntervalTaken, isFalse);
    });

    test('finishIntervalEarly clears timing state', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      controller.startCurrentInterval();

      expect(controller.intervalEndsAt, isNotNull);

      controller.finishIntervalEarly();

      expect(controller.intervalRemaining, Duration.zero);
      expect(controller.intervalEndsAt, isNull);
      expect(controller.intervalTwentySecondsReached, isFalse);
    });

    test('undo restores doubles player positions', () {
      final controller = createDoublesController();
      addTearDown(controller.dispose);

      final topBefore = controller.teamA.topPlayer;
      final bottomBefore = controller.teamA.bottomPlayer;

      controller.addPoint('teamA');

      expect(controller.teamA.topPlayer, isNot(topBefore));

      controller.undoLastPoint();

      expect(controller.teamA.topPlayer, topBefore);
      expect(controller.teamA.bottomPlayer, bottomBefore);
    });

    test('undo restores previousSets after set finish', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamA', 14);
      controller.addPoint('teamA');

      expect(controller.previousSets.length, 1);

      controller.undoLastPoint();

      expect(controller.previousSets, isEmpty);
    });

    test('disciplinary event uses current set number', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');
      prepareNextSet(controller);

      controller.addDisciplinaryEvent(
        playerName: 'Alice',
        type: MatchEventType.warning,
      );

      expect(controller.events.single.setNumber, 2);
    });

    test('retirement in second set still finishes match', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');
      prepareNextSet(controller);

      controller.retirePlayer('Alice');

      expect(controller.matchIsFinished, isTrue);
      expect(controller.teamBSets, 2);
    });
  });
}
