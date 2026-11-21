import 'dart:convert';
import 'dart:io';

import 'package:badminton_umpire/features/court_match/data/court_match_api.dart';
import 'package:badminton_umpire/features/court_match/domain/court_match.dart';
import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';
import 'package:flutter_test/flutter_test.dart';

import '../pairing/helpers.dart';
import 'helpers.dart';

/// The answers below are the ones of the backend (`TabletController`, #144).
void main() {
  late HttpServer server;
  late HttpCourtMatchApi api;
  late TabletSession session;
  late List<HttpRequest> received;
  late Map<String, Future<void> Function(HttpResponse response)> routes;

  Future<void> json(HttpResponse response, int status, Object body) async {
    response
      ..statusCode = status
      ..headers.contentType = ContentType.json
      ..write(jsonEncode(body));
    await response.close();
  }

  Future<void> status(HttpResponse response, int code) async {
    response.statusCode = code;
    await response.close();
  }

  setUp(() async {
    received = [];
    routes = {
      '/api/tablets/me/match': (r) => json(r, 200, matchJson()),
      '/api/tournaments/$tournamentId': (r) =>
          json(r, 200, {'id': tournamentId, 'name': 'Open de Papeete'}),
      '/api/events/$eventId': (r) => json(r, 200, {
        'id': eventId,
        'name': 'Double mixte Senior',
        'discipline': 'XD',
        'categoryLabel': 'Senior',
      }),
    };
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    server.listen((request) async {
      received.add(request);
      final route = routes[request.uri.path];
      await (route == null
          ? status(request.response, 404)
          : route(request.response));
    });
    session = sessionOnCourt(
      3,
    ).copyWithBase(Uri.parse('http://127.0.0.1:${server.port}/api'));
    api = HttpCourtMatchApi(timeout: const Duration(seconds: 2));
  });

  tearDown(() async {
    api.close();
    await server.close(force: true);
  });

  test('reads the match of the court with its labels', () async {
    final result = await api.fetch(session);

    final match = (result as CourtMatchFound).match;
    expect(match.id, matchId);
    expect(match.status, CourtMatchStatus.called);
    expect(match.tournamentName, 'Open de Papeete');
    expect(match.eventName, 'Double mixte Senior');
    expect(match.discipline, 'XD');
    expect(match.categoryLabel, 'Senior');

    final court = received.firstWhere(
      (request) => request.uri.path == '/api/tablets/me/match',
    );
    expect(court.method, 'GET');
    expect(court.headers.value('authorization'), 'Bearer fst_secret');
    // The tournament and the event are public: the token is not sent there
    for (final request in received.where((r) => r != court)) {
      expect(request.headers.value('authorization'), isNull);
    }
  });

  test('labels already known are not read again', () async {
    final known = (await api.fetch(session) as CourtMatchFound).match;
    received.clear();
    routes['/api/tablets/me/match'] = (r) =>
        json(r, 200, matchJson(status: 'WARMUP', stateVersion: 4));

    final match =
        (await api.fetch(session, known: known) as CourtMatchFound).match;

    expect(received.map((r) => r.uri.path), ['/api/tablets/me/match']);
    expect(match.status, CourtMatchStatus.warmup);
    expect(match.eventName, 'Double mixte Senior');
    expect(match.tournamentName, 'Open de Papeete');
  });

  test('another event is read again', () async {
    final known = calledMatch().withLabels(eventName: 'Simple hommes');
    final other = CourtMatch(
      id: known.id,
      tournamentId: known.tournamentId,
      eventId: 'other-event',
      status: known.status,
      stateVersion: known.stateVersion,
      eventName: 'Simple hommes',
    );

    final match =
        (await api.fetch(session, known: other) as CourtMatchFound).match;

    expect(match.eventName, 'Double mixte Senior');
  });

  test('the match is shown without labels when they cannot be read', () async {
    routes.remove('/api/events/$eventId');
    routes['/api/tournaments/$tournamentId'] = (r) => status(r, 500);

    final match = (await api.fetch(session) as CourtMatchFound).match;

    expect(match.id, matchId);
    expect(match.eventName, isNull);
    expect(match.tournamentName, isNull);
    expect(match.category, isNull);
  });

  test('204 means the court is free', () async {
    routes['/api/tablets/me/match'] = (r) => status(r, 204);

    expect(await api.fetch(session), isA<CourtFree>());
    expect(received, hasLength(1));
  });

  test('401 means the tablet was revoked', () async {
    routes['/api/tablets/me/match'] = (r) =>
        json(r, 401, {'status': 401, 'code': 'TABLET_REVOKED'});

    expect(await api.fetch(session), isA<CourtMatchRevoked>());
  });

  test('other answers keep the last known state', () async {
    final answers = <Future<void> Function(HttpResponse)>[
      (r) => status(r, 500),
      (r) => status(r, 503),
      (r) => status(r, 403),
      (r) => json(r, 200, {'title': 'not a match'}),
      (r) async {
        r
          ..statusCode = 200
          ..headers.contentType = ContentType.html
          ..write('<html>Wi-Fi login</html>');
        await r.close();
      },
    ];
    for (final answer in answers) {
      routes['/api/tablets/me/match'] = answer;

      expect(await api.fetch(session), isA<CourtMatchUnreachable>());
    }
  });

  test('a server that takes too long is unreachable', () async {
    routes['/api/tablets/me/match'] = (r) async {
      await Future<void>.delayed(const Duration(seconds: 3));
      await json(r, 200, matchJson());
    };

    expect(await api.fetch(session), isA<CourtMatchUnreachable>());
  });

  test('no server is unreachable', () async {
    await server.close(force: true);

    expect(await api.fetch(session), isA<CourtMatchUnreachable>());
  });
}
