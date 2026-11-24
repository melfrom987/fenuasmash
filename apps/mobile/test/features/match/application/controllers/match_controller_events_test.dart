import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';
import 'package:badminton_umpire/features/match/domain/models/match_event.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/match_test_helpers.dart';

void main() {
  group('MatchController - events', () {
    test('disciplinary event is recorded', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.addDisciplinaryEvent(
        playerName: 'Alice',
        type: MatchEventType.warning,
      );

      expect(controller.events.length, 1);

      final event = controller.events.single;

      expect(event.playerName, 'Alice');
      expect(event.teamId, 'teamA');
      expect(event.type, MatchEventType.warning);
      expect(event.setNumber, 1);
    });

    test('event records current score', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.addPoint('teamA');

      controller.addDisciplinaryEvent(
        playerName: 'Alice',
        type: MatchEventType.warning,
      );

      final event = controller.events.single;

      expect(event.leftScore, 1);
      expect(event.rightScore, 0);
    });

    test('unknown player does not create event', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.addDisciplinaryEvent(
        playerName: 'Unknown',
        type: MatchEventType.warning,
      );

      expect(controller.events, isEmpty);
    });

    test('retirement records event', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.retirePlayer('Alice');

      expect(controller.events.length, 1);

      expect(controller.events.single.type, MatchEventType.retirement);
    });

    test('retirement immediately finishes match', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.retirePlayer('Alice');

      expect(controller.phase, MatchPhase.finished);
      expect(controller.matchIsFinished, isTrue);
    });

    test('opponent wins match when player retires', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.retirePlayer('Alice');

      expect(controller.teamASets, 0);
      expect(controller.teamBSets, 2);

      expect(controller.lastSetWinnerTeamId, 'teamB');
    });

    test('team A wins if team B player retires', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.retirePlayer('Bob');

      expect(controller.teamASets, 2);
      expect(controller.teamBSets, 0);

      expect(controller.lastSetWinnerTeamId, 'teamA');
    });

    test('unknown player cannot retire', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.retirePlayer('Unknown');

      expect(controller.matchIsFinished, isFalse);
      expect(controller.events, isEmpty);
    });

    test('retirement cannot be applied twice after finish', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.retirePlayer('Alice');

      expect(controller.events.length, 1);

      controller.retirePlayer('Bob');

      expect(controller.events.length, 1);
    });
  });
}
