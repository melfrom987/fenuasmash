import 'package:badminton_umpire/features/court_match/application/court_match_controller.dart';
import 'package:badminton_umpire/features/court_match/data/court_match_api.dart';
import 'package:badminton_umpire/features/court_match/domain/court_match.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:badminton_umpire/features/pairing/data/tablet_pairing_api.dart';
import 'package:flutter_test/flutter_test.dart';

import '../pairing/helpers.dart';
import 'helpers.dart';

/// `testWidgets` runs the timers on a fake clock: `pump(duration)` moves it.
void main() {
  late MemorySessionStore store;
  late FakePairingApi pairingApi;
  late TabletSessionController pairing;
  late FakeCourtMatchApi api;
  late CourtMatchController controller;

  const interval = Duration(seconds: 10);

  Future<void> setUpPaired({bool paired = true}) async {
    store = MemorySessionStore();
    if (paired) {
      store.saved = sessionOnCourt(3);
    }
    pairingApi = FakePairingApi();
    pairing = TabletSessionController(store: store, api: pairingApi);
    await pairing.load();
    api = FakeCourtMatchApi();
    controller = CourtMatchController(
      pairing: pairing,
      api: api,
      interval: interval,
    );
  }

  /// A test on the fake clock. The polling timer is stopped before the end:
  /// `testWidgets` fails when a timer is still pending.
  void testCourt(String description, WidgetTesterCallback body) {
    testWidgets(description, (tester) async {
      await body(tester);
      controller.dispose();
    });
  }

  testCourt('an unpaired tablet watches no court', (tester) async {
    await setUpPaired(paired: false);

    controller.start();
    await tester.pump(interval * 3);

    expect(controller.state, CourtMatchState.idle);
    expect(api.calls, isEmpty);
  });

  testCourt('shows the match called to the court', (tester) async {
    await setUpPaired();
    api.next = CourtMatchFound(calledMatch());

    controller.start();
    expect(controller.state, CourtMatchState.loading);
    await tester.pump();

    expect(controller.state, CourtMatchState.match);
    expect(controller.match, calledMatch());
    expect(api.calls.single.session.accessToken, 'fst_secret');
  });

  testCourt('the court is read again until a match is called', (tester) async {
    await setUpPaired();
    controller.start();
    await tester.pump();
    expect(controller.state, CourtMatchState.free);

    await tester.pump(interval);
    expect(api.calls, hasLength(2));
    expect(controller.state, CourtMatchState.free);

    // The organizer calls a match
    api.next = CourtMatchFound(calledMatch());
    await tester.pump(interval);
    expect(controller.state, CourtMatchState.match);

    // Then the umpire starts it on another tablet, or the organizer sends it
    // back to the queue
    api.next = CourtMatchFound(
      calledMatch(status: CourtMatchStatus.warmup, stateVersion: 4),
    );
    await tester.pump(interval);
    expect(controller.match!.status, CourtMatchStatus.warmup);

    api.next = const CourtFree();
    await tester.pump(interval);
    expect(controller.state, CourtMatchState.free);
    expect(controller.match, isNull);
  });

  testCourt('the last match is passed back to reuse its labels', (
    tester,
  ) async {
    await setUpPaired();
    api.next = CourtMatchFound(calledMatch());
    controller.start();
    await tester.pump();

    await tester.pump(interval);

    expect(api.calls.first.known, isNull);
    expect(api.calls.last.known, calledMatch());
  });

  testCourt('listeners hear only the changes', (tester) async {
    await setUpPaired();
    api.next = CourtMatchFound(calledMatch());
    controller.start();
    await tester.pump();
    var notified = 0;
    controller.addListener(() => notified++);

    await tester.pump(interval * 3);

    expect(notified, 0);
  });

  testCourt('without network, the last match stays shown', (tester) async {
    await setUpPaired();
    api.next = CourtMatchFound(calledMatch());
    controller.start();
    await tester.pump();
    final updatedAt = controller.updatedAt;

    api.next = const CourtMatchUnreachable();
    await tester.pump(interval);

    expect(controller.state, CourtMatchState.match);
    expect(controller.match, calledMatch());
    expect(controller.offline, isTrue);
    expect(controller.updatedAt, updatedAt);

    // The network is back
    api.next = CourtMatchFound(calledMatch());
    await tester.pump(interval);
    expect(controller.offline, isFalse);
  });

  testCourt('offline from the start, nothing is known', (tester) async {
    await setUpPaired();
    api.next = const CourtMatchUnreachable();

    controller.start();
    await tester.pump();

    expect(controller.state, CourtMatchState.loading);
    expect(controller.offline, isTrue);
    expect(controller.updatedAt, isNull);
  });

  testCourt('a revoked tablet ends the pairing and stops watching', (
    tester,
  ) async {
    await setUpPaired();
    controller.start();
    await tester.pump();

    api.next = const CourtMatchRevoked();
    pairingApi.checkResult = (status: TabletCheck.revoked, courtNumber: null);
    await tester.pump(interval);

    expect(pairingApi.checkCount, 1);
    expect(pairing.state, TabletPairingState.revoked);
    expect(store.saved, isNull);
    expect(controller.state, CourtMatchState.idle);

    final calls = api.calls.length;
    await tester.pump(interval * 3);
    expect(api.calls, hasLength(calls));
  });

  testCourt('a 401 the pairing does not confirm keeps the session', (
    tester,
  ) async {
    await setUpPaired();
    controller.start();
    await tester.pump();

    api.next = const CourtMatchRevoked();
    await tester.pump(interval);

    expect(pairing.state, TabletPairingState.paired);
    expect(controller.state, CourtMatchState.free);
  });

  testCourt('unpairing stops watching, pairing again starts over', (
    tester,
  ) async {
    await setUpPaired();
    api.next = CourtMatchFound(calledMatch());
    controller.start();
    await tester.pump();

    await pairing.unpair();
    expect(controller.state, CourtMatchState.idle);
    expect(controller.match, isNull);
    final calls = api.calls.length;
    await tester.pump(interval * 2);
    expect(api.calls, hasLength(calls));

    pairingApi.pairResult = Paired(sessionOnCourt(5, token: 'fst_other'));
    api.next = const CourtFree();
    await pairing.pair(Uri.parse('http://10.0.0.5:8081/api'), 'ABC123');
    await tester.pump();

    expect(controller.state, CourtMatchState.free);
    expect(api.calls.last.session.accessToken, 'fst_other');
  });

  testCourt('stopped in the background, the match stays until read again', (
    tester,
  ) async {
    await setUpPaired();
    api.next = CourtMatchFound(calledMatch());
    controller.start();
    await tester.pump();

    controller.stop();
    final calls = api.calls.length;
    await tester.pump(interval * 3);
    expect(api.calls, hasLength(calls));
    expect(controller.match, calledMatch());

    api.next = const CourtFree();
    controller.start();
    expect(controller.state, CourtMatchState.match);
    await tester.pump();
    expect(api.calls, hasLength(calls + 1));
    expect(controller.state, CourtMatchState.free);
  });

  testCourt('reads asked during a read wait for it', (tester) async {
    await setUpPaired();
    controller.start();

    await Future.wait([controller.refresh(), controller.refresh()]);

    expect(api.calls, hasLength(1));
  });
}
