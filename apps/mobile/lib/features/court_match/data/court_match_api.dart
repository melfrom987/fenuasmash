import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:badminton_umpire/features/court_match/domain/court_match.dart';
import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';

/// What the server says about the court of the tablet.
sealed class CourtMatchResult {
  const CourtMatchResult();
}

/// 200: a match is called to the court, or played on it.
final class CourtMatchFound extends CourtMatchResult {
  const CourtMatchFound(this.match);

  final CourtMatch match;
}

/// 204: no match on the court.
final class CourtFree extends CourtMatchResult {
  const CourtFree();
}

/// 401: the tablet was revoked, or its token is unknown.
final class CourtMatchRevoked extends CourtMatchResult {
  const CourtMatchRevoked();
}

/// No answer, a timeout, a server error or an answer that is not the API:
/// the last known match is kept.
final class CourtMatchUnreachable extends CourtMatchResult {
  const CourtMatchUnreachable();
}

/// Reads the match on the court of the tablet.
abstract interface class CourtMatchApi {
  /// `GET /tablets/me/match`, then the names of its tournament and event.
  /// [known] is the last match read: its labels are reused when the tournament
  /// and the event did not change, so that polling costs one request.
  Future<CourtMatchResult> fetch(TabletSession session, {CourtMatch? known});
}

class HttpCourtMatchApi implements CourtMatchApi {
  HttpCourtMatchApi({
    HttpClient? client,
    this.timeout = const Duration(seconds: 10),
  }) : _client = client ?? HttpClient();

  final HttpClient _client;
  final Duration timeout;

  @override
  Future<CourtMatchResult> fetch(
    TabletSession session, {
    CourtMatch? known,
  }) async {
    final CourtMatch match;
    try {
      final response = await _get(session.apiBaseUri, [
        'tablets',
        'me',
        'match',
      ], token: session.accessToken).timeout(timeout);
      switch (response.status) {
        case HttpStatus.noContent:
          return const CourtFree();
        case HttpStatus.unauthorized:
          return const CourtMatchRevoked();
        case HttpStatus.ok when response.json != null:
          try {
            match = CourtMatch.fromJson(response.json!);
          } on FormatException {
            return const CourtMatchUnreachable();
          }
        default:
          return const CourtMatchUnreachable();
      }
    } on TimeoutException {
      return const CourtMatchUnreachable();
    } on IOException {
      return const CourtMatchUnreachable();
    }

    if (known != null &&
        known.tournamentId == match.tournamentId &&
        known.eventId == match.eventId &&
        known.eventName != null) {
      return CourtMatchFound(
        match.withLabels(
          tournamentName: known.tournamentName,
          eventName: known.eventName,
          discipline: known.discipline,
          categoryLabel: known.categoryLabel,
        ),
      );
    }

    // The labels are a help for the umpire: the match is shown without them
    // when they cannot be read. Both routes are public.
    final (tournament, event) = await (
      _readOrNull(session.apiBaseUri, ['tournaments', match.tournamentId]),
      _readOrNull(session.apiBaseUri, ['events', match.eventId]),
    ).wait;
    return CourtMatchFound(
      match.withLabels(
        tournamentName: _string(tournament?['name']),
        eventName: _string(event?['name']),
        discipline: _string(event?['discipline']),
        categoryLabel: _string(event?['categoryLabel']),
      ),
    );
  }

  void close() => _client.close(force: true);

  static String? _string(Object? value) => value is String ? value : null;

  Future<Map<String, dynamic>?> _readOrNull(
    Uri apiBaseUri,
    List<String> path,
  ) async {
    try {
      final response = await _get(apiBaseUri, path).timeout(timeout);
      return response.status == HttpStatus.ok ? response.json : null;
    } on TimeoutException {
      return null;
    } on IOException {
      return null;
    }
  }

  Future<({int status, Map<String, dynamic>? json})> _get(
    Uri apiBaseUri,
    List<String> path, {
    String? token,
  }) async {
    final uri = apiBaseUri.replace(
      pathSegments: [
        ...apiBaseUri.pathSegments.where((segment) => segment.isNotEmpty),
        ...path,
      ],
    );
    final request = await _client.getUrl(uri);
    request.headers.set(HttpHeaders.acceptHeader, 'application/json');
    if (token != null) {
      request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
    }
    final response = await request.close();
    final text = await response.transform(utf8.decoder).join();
    Map<String, dynamic>? json;
    try {
      final decoded = jsonDecode(text);
      json = decoded is Map<String, dynamic> ? decoded : null;
    } on FormatException {
      json = null;
    }
    return (status: response.statusCode, json: json);
  }
}
