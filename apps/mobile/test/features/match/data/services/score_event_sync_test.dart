import 'dart:io';

import 'package:badminton_umpire/features/match/data/services/score_event_outbox.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_sync.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/fake_score_server.dart';

void main() {
  late Directory directory;
  late ScoreEventOutbox outbox;
  late FakeScoreServer server;
  late ScoreEventSync sync;

  ScoreEvent point(int sequence, MatchSide side, {String match = 'm1'}) {
    return ScoreEvent(
      id: '$match-$sequence',
      sequence: sequence,
      type: ScoreEventType.point,
      side: side,
      occurredAt: DateTime.utc(
        2026,
        11,
        21,
        18,
      ).add(Duration(seconds: sequence)),
    );
  }

  ScoreEvent undo(int sequence, {String match = 'm1'}) {
    return ScoreEvent(
      id: '$match-$sequence',
      sequence: sequence,
      type: ScoreEventType.undo,
      side: null,
      occurredAt: DateTime.utc(
        2026,
        11,
        21,
        18,
      ).add(Duration(seconds: sequence)),
    );
  }

  List<ScoreEvent> points(int count, {String match = 'm1'}) => [
    for (var sequence = 1; sequence <= count; sequence++)
      point(
        sequence,
        sequence.isEven ? MatchSide.b : MatchSide.a,
        match: match,
      ),
  ];

  setUp(() async {
    directory = await Directory.systemTemp.createTemp('sync_test');
    outbox = ScoreEventOutbox(directory: () async => directory);
    server = FakeScoreServer();
    sync = ScoreEventSync(outbox: outbox, api: server);
  });

  tearDown(() async {
    await directory.delete(recursive: true);
  });

  group('ScoreEventSync', () {
    test('sends the queued events in order and empties the queue', () async {
      await outbox.enqueue('m1', [
        point(1, MatchSide.a),
        point(2, MatchSide.a),
        undo(3),
      ]);

      final report = await sync.syncNow();

      expect(report.status, ScoreSyncStatus.upToDate);
      expect(report.confirmedCount, 3);
      expect(await outbox.pendingCount(), 0);
      expect(server.score('m1'), (a: 1, b: 0));
    });

    test('sends large queues in batches of 100', () async {
      await outbox.enqueue('m1', points(250));

      await sync.syncNow();

      expect(server.requestCount, 3);
      expect(server.applied['m1'], hasLength(250));
      expect(await outbox.pendingCount(), 0);
    });

    test('keeps everything while offline, then sends it once', () async {
      await outbox.enqueue('m1', points(5));
      server.online = false;

      final offline = await sync.syncNow();

      expect(offline.status, ScoreSyncStatus.offline);
      expect(offline.pendingCount, 5);
      expect(server.applied, isEmpty);

      server.online = true;
      final online = await sync.syncNow();

      expect(online.status, ScoreSyncStatus.upToDate);
      expect(server.applied['m1'], hasLength(5));
    });

    test('counts nothing twice when an answer is lost', () async {
      await outbox.enqueue('m1', points(4));
      server.loseNextAnswer = true;

      final lost = await sync.syncNow();
      expect(lost.status, ScoreSyncStatus.offline);
      expect(await outbox.pendingCount(), 4);

      await outbox.enqueue('m1', [...points(4), point(5, MatchSide.a)]);
      final retry = await sync.syncNow();

      expect(retry.status, ScoreSyncStatus.upToDate);
      expect(server.applied['m1'], hasLength(5));
      expect(server.score('m1'), (a: 3, b: 2));
    });

    test(
      'keeps the events after a server error and sends them later',
      () async {
        await outbox.enqueue('m1', points(2));
        server.failNextWithServerError = true;

        expect((await sync.syncNow()).status, ScoreSyncStatus.offline);
        expect((await sync.syncNow()).status, ScoreSyncStatus.upToDate);
        expect(server.applied['m1'], hasLength(2));
      },
    );

    test('keeps refused events and still sends the other matches', () async {
      await outbox.enqueue('m1', points(2));
      await outbox.enqueue('m2', points(3, match: 'm2'));
      server.rejectedMatches['m1'] = 'MATCH_NOT_IN_PROGRESS';

      final report = await sync.syncNow();

      expect(report.status, ScoreSyncStatus.blocked);
      expect(report.problems, {
        'm1': const ScoreSyncProblem.rejected('MATCH_NOT_IN_PROGRESS'),
      });
      expect(await outbox.pending('m1'), hasLength(2));
      expect(await outbox.pending('m2'), isEmpty);
      expect(server.applied['m2'], hasLength(3));
    });

    test('reports a sequence gap without dropping anything', () async {
      await outbox.enqueue('m1', [
        point(2, MatchSide.a),
        point(3, MatchSide.b),
      ]);

      final report = await sync.syncNow();

      expect(report.problems['m1'], const ScoreSyncProblem.sequenceGap(1));
      expect(await outbox.pending('m1'), hasLength(2));
    });

    test('reports a conflicting event id', () async {
      await outbox.enqueue('m1', [point(1, MatchSide.a)]);
      await sync.syncNow();
      server.applied['m1'] = [point(1, MatchSide.b)];
      final other = ScoreEvent(
        id: 'm1-1',
        sequence: 1,
        type: ScoreEventType.point,
        side: MatchSide.a,
        occurredAt: DateTime.utc(2026),
      );
      final freshDirectory = await Directory.systemTemp.createTemp('fresh');
      addTearDown(() => freshDirectory.delete(recursive: true));
      final freshOutbox = ScoreEventOutbox(
        directory: () async => freshDirectory,
      );
      await freshOutbox.enqueue('m1', [other]);

      final report = await ScoreEventSync(
        outbox: freshOutbox,
        api: server,
      ).syncNow();

      expect(
        report.problems['m1'],
        const ScoreSyncProblem.rejected('EVENT_ID_CONFLICT'),
      );
    });

    test('sends events queued during a sync in the same call', () async {
      await outbox.enqueue('m1', points(2));

      final first = sync.syncNow();
      await outbox.enqueue('m1', [...points(2), point(3, MatchSide.a)]);
      final second = sync.syncNow();

      expect(identical(first, second), isTrue);
      final report = await first;
      expect(report.pendingCount, 0);
      expect(server.applied['m1'], hasLength(3));
    });

    test('never sends the same events twice at the same time', () async {
      await outbox.enqueue('m1', points(10));

      await Future.wait([sync.syncNow(), sync.syncNow(), sync.syncNow()]);

      expect(server.applied['m1'], hasLength(10));
    });
  });

  group('ScoreEventSyncScheduler', () {
    const retryDelays = [
      Duration(milliseconds: 10),
      Duration(milliseconds: 20),
    ];

    /// Waits until [condition] is true, for at most 2 seconds.
    Future<void> waitUntil(bool Function() condition) async {
      final deadline = DateTime.now().add(const Duration(seconds: 2));
      while (!condition()) {
        if (DateTime.now().isAfter(deadline)) {
          fail('Condition not reached in time');
        }
        await Future<void>.delayed(const Duration(milliseconds: 5));
      }
    }

    test('tries again later, waiting longer each time', () async {
      await outbox.enqueue('m1', points(1));
      server.online = false;
      final scheduler = ScoreEventSyncScheduler(sync, retryDelays: retryDelays);
      addTearDown(scheduler.dispose);

      await scheduler.syncNow();
      expect(scheduler.lastReport.value!.status, ScoreSyncStatus.offline);
      expect(scheduler.nextRetryDelay, retryDelays[0]);

      // A retry may be running: wait until the next one is planned.
      await waitUntil(
        () => server.attemptCount >= 3 && scheduler.nextRetryDelay != null,
      );
      expect(scheduler.nextRetryDelay, retryDelays[1]);

      server.online = true;
      await waitUntil(
        () => scheduler.lastReport.value?.status == ScoreSyncStatus.upToDate,
      );
      expect(scheduler.nextRetryDelay, isNull);
      expect(server.applied['m1'], hasLength(1));
    });

    test('a manual sync cancels the planned retry', () async {
      await outbox.enqueue('m1', points(1));
      server.online = false;
      final scheduler = ScoreEventSyncScheduler(
        sync,
        retryDelays: const [Duration(minutes: 1)],
      );
      addTearDown(scheduler.dispose);

      await scheduler.syncNow();
      expect(scheduler.nextRetryDelay, const Duration(minutes: 1));

      server.online = true;
      await scheduler.syncNow();

      expect(scheduler.nextRetryDelay, isNull);
      expect(scheduler.lastReport.value!.status, ScoreSyncStatus.upToDate);
    });

    test('plans no retry when everything is sent', () async {
      await outbox.enqueue('m1', points(3));
      final scheduler = ScoreEventSyncScheduler(sync, retryDelays: retryDelays);
      addTearDown(scheduler.dispose);

      await scheduler.syncNow();

      expect(scheduler.nextRetryDelay, isNull);
      expect(scheduler.lastReport.value!.confirmedCount, 3);
    });
  });
}
