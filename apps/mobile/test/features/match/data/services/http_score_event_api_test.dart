import 'dart:convert';
import 'dart:io';

import 'package:badminton_umpire/features/match/data/services/http_score_event_api.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_api.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:flutter_test/flutter_test.dart';

/// A request received by the local server.
class _Received {
  _Received(this.method, this.uri, this.headers, this.body);

  final String method;
  final Uri uri;
  final HttpHeaders headers;
  final Map<String, dynamic> body;
}

/// The answers below are the ones of the backend (`ScoreService`, #103).
void main() {
  late HttpServer server;
  late HttpScoreEventApi api;
  late List<_Received> received;
  late Future<void> Function(HttpResponse response) answer;
  String? token;

  final matchId = '9b2f6a52-7c1e-4d5e-8a3b-1f0c2d4e6a8b';
  final events = [
    ScoreEvent(
      id: '0f8e2c1a-1b2c-4d3e-8f9a-0b1c2d3e4f5a',
      sequence: 1,
      type: ScoreEventType.point,
      side: MatchSide.a,
      occurredAt: DateTime.utc(2026, 11, 21, 18, 0, 1),
    ),
    ScoreEvent(
      id: '1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d',
      sequence: 2,
      type: ScoreEventType.undo,
      side: null,
      occurredAt: DateTime.utc(2026, 11, 21, 18, 0, 2),
    ),
  ];

  Future<void> json(HttpResponse response, int status, Object body) async {
    response
      ..statusCode = status
      ..headers.contentType = status == 200
          ? ContentType.json
          : ContentType('application', 'problem+json')
      ..write(jsonEncode(body));
    await response.close();
  }

  Map<String, Object> problem(int status, String code) => {
    'type': 'about:blank',
    'title': 'Conflict',
    'status': status,
    'detail': 'Technical message',
    'instance': '/api/matches/$matchId/events',
    'code': code,
  };

  setUp(() async {
    received = [];
    token = 'fst_secret';
    answer = (response) => json(response, 200, {
      'acceptedIds': [events[0].id, events[1].id],
      'duplicateIds': <String>[],
      'lastSequence': 2,
      'score': {'games': <Object>[], 'winner': null},
      'status': 'IN_PROGRESS',
      'stateVersion': 6,
    });
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    server.listen((request) async {
      final body = await utf8.decoder.bind(request).join();
      received.add(
        _Received(
          request.method,
          request.uri,
          request.headers,
          body.isEmpty ? {} : jsonDecode(body) as Map<String, dynamic>,
        ),
      );
      await answer(request.response);
    });
    api = HttpScoreEventApi(
      baseUri: Uri.parse('http://127.0.0.1:${server.port}/api'),
      token: () async => token,
      timeout: const Duration(seconds: 2),
    );
  });

  tearDown(() async {
    api.close();
    await server.close(force: true);
  });

  test('posts the events of the match with the tablet token', () async {
    await api.send(matchId, events);

    expect(received, hasLength(1));
    final request = received.single;
    expect(request.method, 'POST');
    expect(request.uri.path, '/api/matches/$matchId/events');
    expect(request.headers.value('authorization'), 'Bearer fst_secret');
    expect(request.headers.contentType?.mimeType, 'application/json');
    expect(request.body, {
      'events': [
        {
          'id': events[0].id,
          'sequence': 1,
          'type': 'POINT',
          'side': 'A',
          'occurredAt': '2026-11-21T18:00:01.000Z',
        },
        {
          'id': events[1].id,
          'sequence': 2,
          'type': 'UNDO',
          'occurredAt': '2026-11-21T18:00:02.000Z',
        },
      ],
    });
  });

  test('accepts a base address with a trailing slash', () async {
    final slashed = HttpScoreEventApi(
      baseUri: Uri.parse('http://127.0.0.1:${server.port}/api/'),
      token: () async => token,
    );
    addTearDown(slashed.close);

    await slashed.send(matchId, events);

    expect(received.single.uri.path, '/api/matches/$matchId/events');
  });

  test('a 200 confirms the accepted and duplicate events', () async {
    answer = (response) => json(response, 200, {
      'acceptedIds': [events[1].id],
      'duplicateIds': [events[0].id],
      'lastSequence': 2,
      'score': {
        'games': [
          {'a': 1, 'b': 0},
        ],
        'winner': null,
      },
      'status': 'IN_PROGRESS',
      'stateVersion': 6,
    });

    final result = await api.send(matchId, events);

    expect(result, isA<ScoreEventsConfirmed>());
    final confirmed = result as ScoreEventsConfirmed;
    expect(confirmed.acceptedIds, [events[1].id]);
    expect(confirmed.duplicateIds, [events[0].id]);
    expect(confirmed.lastSequence, 2);
  });

  test('a sequence gap gives the sequence to send again from', () async {
    answer = (response) => json(response, 409, {
      ...problem(409, 'SEQUENCE_GAP'),
      'expectedSequence': 3,
    });

    final result = await api.send(matchId, events);

    expect(result, isA<ScoreEventsSequenceGap>());
    expect((result as ScoreEventsSequenceGap).expectedSequence, 3);
  });

  test('other refusals keep the code of the backend', () async {
    final refusals = {
      409: [
        'EVENT_ID_CONFLICT',
        'SEQUENCE_ALREADY_USED',
        'MATCH_NOT_IN_PROGRESS',
        'COURT_OCCUPIED',
      ],
      400: ['INVALID_SCORE_EVENT', 'VALIDATION_FAILED'],
      401: ['TABLET_REVOKED', 'UNAUTHORIZED'],
      403: ['MATCH_NOT_ON_TABLET_COURT'],
      404: ['MATCH_NOT_FOUND'],
    };
    for (final MapEntry(key: status, value: codes) in refusals.entries) {
      for (final code in codes) {
        answer = (response) => json(response, status, problem(status, code));

        final result = await api.send(matchId, events);

        expect(result, isA<ScoreEventsRejected>(), reason: code);
        expect((result as ScoreEventsRejected).code, code);
      }
    }
  });

  test('a refusal without a problem body keeps the status', () async {
    answer = (response) async {
      response.statusCode = 403;
      await response.close();
    };

    final result = await api.send(matchId, events);

    expect((result as ScoreEventsRejected).code, 'HTTP_403');
  });

  test('server errors, timeouts and rate limits are temporary', () async {
    for (final status in [500, 502, 503, 408, 429]) {
      answer = (response) => json(response, status, problem(status, 'X'));

      final result = await api.send(matchId, events);

      expect(result, isA<ScoreEventsUnreachable>(), reason: '$status');
    }
  });

  test('a 200 that is not the API is not taken as a confirmation', () async {
    answer = (response) async {
      response
        ..statusCode = 200
        ..headers.contentType = ContentType.html
        ..write('<html>Wi-Fi login</html>');
      await response.close();
    };

    expect(await api.send(matchId, events), isA<ScoreEventsUnreachable>());
  });

  test('an answer that takes too long is a timeout', () async {
    answer = (response) async {
      await Future<void>.delayed(const Duration(seconds: 3));
      await json(response, 200, {});
    };

    final result = await api.send(matchId, events);

    expect(result, isA<ScoreEventsUnreachable>());
    expect((result as ScoreEventsUnreachable).reason, 'timeout');
  });

  test('no server is unreachable', () async {
    await server.close(force: true);

    expect(await api.send(matchId, events), isA<ScoreEventsUnreachable>());
  });

  test('a tablet without token sends nothing', () async {
    token = null;

    final result = await api.send(matchId, events);

    expect((result as ScoreEventsRejected).code, HttpScoreEventApi.notPaired);
    expect(received, isEmpty);
  });
}
