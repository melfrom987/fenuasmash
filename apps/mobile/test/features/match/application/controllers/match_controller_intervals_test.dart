import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/match_test_helpers.dart';

void main() {
  group('MatchController - intervals', () {
    test('mid-set interval becomes ready at 8 points', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 7; i++) {
        controller.addPoint('teamA');
      }

      expect(controller.phase, MatchPhase.playing);

      controller.addPoint('teamA');

      expect(controller.teamAScore, 8);
      expect(controller.phase, MatchPhase.midSetIntervalReady);

      expect(controller.midSetIntervalTaken, isTrue);

      expect(controller.intervalRemaining, const Duration(minutes: 1));
    });

    test('points cannot be added while interval is ready', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      expect(controller.phase, MatchPhase.midSetIntervalReady);

      controller.addPoint('teamA');

      expect(controller.teamAScore, 8);
    });

    test('starting mid-set interval changes phase to running', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      controller.startCurrentInterval();

      expect(controller.phase, MatchPhase.midSetIntervalRunning);

      expect(controller.intervalRemaining, const Duration(minutes: 1));

      expect(controller.intervalEndsAt, isNotNull);
    });

    test('finishing ready mid-set interval returns to play', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      controller.finishIntervalEarly();

      expect(controller.phase, MatchPhase.playing);
      expect(controller.intervalRemaining, Duration.zero);
      expect(controller.intervalEndsAt, isNull);
    });

    test('mid-set interval is taken only once per set', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamA', 8);

      expect(controller.midSetIntervalTaken, isTrue);

      addTestPoints(controller, 'teamB', 8);

      expect(controller.phase, MatchPhase.playing);
    });

    test('between-set interval is 2 minutes', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');

      expect(controller.phase, MatchPhase.betweenSetsIntervalReady);

      expect(controller.intervalRemaining, const Duration(minutes: 2));
    });

    test('starting between-set interval changes phase', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');

      controller.startCurrentInterval();

      expect(controller.phase, MatchPhase.betweenSetsIntervalRunning);

      expect(controller.intervalEndsAt, isNotNull);
    });

    test('finishing between-set interval prepares next set', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');

      controller.finishIntervalEarly();

      expect(controller.phase, MatchPhase.preparingNextSet);
    });

    test('cannot start interval while playing normally', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.startCurrentInterval();

      expect(controller.phase, MatchPhase.playing);
    });

    test('cannot finish nonexistent interval', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.finishIntervalEarly();

      expect(controller.phase, MatchPhase.playing);
    });

    test('resumeIntervalIfNeeded does nothing outside running interval', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.resumeIntervalIfNeeded();

      expect(controller.phase, MatchPhase.playing);
    });

    test('resume interval completes when end time is missing', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamA', 8);
      controller.startCurrentInterval();

      controller.intervalEndsAt = null;

      controller.resumeIntervalIfNeeded();

      expect(controller.phase, MatchPhase.playing);
    });

    test('resume interval completes when end time is in the past', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      addTestPoints(controller, 'teamA', 8);
      controller.startCurrentInterval();

      controller.intervalEndsAt = DateTime.now().subtract(
        const Duration(seconds: 1),
      );

      controller.resumeIntervalIfNeeded();

      expect(controller.phase, MatchPhase.playing);
    });

    test('resume interval restores remaining time', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      expect(controller.phase, MatchPhase.midSetIntervalReady);

      controller.startCurrentInterval();

      expect(controller.phase, MatchPhase.midSetIntervalRunning);

      controller.intervalEndsAt = DateTime.now().add(
        const Duration(seconds: 30),
      );

      controller.resumeIntervalIfNeeded();

      expect(controller.intervalRemaining.inSeconds, greaterThan(0));
      expect(controller.intervalRemaining.inSeconds, lessThanOrEqualTo(30));
    });

    test('resume interval marks 20 second threshold', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      for (var i = 0; i < 8; i++) {
        controller.addPoint('teamA');
      }

      expect(controller.phase, MatchPhase.midSetIntervalReady);

      controller.startCurrentInterval();

      expect(controller.phase, MatchPhase.midSetIntervalRunning);

      controller.intervalEndsAt = DateTime.now().add(
        const Duration(seconds: 10),
      );

      controller.resumeIntervalIfNeeded();

      expect(controller.intervalTwentySecondsReached, isTrue);
    });

    test('startNextSet rejects invalid phase', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      controller.startNextSet(
        selectedServerName: 'Alice',
        selectedReceiverName: 'Bob',
      );

      expect(controller.phase, MatchPhase.playing);
    });

    test('startNextSet rejects invalid server', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');
      finishBetweenSetsInterval(controller);

      controller.startNextSet(
        selectedServerName: 'Unknown',
        selectedReceiverName: controller.nextSetReceiverCandidates.first,
      );

      expect(controller.phase, MatchPhase.preparingNextSet);
    });

    test('startNextSet rejects invalid receiver', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');
      finishBetweenSetsInterval(controller);

      controller.startNextSet(
        selectedServerName: controller.nextSetServerCandidates.first,
        selectedReceiverName: 'Unknown',
      );

      expect(controller.phase, MatchPhase.preparingNextSet);
    });

    test('startNextSet starts play with valid players', () {
      final controller = createSinglesController();
      addTearDown(controller.dispose);

      winSet(controller, 'teamA');
      finishBetweenSetsInterval(controller);

      controller.startNextSet(
        selectedServerName: controller.nextSetServerCandidates.first,
        selectedReceiverName: controller.nextSetReceiverCandidates.first,
      );

      expect(controller.phase, MatchPhase.playing);
    });
  });
}
