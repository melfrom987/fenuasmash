import 'dart:async';
import 'dart:io';

import 'package:badminton_umpire/features/match/data/services/http_score_event_api.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_api.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';

/// Sends score events to the server the tablet is paired with, with its
/// token, as read at the time of sending: pairing again with another address
/// takes effect at the next send.
///
/// While the tablet is not paired, nothing is sent and the events wait, as
/// when offline. A refused token makes the pairing check the tablet, which
/// ends the session if the organizer revoked it.
class PairedScoreEventApi implements ScoreEventApi {
  PairedScoreEventApi({
    required this.pairing,
    ScoreEventApi Function(TabletSession session)? apiFor,
  }) : _apiFor = apiFor ?? _httpApiFor;

  final TabletSessionController pairing;
  final ScoreEventApi Function(TabletSession session) _apiFor;

  /// Reason of [ScoreEventsUnreachable] while the tablet is not paired.
  static const String notPaired = 'not paired';

  static final HttpClient _client = HttpClient();

  static ScoreEventApi _httpApiFor(TabletSession session) {
    return HttpScoreEventApi(
      baseUri: session.apiBaseUri,
      token: () async => session.accessToken,
      client: _client,
    );
  }

  @override
  Future<ScoreEventSendResult> send(
    String matchId,
    List<ScoreEvent> events,
  ) async {
    final session = pairing.session;
    if (session == null) {
      return const ScoreEventsUnreachable(reason: notPaired);
    }
    final result = await _apiFor(session).send(matchId, events);
    if (result is ScoreEventsRejected && _isRefusedToken(result.code)) {
      unawaited(pairing.verify());
    }
    return result;
  }

  static bool _isRefusedToken(String code) =>
      code == 'TABLET_REVOKED' || code == 'HTTP_${HttpStatus.unauthorized}';
}
