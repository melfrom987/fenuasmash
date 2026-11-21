import 'dart:io';

import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_outbox.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_sync.dart';
import 'package:badminton_umpire/features/match/domain/models/match_draft.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:flutter_test/flutter_test.dart';

import 'helpers/fake_score_server.dart';
import 'helpers/match_test_helpers.dart';

/// The umpire tablet as the match page runs it: after each point or undo, the
/// match is saved, its events are queued, and a sync is started.
///
/// Only the files of [directory] survive a [restart], like on a real device.
class _Tablet {
  _Tablet(this.directory, this.server)
    : matchId = 'match-1',
      controller = createSinglesController() {
    _open();
  }

  final Directory directory;
  final FakeScoreServer server;
  String matchId;
  MatchController controller;

  late ScoreEventOutbox outbox;
  late ScoreEventSync sync;

  /// The saved current match, as JSON, like `current_match.json`.
  Map<String, dynamic>? savedMatch;

  void _open() {
    outbox = ScoreEventOutbox(directory: () async => directory);
    sync = ScoreEventSync(outbox: outbox, api: server);
  }

  Future<ScoreSyncReport> point(String teamId) async {
    addTestPoint(controller, teamId);
    return _save();
  }

  Future<ScoreSyncReport> undo() async {
    controller.undoLastPoint();
    return _save();
  }

  Future<ScoreSyncReport> _save() async {
    savedMatch = draftOf(
      controller,
      matchId: matchId,
      savedAt: DateTime.utc(2026, 11, 21),
    ).toJson();
    await outbox.enqueue(matchId, controller.scoreEvents);
    return sync.syncNow();
  }

  /// The app is closed and opened again: everything in memory is lost, the
  /// match comes back from its saved file.
  void restart() {
    controller = MatchController.fromDraft(MatchDraft.fromJson(savedMatch!));
    _open();
  }

  /// A new match replaces the current one before its points were sent, as the
  /// home page does.
  Future<void> replaceMatch(String newMatchId) async {
    await outbox.enqueue(matchId, controller.scoreEvents);
    matchId = newMatchId;
    controller = createSinglesController();
    savedMatch = null;
  }
}

