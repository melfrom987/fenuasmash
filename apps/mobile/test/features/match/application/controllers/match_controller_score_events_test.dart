import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';
import 'package:badminton_umpire/features/match/domain/models/match_draft.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/match_test_helpers.dart';

void main() {
  var nextId = 0;
  final now = DateTime.utc(2026, 11, 21, 18, 30);

  String newId() => 'event-${++nextId}';

  MatchController createController() {
    return MatchController(
      isDoubles: false,
      teamAPlayers: const ['Alice'],
      teamBPlayers: const ['Bob'],
      leftSideTeamId: 'teamA',
      servingTeamId: 'teamA',
      serverName: 'Alice',
      receiverName: 'Bob',
      courtNumber: '1',
      newEventId: newId,
      clock: () => now,
    );
  }

  setUp(() => nextId = 0);

  test('a new match has no score event', () {
    expect(createController().scoreEvents, isEmpty);
  });

  test('records each point with its side, id, sequence and time', () {
    final controller = createController();

    controller.addPoint('teamA');
    controller.addPoint('teamB');

    expect(controller.scoreEvents.map((event) => event.toJson()), [
      {
        'id': 'event-1',
        'sequence': 1,
        'type': 'POINT',
        'side': 'A',
        'occurredAt': '2026-11-21T18:30:00.000Z',
      },
      {
        'id': 'event-2',
        'sequence': 2,
        'type': 'POINT',
        'side': 'B',
        'occurredAt': '2026-11-21T18:30:00.000Z',
      },
    ]);
  });

  test('records an undo after the points, without removing them', () {
    final controller = createController();

    controller.addPoint('teamA');
    controller.addPoint('teamA');
    controller.undoLastPoint();

    expect(controller.teamAScore, 1);
    expect(controller.scoreEvents.map((event) => event.type), [
      ScoreEventType.point,
      ScoreEventType.point,
      ScoreEventType.undo,
    ]);
    expect(controller.scoreEvents.last.sequence, 3);
    expect(controller.scoreEvents.last.side, isNull);
  });

  test('sequences have no gaps and ids are unique', () {
    final controller = MatchController(
      isDoubles: false,
      teamAPlayers: const ['Alice'],
      teamBPlayers: const ['Bob'],
      leftSideTeamId: 'teamA',
      servingTeamId: 'teamA',
      serverName: 'Alice',
      receiverName: 'Bob',
      courtNumber: '1',
    );

    for (var i = 0; i < 30; i++) {
      addTestPoint(controller, i.isEven ? 'teamA' : 'teamB');
      if (i % 7 == 0) {
        controller.undoLastPoint();
      }
    }

    final events = controller.scoreEvents;
    expect(
      events.map((event) => event.sequence),
      List.generate(events.length, (index) => index + 1),
    );
    expect(events.map((event) => event.id).toSet(), hasLength(events.length));
  });

  test('records nothing when a point is refused', () {
    final controller = createController();

    winSet(controller, 'teamA');
    final count = controller.scoreEvents.length;

    expect(controller.phase, isNot(MatchPhase.playing));
    controller.addPoint('teamB');

    expect(controller.scoreEvents, hasLength(count));
  });

  test('records nothing when there is no point to undo', () {
    final controller = createController();

    controller.undoLastPoint();

    expect(controller.scoreEvents, isEmpty);
  });

  test('records the undo of the last point of a finished match', () {
    final controller = createController();

    winSet(controller, 'teamA');
    prepareNextSet(controller);
    winSet(controller, 'teamA');
    expect(controller.matchIsFinished, isTrue);

    controller.undoLastPoint();

    expect(controller.matchIsFinished, isFalse);
    expect(controller.scoreEvents.last.type, ScoreEventType.undo);
  });

  test('keeps the events and the numbering after a restart', () {
    final controller = createController();
    controller.addPoint('teamA');
    controller.addPoint('teamB');

    final saved = MatchDraft.fromJson(
      draftOf(controller, savedAt: now).toJson(),
    );
    final restored = MatchController.fromDraft(
      saved,
      newEventId: newId,
      clock: () => now,
    );
    restored.addPoint('teamA');

    expect(restored.scoreEvents.map((event) => event.id), [
      'event-1',
      'event-2',
      'event-3',
    ]);
    expect(restored.scoreEvents.last.sequence, 3);
  });

  test('reads drafts saved before score events existed', () {
    final json = draftOf(createController(), savedAt: now).toJson()
      ..remove('scoreEvents');

    expect(MatchDraft.fromJson(json).scoreEvents, isEmpty);
  });
}
