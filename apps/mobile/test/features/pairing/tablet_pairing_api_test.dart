import 'dart:convert';
import 'dart:io';

import 'package:badminton_umpire/features/pairing/data/tablet_pairing_api.dart';
import 'package:flutter_test/flutter_test.dart';

import 'helpers.dart';

/// The answers below are the ones of the backend (`TabletController`, #144).
void main() {
  late HttpServer server;
  late HttpTabletPairingApi api;
  late Uri base;
  late Future<void> Function(HttpRequest request) answer;
  final received =
      <({String method, String path, String? auth, String body})>[];

  Future<void> json(
    HttpResponse response,
    int status,
    Object body, {
    Map<String, String> headers = const {},
  }) async {
    response.statusCode = status;
    headers.forEach(response.headers.set);
    response.headers.contentType = ContentType.json;
    response.write(jsonEncode(body));
    await response.close();
  }

  Map<String, Object> problem(int status, String code) => {
    'type': 'about:blank',
    'title': 'Error',
    'status': status,
    'code': code,
  };

  final court = {
    'id': 'court-3',
    'tournamentId': 'tournament-1',
    'number': 3,
    'active': true,
  };

  setUp(() async {
    received.clear();
    answer = (request) => json(request.response, 500, {});
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    server.listen((request) async {
      received.add((
        method: request.method,
        path: request.uri.path,
        auth: request.headers.value('authorization'),
        body: await utf8.decoder.bind(request).join(),
      ));
      await answer(request);
    });
    base = Uri.parse('http://127.0.0.1:${server.port}/api');
    api = HttpTabletPairingApi(timeout: const Duration(seconds: 2));
  });

  tearDown(() async {
    api.close();
    await server.close(force: true);
  });

  group('pair', () {
    test('sends the code and returns the session', () async {
      answer = (request) => json(request.response, 200, {
        'tabletId': 'tablet-9',
        'accessToken': 'fst_new',
        'tokenType': 'Bearer',
        'court': court,
      });

      final result = await api.pair(base, 'K7MP4X', deviceName: 'Tab A8');

      expect(received.single.method, 'POST');
      expect(received.single.path, '/api/tablets/pair');
      expect(received.single.auth, isNull);
      expect(jsonDecode(received.single.body), {
        'code': 'K7MP4X',
        'deviceName': 'Tab A8',
      });
      final session = (result as Paired).session;
      expect(session.apiBaseUri, base);
      expect(session.accessToken, 'fst_new');
      expect(session.tabletId, 'tablet-9');
      expect(session.courtId, 'court-3');
      expect(session.courtNumber, 3);
      expect(session.tournamentId, 'tournament-1');
    });

    test('a wrong code', () async {
      answer = (request) =>
          json(request.response, 400, problem(400, 'INVALID_PAIRING_CODE'));

      expect(await api.pair(base, 'AAAAAA'), isA<InvalidPairingCode>());
    });

    test('too many wrong codes, with the wait', () async {
      answer = (request) => json(
        request.response,
        429,
        problem(429, 'TOO_MANY_PAIRING_ATTEMPTS'),
        headers: {'Retry-After': '600'},
      );

      final result = await api.pair(base, 'AAAAAA');

      expect(
        (result as TooManyPairingAttempts).retryAfter,
        const Duration(minutes: 10),
      );
    });

    test('a disabled court', () async {
      answer = (request) =>
          json(request.response, 409, problem(409, 'COURT_INACTIVE'));

      expect(await api.pair(base, 'K7MP4X'), isA<PairingCourtInactive>());
    });

    test('another refusal keeps its code', () async {
      answer = (request) => json(request.response, 403, problem(403, 'X_Y'));

      expect((await api.pair(base, 'K7MP4X') as PairingFailed).code, 'X_Y');
    });

    test('a server error, no server or a page that is not the API', () async {
      expect(await api.pair(base, 'K7MP4X'), isA<PairingServerUnreachable>());

      answer = (request) async {
        request.response
          ..statusCode = 200
          ..write('<html>Wi-Fi login</html>');
        await request.response.close();
      };
      expect(await api.pair(base, 'K7MP4X'), isA<PairingServerUnreachable>());

      await server.close(force: true);
      expect(await api.pair(base, 'K7MP4X'), isA<PairingServerUnreachable>());
    });
  });

  group('check', () {
    test('sends the token and returns the court', () async {
      answer = (request) => json(request.response, 200, {
        'id': 'tablet-1',
        'deviceName': null,
        'pairedAt': '2026-11-21T08:00:00Z',
        'court': {...court, 'number': 4},
      });
      final session = sessionOnCourt(3).copyWithBase(base);

      final result = await api.check(session);

      expect(received.single.method, 'GET');
      expect(received.single.path, '/api/tablets/me');
      expect(received.single.auth, 'Bearer fst_secret');
      expect(result.status, TabletCheck.paired);
      expect(result.courtNumber, 4);
    });

    test('a revoked or unknown token', () async {
      for (final code in ['TABLET_REVOKED', 'UNAUTHORIZED']) {
        answer = (request) => json(request.response, 401, problem(401, code));

        final result = await api.check(sessionOnCourt(3).copyWithBase(base));

        expect(result.status, TabletCheck.revoked, reason: code);
      }
    });

    test('anything else says nothing about the pairing', () async {
      expect(
        (await api.check(sessionOnCourt(3).copyWithBase(base))).status,
        TabletCheck.unknown,
      );

      await server.close(force: true);
      expect(
        (await api.check(sessionOnCourt(3).copyWithBase(base))).status,
        TabletCheck.unknown,
      );
    });
  });
}
