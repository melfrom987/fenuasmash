import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:badminton_umpire/features/pairing/data/tablet_pairing_api.dart';
import 'package:flutter_test/flutter_test.dart';

import 'helpers.dart';

void main() {
  late MemorySessionStore store;
  late FakePairingApi api;

  TabletSessionController restart() =>
      TabletSessionController(store: store, api: api);

  setUp(() {
    store = MemorySessionStore();
    api = FakePairingApi();
  });

  test('starts unpaired without a saved session', () async {
    final controller = restart();
    expect(controller.state, TabletPairingState.loading);

    await controller.load();

    expect(controller.state, TabletPairingState.unpaired);
    expect(await controller.accessToken(), isNull);
  });

  test('a successful pairing is kept across restarts', () async {
    api.pairResult = Paired(sessionOnCourt(2));
    final controller = restart();
    await controller.load();

    await controller.pair(
      Uri.parse('http://10.0.0.5:8081/api'),
      ' k7m-p4x ',
      deviceName: '  Galaxy Tab  ',
    );

    expect(api.pairCalls.single.code, 'K7MP4X');
    expect(api.pairCalls.single.deviceName, 'Galaxy Tab');
    expect(controller.state, TabletPairingState.paired);

    final afterRestart = restart();
    await afterRestart.load();
    expect(afterRestart.state, TabletPairingState.paired);
    expect(afterRestart.session, sessionOnCourt(2));
    expect(await afterRestart.accessToken(), 'fst_secret');
  });

  test('an empty device name is not sent', () async {
    api.pairResult = Paired(sessionOnCourt(2));
    final controller = restart();

    await controller.pair(Uri.parse('http://x/api'), 'K7MP4X', deviceName: ' ');

    expect(api.pairCalls.single.deviceName, isNull);
  });

  test('a failed pairing keeps the previous one', () async {
    store.saved = sessionOnCourt(1);
    api.pairResult = const InvalidPairingCode();
    final controller = restart();
    await controller.load();

    final result = await controller.pair(Uri.parse('http://x/api'), 'AAAAAA');

    expect(result, isA<InvalidPairingCode>());
    expect(controller.state, TabletPairingState.paired);
    expect(store.saved, sessionOnCourt(1));
  });

  test('the saved session works offline', () async {
    store.saved = sessionOnCourt(1);
    api.checkResult = (status: TabletCheck.unknown, courtNumber: null);
    final controller = restart();
    await controller.load();

    await controller.verify();

    expect(controller.state, TabletPairingState.paired);
    expect(store.saved, sessionOnCourt(1));
  });

  test('a revoked tablet forgets its session', () async {
    store.saved = sessionOnCourt(1);
    api.checkResult = (status: TabletCheck.revoked, courtNumber: null);
    final controller = restart();
    await controller.load();

    await controller.verify();

    expect(controller.state, TabletPairingState.revoked);
    expect(controller.session, isNull);
    expect(store.saved, isNull);
  });

  test('a new pairing after revocation works', () async {
    store.saved = sessionOnCourt(1);
    api.checkResult = (status: TabletCheck.revoked, courtNumber: null);
    final controller = restart();
    await controller.load();
    await controller.verify();

    api.pairResult = Paired(sessionOnCourt(5));
    await controller.pair(Uri.parse('http://x/api'), 'K7MP4X');

    expect(controller.state, TabletPairingState.paired);
    expect(store.saved, sessionOnCourt(5));
  });

  test('the court number follows the server', () async {
    store.saved = sessionOnCourt(1);
    api.checkResult = (status: TabletCheck.paired, courtNumber: 7);
    final controller = restart();
    await controller.load();

    await controller.verify();

    expect(controller.session!.courtNumber, 7);
    expect(store.saved!.courtNumber, 7);
  });

  test('verify does nothing without a session', () async {
    final controller = restart();
    await controller.load();

    await controller.verify();

    expect(api.checkCount, 0);
  });

  test('unpair forgets the session', () async {
    store.saved = sessionOnCourt(1);
    final controller = restart();
    await controller.load();

    await controller.unpair();

    expect(controller.state, TabletPairingState.unpaired);
    expect(store.saved, isNull);
  });
}