void main() {
  late Directory directory;
  late FakeScoreServer server;
  late _Tablet tablet;

  setUp(() async {
    directory = await Directory.systemTemp.createTemp('offline_sync_test');
    server = FakeScoreServer();
    tablet = _Tablet(directory, server);
  });

  tearDown(() async {
    await directory.delete(recursive: true);
  });

  /// No point lost, no duplicate: the server holds exactly the events of the
  /// tablet, in the same order, and the queue is empty.
  Future<void> expectServerMatchesTablet(String matchId) async {
    expect(
      server.applied[matchId]?.map((event) => event.id),
      tablet.controller.scoreEvents.map((event) => event.id),
    );
    expect(await tablet.outbox.pending(matchId), isEmpty);
  }

  /// Points of the current set, as shown on the tablet.
  ({int a, int b}) tabletScore() =>
      (a: tablet.controller.teamAScore, b: tablet.controller.teamBScore);

  test('points entered offline reach the server when it returns', () async {
    await tablet.point('teamA');
    server.online = false;

    await tablet.point('teamB');
    await tablet.point('teamB');
    final offline = await tablet.undo();

    expect(offline.status, ScoreSyncStatus.offline);
    expect(offline.pendingCount, 3);
    expect(server.score('match-1'), (a: 1, b: 0));

    server.online = true;
    final back = await tablet.point('teamA');

    expect(back.status, ScoreSyncStatus.upToDate);
    expect(server.score('match-1'), tabletScore());
    expect(server.score('match-1'), (a: 2, b: 1));
    await expectServerMatchesTablet('match-1');
  });

  test('the network cuts in the middle of a sync in several batches', () async {
    server.online = false;
    for (var i = 0; i < 60; i++) {
      await tablet.point(i.isEven ? 'teamA' : 'teamB');
      await tablet.undo();
    }
    await tablet.point('teamA');
    expect(await tablet.outbox.pendingCount(), 121);

    server
      ..online = true
      ..requestsBeforeCut = 1;
    final cut = await tablet.sync.syncNow();

    expect(cut.status, ScoreSyncStatus.offline);
    expect(server.applied['match-1'], hasLength(100));
    expect(cut.pendingCount, 21);

    server.online = true;
    final back = await tablet.sync.syncNow();

    expect(back.status, ScoreSyncStatus.upToDate);
    expect(server.score('match-1'), (a: 1, b: 0));
    await expectServerMatchesTablet('match-1');
  });

  test('a lost answer never counts a point twice', () async {
    await tablet.point('teamA');
    server.loseNextAnswer = true;

    final lost = await tablet.point('teamB');

    expect(lost.status, ScoreSyncStatus.offline);
    expect(server.applied['match-1'], hasLength(2));
    expect(await tablet.outbox.pendingCount(), 1);

    final next = await tablet.point('teamB');

    expect(next.status, ScoreSyncStatus.upToDate);
    expect(server.score('match-1'), (a: 1, b: 2));
    await expectServerMatchesTablet('match-1');
  });

  test('the same events sent twice are applied once', () async {
    await tablet.point('teamA');
    await tablet.point('teamB');

    // Another queue that missed the confirmation sends the same events again.
    final staleDirectory = await Directory.systemTemp.createTemp('stale');
    addTearDown(() => staleDirectory.delete(recursive: true));
    final staleOutbox = ScoreEventOutbox(directory: () async => staleDirectory);
    await staleOutbox.enqueue('match-1', tablet.controller.scoreEvents);

    final report = await ScoreEventSync(
      outbox: staleOutbox,
      api: server,
    ).syncNow();

    expect(report.status, ScoreSyncStatus.upToDate);
    expect(await staleOutbox.pendingCount(), 0);
    expect(server.score('match-1'), (a: 1, b: 1));
    await expectServerMatchesTablet('match-1');
  });

  test('points survive the app being closed while offline', () async {
    await tablet.point('teamA');
    server.online = false;
    await tablet.point('teamA');
    await tablet.point('teamB');

    tablet.restart();

    expect(await tablet.outbox.pendingCount(), 2);
    expect(tabletScore(), (a: 2, b: 1));

    server.online = true;
    final report = await tablet.point('teamB');

    expect(report.status, ScoreSyncStatus.upToDate);
    expect(server.score('match-1'), (a: 2, b: 2));
    await expectServerMatchesTablet('match-1');
  });

  test('the app is closed after the server applied a batch but before the '
      'tablet saw the answer', () async {
    await tablet.point('teamA');
    server.loseNextAnswer = true;
    await tablet.point('teamB');
    await tablet.point('teamB');

    tablet.restart();
    final report = await tablet.sync.syncNow();

    expect(report.status, ScoreSyncStatus.upToDate);
    expect(server.score('match-1'), (a: 1, b: 2));
    await expectServerMatchesTablet('match-1');
  });

  test(
    'a match replaced before its points were sent still sends them',
    () async {
      server.online = false;
      await tablet.point('teamA');
      await tablet.point('teamA');
      final firstMatchEvents = tablet.controller.scoreEvents;

      await tablet.replaceMatch('match-2');
      await tablet.point('teamB');

      server.online = true;
      final report = await tablet.sync.syncNow();

      expect(report.status, ScoreSyncStatus.upToDate);
      expect(
        server.applied['match-1']?.map((event) => event.id),
        firstMatchEvents.map((event) => event.id),
      );
      expect(server.score('match-1'), (a: 2, b: 0));
      await expectServerMatchesTablet('match-2');
    },
  );

  test('a whole match with cuts, lost answers and restarts ends up on the '
      'server exactly once', () async {
    var step = 0;

    /// Changes the network in a fixed pattern, so the test always runs the
    /// same way.
    Future<void> disturb() async {
      step++;
      server.online = step % 7 != 0 && step % 7 != 1;
      if (step % 11 == 0) {
        server.loseNextAnswer = true;
      }
      if (step % 13 == 0) {
        server.failNextWithServerError = true;
      }
      if (step % 17 == 0) {
        tablet.restart();
      }
    }

    while (!tablet.controller.matchIsFinished) {
      await disturb();
      if (step % 5 == 0) {
        await tablet.undo();
      }
      await disturb();
      final winner = step % 3 == 0 ? 'teamB' : 'teamA';
      await tablet.point(winner);
      if (tablet.controller.phase == MatchPhase.betweenSetsIntervalReady) {
        prepareNextSet(tablet.controller);
      }
    }

    server
      ..online = true
      ..loseNextAnswer = false
      ..failNextWithServerError = false;
    final report = await tablet.sync.syncNow();

    expect(report.status, ScoreSyncStatus.upToDate);
    expect(tablet.controller.scoreEvents.length, greaterThan(50));
    await expectServerMatchesTablet('match-1');
  });
}
