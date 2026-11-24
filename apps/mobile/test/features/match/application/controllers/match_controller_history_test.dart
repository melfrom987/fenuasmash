import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/match_test_helpers.dart';

void main() {
  group('MatchController - undo', () {
    test('cannot undo at beginning of match', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      expect(controller.canUndo, isFalse);

      controller.undoLastPoint();

      expect(controller.teamAScore, 0);
      expect(controller.teamBScore, 0);
    });

    test('can undo after one point', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamA');

      expect(controller.canUndo, isTrue);
    });

    test('undo restores previous score', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamA');

      expect(controller.teamAScore, 1);

      controller.undoLastPoint();

      expect(controller.teamAScore, 0);
      expect(controller.teamBScore, 0);
    });

    test('undo restores serving team', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamB');

      expect(controller.servingTeamId, 'teamB');

      controller.undoLastPoint();

      expect(controller.servingTeamId, 'teamA');
      expect(controller.serverName, 'Alice');
      expect(controller.receiverName, 'Bob');
    });

    test('undo restores state before mid-set interval', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 7; i++) {
        controller.addPoint('teamA');
      }

      controller.addPoint('teamA');

      expect(controller.phase, MatchPhase.midSetIntervalReady);

      expect(controller.teamAScore, 8);

      controller.undoLastPoint();

      expect(controller.teamAScore, 7);
      expect(controller.phase, MatchPhase.playing);
      expect(controller.midSetIntervalTaken, isFalse);
    });

    test('undo restores completed set', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamA', 14);

      controller.addPoint('teamA');

      expect(controller.teamASets, 1);

      controller.undoLastPoint();

      expect(controller.teamAScore, 14);
      expect(controller.teamBScore, 0);

      expect(controller.teamASets, 0);
      expect(controller.previousSets, isEmpty);

      expect(controller.phase, MatchPhase.playing);
    });

    test('undo can reopen a finished match', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');
      prepareNextSet(controller);

      addTestPoints(controller, 'teamA', 14);

      controller.addPoint('teamA');

      expect(controller.matchIsFinished, isTrue);

      controller.undoLastPoint();

      expect(controller.matchIsFinished, isFalse);
      expect(controller.teamASets, 1);
      expect(controller.teamAScore, 14);
    });

    test('multiple undos restore points in order', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamA');
      controller.addPoint('teamB');
      controller.addPoint('teamA');

      expect(controller.teamAScore, 2);
      expect(controller.teamBScore, 1);

      controller.undoLastPoint();

      expect(controller.teamAScore, 1);
      expect(controller.teamBScore, 1);

      controller.undoLastPoint();

      expect(controller.teamAScore, 1);
      expect(controller.teamBScore, 0);

      controller.undoLastPoint();

      expect(controller.teamAScore, 0);
      expect(controller.teamBScore, 0);
    });
  });
}
