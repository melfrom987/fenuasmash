import 'dart:io';

import 'package:badminton_umpire/features/match/data/services/score_event_outbox.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late Directory directory;

  ScoreEventOutbox outbox() =>
      ScoreEventOutbox(directory: () async => directory);

  ScoreEvent point(int sequence, {String? id}) {
    return ScoreEvent(
      id: id ?? 'event-$sequence',
      sequence: sequence,
      type: ScoreEventType.point,
      side: MatchSide.a,
      occurredAt: DateTime.utc(2026, 11, 21, 18, 30, sequence),
    );
  }

  List<String> ids(List<ScoreEvent> events) =>
      events.map((event) => event.id).toList();

  setUp(() async {
    directory = await Directory.systemTemp.createTemp('outbox_test');
  });

  tearDown(() async {
    await directory.delete(recursive: true);
  });

  test('is empty at first', () async {
    expect(await outbox().pending('match-1'), isEmpty);
    expect(await outbox().pendingCount(), 0);
  });

  test('keeps queued events in sequence order', () async {
    await outbox().enqueue('match-1', [point(2), point(1), point(3)]);

    expect(ids(await outbox().pending('match-1')), [
      'event-1',
      'event-2',
      'event-3',
    ]);
  });

  test('keeps the events after a restart', () async {
    await outbox().enqueue('match-1', [point(1), point(2)]);

    final afterRestart = outbox();

    expect(ids(await afterRestart.pending('match-1')), ['event-1', 'event-2']);
  });

  test('queues each event once', () async {
    final queue = outbox();
    await queue.enqueue('match-1', [point(1)]);
    await queue.enqueue('match-1', [point(1), point(2)]);
    await queue.enqueue('match-1', [point(1), point(2)]);

    expect(ids(await queue.pending('match-1')), ['event-1', 'event-2']);
  });

  test('removes confirmed events and never queues them again', () async {
    final queue = outbox();
    await queue.enqueue('match-1', [point(1), point(2), point(3)]);

    await queue.confirm('match-1', ['event-1', 'event-2']);
    await queue.enqueue('match-1', [point(1), point(2), point(3), point(4)]);

    expect(ids(await queue.pending('match-1')), ['event-3', 'event-4']);
  });

  test('remembers confirmations after a restart', () async {
    await outbox().enqueue('match-1', [point(1), point(2)]);
    await outbox().confirm('match-1', ['event-1', 'event-2']);

    await outbox().enqueue('match-1', [point(1), point(2), point(3)]);

    expect(ids(await outbox().pending('match-1')), ['event-3']);
  });

  test('ignores unknown confirmed ids', () async {
    final queue = outbox();
    await queue.enqueue('match-1', [point(1)]);

    await queue.confirm('match-1', ['unknown']);
    await queue.confirm('match-2', ['event-1']);

    expect(ids(await queue.pending('match-1')), ['event-1']);
  });

  test('keeps the events of each match apart', () async {
    final queue = outbox();
    await queue.enqueue('match-1', [point(1), point(2)]);
    await queue.enqueue('match-2', [point(1, id: 'other-1')]);

    await queue.confirm('match-1', ['event-1', 'event-2']);

    expect(await queue.pending('match-1'), isEmpty);
    expect(ids(await queue.pending('match-2')), ['other-1']);
    expect(await queue.matchesWithPendingEvents(), ['match-2']);
    expect(await queue.pendingCount(), 1);
  });

  test('loses nothing when events are queued at the same time', () async {
    final queue = outbox();

    await Future.wait([
      for (var sequence = 1; sequence <= 50; sequence++)
        queue.enqueue('match-1', [point(sequence)]),
    ]);

    expect(await outbox().pending('match-1'), hasLength(50));
  });

  test('keeps the same content after a confirmation', () async {
    final queue = outbox();
    final event = point(1);
    await queue.enqueue('match-1', [event, point(2)]);

    await queue.confirm('match-1', ['event-2']);

    expect((await outbox().pending('match-1')).single.toJson(), event.toJson());
  });

  test('leaves no temporary file behind', () async {
    await outbox().enqueue('match-1', [point(1)]);

    final files = directory.listSync().map(
      (file) => file.uri.pathSegments.last,
    );
    expect(files, [ScoreEventOutbox.fileName]);
  });

  test('keeps an unreadable queue aside instead of deleting it', () async {
    final file = File('${directory.path}/${ScoreEventOutbox.fileName}');
    await file.writeAsString('{not json');

    final queue = outbox();
    expect(await queue.pending('match-1'), isEmpty);
    await queue.enqueue('match-1', [point(1)]);

    final names = directory
        .listSync()
        .map((file) => file.uri.pathSegments.last)
        .toList();
    expect(names, contains(ScoreEventOutbox.fileName));
    expect(names.where((name) => name.contains('.unreadable-')), hasLength(1));
    expect(ids(await queue.pending('match-1')), ['event-1']);
  });
}
