import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';

/// What the server answered to a pairing code.
sealed class PairingResult {
  const PairingResult();
}

final class Paired extends PairingResult {
  const Paired(this.session);

  final TabletSession session;
}

/// 400 `INVALID_PAIRING_CODE`: wrong, expired or already used code.
final class InvalidPairingCode extends PairingResult {
  const InvalidPairingCode();
}

/// 429 `TOO_MANY_PAIRING_ATTEMPTS`: too many wrong codes from this network.
final class TooManyPairingAttempts extends PairingResult {
  const TooManyPairingAttempts(this.retryAfter);

  final Duration retryAfter;
}

/// 409 `COURT_INACTIVE`: the organizer disabled the court.
final class PairingCourtInactive extends PairingResult {
  const PairingCourtInactive();
}

/// No answer, a timeout or a server error.
final class PairingServerUnreachable extends PairingResult {
  const PairingServerUnreachable();
}

/// Any other answer, with the `code` of the problem.
final class PairingFailed extends PairingResult {
  const PairingFailed(this.code);

  final String code;
}

/// Whether the saved pairing still works, from `GET /tablets/me`.
enum TabletCheck {
  /// The tablet is still paired.
  paired,

  /// 401: the tablet was revoked (`TABLET_REVOKED`), or its token is unknown.
  revoked,

  /// No answer, or an answer that says nothing about the pairing: the saved
  /// session is kept, as the tablet must work offline.
  unknown,
}

/// Result of `GET /tablets/me`, with the court number when it is known.
typedef TabletCheckResult = ({TabletCheck status, int? courtNumber});

/// The pairing routes of the FenuaSmash API.
abstract interface class TabletPairingApi {
  /// `POST /tablets/pair` with the code given by the organizer.
  Future<PairingResult> pair(Uri apiBaseUri, String code, {String? deviceName});

  /// `GET /tablets/me` with the token of the session.
  Future<TabletCheckResult> check(TabletSession session);
}

class HttpTabletPairingApi implements TabletPairingApi {
  HttpTabletPairingApi({
    HttpClient? client,
    this.timeout = const Duration(seconds: 10),
  }) : _client = client ?? HttpClient();

  final HttpClient _client;
  final Duration timeout;

  @override
  Future<PairingResult> pair(
    Uri apiBaseUri,
    String code, {
    String? deviceName,
  }) async {
    final ({int status, Map<String, dynamic>? json, HttpHeaders headers})
    response;
    try {
      response = await _send(
        'POST',
        _uri(apiBaseUri, ['tablets', 'pair']),
        body: {'code': code, 'deviceName': deviceName},
      ).timeout(timeout);
    } on TimeoutException {
      return const PairingServerUnreachable();
    } on IOException {
      return const PairingServerUnreachable();
    }

    final json = response.json;
    final problem = json?['code'];
    switch (response.status) {
      case HttpStatus.ok:
        try {
          final court = json!['court'] as Map<String, dynamic>;
          return Paired(
            TabletSession(
              apiBaseUri: apiBaseUri,
              accessToken: json['accessToken'] as String,
              tabletId: json['tabletId'] as String,
              courtId: court['id'] as String,
              courtNumber: court['number'] as int,
              tournamentId: court['tournamentId'] as String,
              pairedAt: DateTime.now().toUtc(),
            ),
          );
        } on Object {
          // Not the FenuaSmash API, e.g. a Wi-Fi login page
          return const PairingServerUnreachable();
        }
      case HttpStatus.badRequest
          when problem == 'INVALID_PAIRING_CODE' ||
              problem == 'VALIDATION_FAILED':
        return const InvalidPairingCode();
      case HttpStatus.tooManyRequests:
        final seconds = int.tryParse(
          response.headers.value(HttpHeaders.retryAfterHeader) ?? '',
        );
        return TooManyPairingAttempts(Duration(seconds: seconds ?? 900));
      case HttpStatus.conflict when problem == 'COURT_INACTIVE':
        return const PairingCourtInactive();
      case >= 500:
        return const PairingServerUnreachable();
      default:
        return PairingFailed(
          problem is String ? problem : 'HTTP_${response.status}',
        );
    }
  }

  @override
  Future<TabletCheckResult> check(TabletSession session) async {
    try {
      final response = await _send(
        'GET',
        _uri(session.apiBaseUri, ['tablets', 'me']),
        token: session.accessToken,
      ).timeout(timeout);
      if (response.status == HttpStatus.unauthorized) {
        return (status: TabletCheck.revoked, courtNumber: null);
      }
      if (response.status == HttpStatus.ok) {
        final court = response.json?['court'];
        final number = court is Map<String, dynamic> ? court['number'] : null;
        if (number is int) {
          return (status: TabletCheck.paired, courtNumber: number);
        }
      }
      return (status: TabletCheck.unknown, courtNumber: null);
    } on TimeoutException {
      return (status: TabletCheck.unknown, courtNumber: null);
    } on IOException {
      return (status: TabletCheck.unknown, courtNumber: null);
    }
  }

  void close() => _client.close(force: true);

  static Uri _uri(Uri apiBaseUri, List<String> path) {
    return apiBaseUri.replace(
      pathSegments: [
        ...apiBaseUri.pathSegments.where((segment) => segment.isNotEmpty),
        ...path,
      ],
    );
  }

  Future<({int status, Map<String, dynamic>? json, HttpHeaders headers})> _send(
    String method,
    Uri uri, {
    Object? body,
    String? token,
  }) async {
    final request = await _client.openUrl(method, uri);
    request.headers.set(HttpHeaders.acceptHeader, 'application/json');
    if (token != null) {
      request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
    }
    if (body != null) {
      request.headers.contentType = ContentType.json;
      request.write(jsonEncode(body));
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
    return (status: response.statusCode, json: json, headers: response.headers);
  }
}
