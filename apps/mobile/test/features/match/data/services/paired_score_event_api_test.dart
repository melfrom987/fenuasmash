import 'package:badminton_umpire/features/match/data/services/paired_score_event_api.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_api.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:badminton_umpire/features/pairing/data/tablet_pairing_api.dart';
import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../../pairing/helpers.dart';

class _RecordingApi implements ScoreEventApi {
  _RecordingApi(this.session, this.calls, this.result);

  final TabletSession session;
  final List<({TabletSession session, String matchId})> calls;
  final ScoreEventSendResult result;

  @override
  Future<ScoreEventSendResult> send(
    String matchId,
    List<ScoreEvent> events,
  ) async {
    calls.add((session: session, matchId: matchId));
    return result;
  }
}

void main() {
  late MemorySessionStore store;
  late FakePairingApi pairingApi;
  late TabletSessionController pairing;
  late List<({TabletSession session, String matchId})> calls;
  late ScoreEventSendResult answer;
  late PairedScoreEventApi api;

  final event = ScoreEvent(
    id: 'event-1',
    sequence: 1,
    type: ScoreEventType.point,
    side: MatchSide.a,
    occurredAt: DateTime.utc(2026, 11, 21, 9),
  );

  setUp(() async {
    store = MemorySessionStore()..saved = sessionOnCourt(3);
    pairingApi = FakePairingApi();
    pairing = TabletSessionController(store: store, api: pairingApi);
    await pairing.load();
    calls = [];
    answer = const ScoreEventsConfirmed(
      acceptedIds: ['event-1'],
      duplicateIds: [],
      lastSequence: 1,
    );
    api = PairedScoreEventApi(
      pairing: pairing,
      apiFor: (session) => _RecordingApi(session, calls, answer),
    );
  });

  test('sends to the server and with the token of the pairing', () async {
    final result = await api.send('match-1', [event]);

    expect(result, isA<ScoreEventsConfirmed>());
    expect(calls.single.matchId, 'match-1');
    expect(calls.single.session, sessionOnCourt(3));
  });

  test('keeps the events while the tablet is not paired', () async {
    await pairing.unpair();

    final result = await api.send('match-1', [event]);

    expect(result, isA<ScoreEventsUnreachable>());
    expect(
      (result as ScoreEventsUnreachable).reason,
      PairedScoreEventApi.notPaired,
    );
    expect(calls, isEmpty);
  });

  test('uses the new pairing at the next send', () async {
    await api.send('match-1', [event]);
    final other = sessionOnCourt(5, token: 'fst_other');
    pairingApi.pairResult = Paired(other);
    await pairing.pair(other.apiBaseUri, 'K7MP4X');

    await api.send('match-1', [event]);

    expect(calls.last.session, other);
  });

  for (final code in ['TABLET_REVOKED', 'HTTP_401']) {
    test(
      'a refused token ($code) makes the pairing check the tablet',
      () async {
        answer = ScoreEventsRejected(code: code);
        pairingApi.checkResult = (
          status: TabletCheck.revoked,
          courtNumber: null,
        );

        final result = await api.send('match-1', [event]);
        await pumpEventQueue();

        expect(result, isA<ScoreEventsRejected>());
        expect(pairingApi.checkCount, 1);
        expect(pairing.state, TabletPairingState.revoked);
      },
    );
  }

  test('another refusal does not check the tablet', () async {
    answer = const ScoreEventsRejected(code: 'MATCH_NOT_IN_PROGRESS');

    await api.send('match-1', [event]);
    await pumpEventQueue();

    expect(pairingApi.checkCount, 0);
    expect(pairing.state, TabletPairingState.paired);
  });
}
