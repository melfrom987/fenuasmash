import 'package:flutter/foundation.dart';

/// The pairing of this tablet with a court of a tournament, received from
/// `POST /tablets/pair` and kept until the tablet is revoked or unpaired.
@immutable
class TabletSession {
  const TabletSession({
    required this.apiBaseUri,
    required this.accessToken,
    required this.tabletId,
    required this.courtId,
    required this.courtNumber,
    required this.tournamentId,
    required this.pairedAt,
  });

  /// The API root the tablet was paired with, e.g. `http://10.0.0.5:8081/api`.
  final Uri apiBaseUri;

  /// Tablet token, sent as `Authorization: Bearer <accessToken>`. It has no
  /// expiry: it stops working once the organizer revokes the tablet.
  final String accessToken;

  final String tabletId;
  final String courtId;
  final int courtNumber;
  final String tournamentId;
  final DateTime pairedAt;

  TabletSession withCourtNumber(int number) {
    return TabletSession(
      apiBaseUri: apiBaseUri,
      accessToken: accessToken,
      tabletId: tabletId,
      courtId: courtId,
      courtNumber: number,
      tournamentId: tournamentId,
      pairedAt: pairedAt,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'apiBaseUri': apiBaseUri.toString(),
      'accessToken': accessToken,
      'tabletId': tabletId,
      'courtId': courtId,
      'courtNumber': courtNumber,
      'tournamentId': tournamentId,
      'pairedAt': pairedAt.toUtc().toIso8601String(),
    };
  }

  factory TabletSession.fromJson(Map<String, dynamic> json) {
    return TabletSession(
      apiBaseUri: Uri.parse(json['apiBaseUri'] as String),
      accessToken: json['accessToken'] as String,
      tabletId: json['tabletId'] as String,
      courtId: json['courtId'] as String,
      courtNumber: json['courtNumber'] as int,
      tournamentId: json['tournamentId'] as String,
      pairedAt: DateTime.parse(json['pairedAt'] as String),
    );
  }

  @override
  bool operator ==(Object other) =>
      other is TabletSession &&
      other.apiBaseUri == apiBaseUri &&
      other.accessToken == accessToken &&
      other.tabletId == tabletId &&
      other.courtId == courtId &&
      other.courtNumber == courtNumber &&
      other.tournamentId == tournamentId &&
      other.pairedAt == pairedAt;

  @override
  int get hashCode => Object.hash(
    apiBaseUri,
    accessToken,
    tabletId,
    courtId,
    courtNumber,
    tournamentId,
    pairedAt,
  );
}

/// Turns what the umpire typed, or the `API_URL` given at build time, into
/// the API root: `http://10.0.0.5:8081` becomes `http://10.0.0.5:8081/api`.
///
/// Returns null when the address is not an `http` or `https` URL with a host.
Uri? parseApiBaseUri(String input) {
  final trimmed = input.trim();
  if (trimmed.isEmpty) {
    return null;
  }
  final withScheme = trimmed.contains('://') ? trimmed : 'http://$trimmed';
  final uri = Uri.tryParse(withScheme);
  if (uri == null ||
      (uri.scheme != 'http' && uri.scheme != 'https') ||
      uri.host.isEmpty) {
    return null;
  }
  final segments = uri.pathSegments.where((s) => s.isNotEmpty).toList();
  if (segments.isEmpty || segments.last != 'api') {
    segments.add('api');
  }
  return uri.replace(pathSegments: segments, query: null, fragment: null);
}

/// The pairing code without spaces and dashes, in capitals, as the server
/// compares it: ` k7m-p4x ` becomes `K7MP4X`.
String normalizePairingCode(String input) {
  return input.replaceAll(RegExp(r'[\s-]'), '').toUpperCase();
}

/// Length of the codes created by the organizer.
const int pairingCodeLength = 6;
