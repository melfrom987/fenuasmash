import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:badminton_umpire/features/match/data/services/score_event_api.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';

/// Sends score events to the FenuaSmash backend over HTTP:
/// `POST {baseUri}/matches/{matchId}/events`, with the tablet token.
///
/// Every answer becomes a [ScoreEventSendResult]:
/// - 200: [ScoreEventsConfirmed];
/// - 409 `SEQUENCE_GAP`: [ScoreEventsSequenceGap];
/// - no answer, a timeout, 408, 429 or 5xx: [ScoreEventsUnreachable], as
///   sending again later may work;
/// - any other answer (400, 401 `TABLET_REVOKED`, 403, 404, other 409):
///   [ScoreEventsRejected] with the `code` of the problem.
class HttpScoreEventApi implements ScoreEventApi {
  HttpScoreEventApi({
    required this.baseUri,
    required this.token,
    HttpClient? client,
    this.timeout = const Duration(seconds: 10),
  }) : _client = client ?? HttpClient();

  /// The API root, e.g. `http://192.168.1.10:8081/api`.
  final Uri baseUri;

  /// The tablet token received at pairing, or null when the tablet is not
  /// paired: nothing is sent then.
  final Future<String?> Function() token;

  /// Longest wait for a whole request and its answer.
  final Duration timeout;

  final HttpClient _client;

  /// Code of [ScoreEventsRejected] when the tablet has no token.
  static const String notPaired = 'NOT_PAIRED';

  @override
  Future<ScoreEventSendResult> send(
    String matchId,
    List<ScoreEvent> events,
  ) async {
    final accessToken = await token();
    if (accessToken == null || accessToken.isEmpty) {
      return const ScoreEventsRejected(code: notPaired);
    }
    try {
      return await _post(matchId, events, accessToken).timeout(timeout);
    } on TimeoutException {
      return const ScoreEventsUnreachable(reason: 'timeout');
    } on IOException catch (error) {
      return ScoreEventsUnreachable(reason: error.toString());
    }
  }

  /// Stops the connections kept open for the next requests.
  void close() => _client.close(force: true);

  Future<ScoreEventSendResult> _post(
    String matchId,
    List<ScoreEvent> events,
    String accessToken,
  ) async {
    final request = await _client.postUrl(_eventsUri(matchId));
    request.headers
      ..set(HttpHeaders.authorizationHeader, 'Bearer $accessToken')
      ..set(HttpHeaders.acceptHeader, 'application/json')
      ..contentType = ContentType.json;
    request.write(
      jsonEncode({'events': events.map((event) => event.toJson()).toList()}),
    );
    final response = await request.close();
    final body = await response.transform(utf8.decoder).join();
    return _resultOf(response.statusCode, body);
  }

  Uri _eventsUri(String matchId) {
    final segments = [
      ...baseUri.pathSegments.where((segment) => segment.isNotEmpty),
      'matches',
      matchId,
      'events',
    ];
    return baseUri.replace(pathSegments: segments);
  }

  static ScoreEventSendResult _resultOf(int status, String body) {
    final json = _decode(body);
    if (status == HttpStatus.ok) {
      try {
        return ScoreEventsConfirmed(
          acceptedIds: _ids(json?['acceptedIds']),
          duplicateIds: _ids(json?['duplicateIds']),
          lastSequence: json!['lastSequence'] as int,
        );
      } on Object {
        // A 200 that is not the API (e.g. a captive portal page): the events
        // may not have arrived, sending them again is safe.
        return const ScoreEventsUnreachable(reason: 'unexpected answer');
      }
    }
    if (_isTemporary(status)) {
      return ScoreEventsUnreachable(reason: 'HTTP $status');
    }
    final code = json?['code'];
    final expected = json?['expectedSequence'];
    if (status == HttpStatus.conflict &&
        code == 'SEQUENCE_GAP' &&
        expected is int) {
      return ScoreEventsSequenceGap(expectedSequence: expected);
    }
    return ScoreEventsRejected(code: code is String ? code : 'HTTP_$status');
  }

  static bool _isTemporary(int status) =>
      status >= 500 ||
      status == HttpStatus.requestTimeout ||
      status == HttpStatus.tooManyRequests;

  static Map<String, dynamic>? _decode(String body) {
    try {
      final json = jsonDecode(body);
      return json is Map<String, dynamic> ? json : null;
    } on FormatException {
      return null;
    }
  }

  static List<String> _ids(Object? value) =>
      (value as List<dynamic>).cast<String>();
}
