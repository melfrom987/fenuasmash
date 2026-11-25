import 'dart:io';

import 'package:badminton_umpire/features/match/application/score_sync.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_outbox.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_sync.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:badminton_umpire/features/pairing/data/tablet_pairing_api.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../pairing/helpers.dart';
import '../helpers/fake_score_server.dart';
import '../helpers/match_test_helpers.dart';

void main() {
  late Directory directory;
  late ScoreEventOutbox outbox;
  late FakeScoreServer server;
  late ScoreEventSyncScheduler scheduler;
  late MemorySessionStore store;
  late FakePairingApi pairingApi;
  late TabletSessionController pairing;

  setUp(() async {
    directory = await Directory.systemTemp.createTemp('score_sync_test');
    outbox = ScoreEventOutbox(directory: () async => directory);
    server = FakeScoreServer();
    scheduler = ScoreEventSyncScheduler(
      ScoreEventSync(outbox: outbox, api: server),
    );
    store = MemorySessionStore();
    pairingApi = FakePairingApi();
    pairing = TabletSessionController(store: store, api: pairingApi);

    final controller = createSinglesController();
    addTestPoints(controller, 'teamA', 2);
    await outbox.enqueue('match-1', controller.scoreEvents);
  });

  /// Waits for the sync started in the background to report.
  Future<void> synced() async {
    for (var i = 0; i < 200 && scheduler.lastReport.value == null; i++) {
      await Future<void>.delayed(const Duration(milliseconds: 10));
    }
  }

  tearDown(() async {
    scheduler.dispose();
    await directory.delete(recursive: true);
  });

  test('sends the points left by a previous run at startup', () async {
    store.saved = sessionOnCourt(3);
    await pairing.load();

    startScoreSync(pairing: pairing, scheduler: scheduler);
    await synced();

    expect(server.applied['match-1'], hasLength(2));
    expect(await outbox.pendingCount(), 0);
  });

  test('waits for the pairing, then sends', () async {
    await pairing.load();

    startScoreSync(pairing: pairing, scheduler: scheduler);
    await pumpEventQueue();
    expect(server.attemptCount, 0);

    pairingApi.pairResult = Paired(sessionOnCourt(3));
    await pairing.pair(sessionOnCourt(3).apiBaseUri, 'K7MP4X');
    await synced();

    expect(server.applied['match-1'], hasLength(2));
  });

  test('sends again when paired with another court', () async {
    store.saved = sessionOnCourt(3);
    await pairing.load();
    server.online = false;
    startScoreSync(pairing: pairing, scheduler: scheduler);
    await synced();
    expect(server.applied, isEmpty);

    server.online = true;
    scheduler.lastReport.value = null;
    pairingApi.pairResult = Paired(sessionOnCourt(5));
    await pairing.pair(sessionOnCourt(5).apiBaseUri, 'K7MP4X');
    await synced();

    expect(server.applied['match-1'], hasLength(2));
  });
}
