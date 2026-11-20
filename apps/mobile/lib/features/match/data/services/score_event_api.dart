import 'package:badminton_umpire/features/match/domain/models/score_event.dart';

/// Sends score events to the backend: `POST /matches/{matchId}/events`
/// (`sendScoreEvents` in `contracts/openapi.yaml`).
///
/// Implementations never throw for an expected answer: they return one of the
/// [ScoreEventSendResult] cases, so that the sync can decide what to do.
abstract interface class ScoreEventApi {
  /// Sends 1 to [maxBatchSize] events of one match, in sequence order.
  Future<ScoreEventSendResult> send(String matchId, List<ScoreEvent> events);
}

/// Most events accepted in one request by the backend.
const int maxBatchSize = 100;

/// What the backend answered to a batch of events.
sealed class ScoreEventSendResult {
  const ScoreEventSendResult();
}

/// 200: the events are applied, now or by an earlier request.
final class ScoreEventsConfirmed extends ScoreEventSendResult {
  const ScoreEventsConfirmed({
    required this.acceptedIds,
    required this.duplicateIds,
    required this.lastSequence,
  });

  /// Applied by this request.
  final List<String> acceptedIds;

  /// Already received before (for example when the previous answer was lost).
  final List<String> duplicateIds;

  /// Highest sequence the backend applied for the match.
  final int lastSequence;

  Iterable<String> get confirmedIds => [...acceptedIds, ...duplicateIds];
}

/// 409 `SEQUENCE_GAP`: the backend is missing events before this batch.
final class ScoreEventsSequenceGap extends ScoreEventSendResult {
  const ScoreEventsSequenceGap({required this.expectedSequence});

  /// The sequence the tablet has to send again from.
  final int expectedSequence;
}

/// The backend refused the batch and sending it again will not help, e.g.
/// `EVENT_ID_CONFLICT`, `SEQUENCE_ALREADY_USED`, `INVALID_SCORE_EVENT`,
/// `MATCH_NOT_IN_PROGRESS`, `TABLET_REVOKED`, an unknown match, or a tablet
/// that is not allowed on this match.
final class ScoreEventsRejected extends ScoreEventSendResult {
  const ScoreEventsRejected({required this.code});

  /// The `code` of the problem returned by the backend.
  final String code;
}

/// No answer (no network, timeout) or a server error (5xx): the events were
/// maybe applied, maybe not. Sending them again is safe, as the backend
/// ignores events it already received.
final class ScoreEventsUnreachable extends ScoreEventSendResult {
  const ScoreEventsUnreachable({this.reason});

  final String? reason;
}
