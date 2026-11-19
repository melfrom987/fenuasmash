import 'dart:convert';

import 'package:badminton_umpire/features/match/data/services/score_event_api.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';

/// In-memory backend following the rules of `sendScoreEvents` in
/// `contracts/openapi.yaml`, with switches to simulate network problems.
class FakeScoreServer implements ScoreEventApi {
  /// When false, every request fails as if there were no network.
  bool online = true;

  /// The next request is applied, but its answer is lost on the way back.
  bool loseNextAnswer = false;

  /// When set, the network cuts once this many more requests have reached
  /// the server, for example in the middle of a sync sent in several batches.
  int? requestsBeforeCut;

  /// The next request fails with a server error, without being applied.
  bool failNextWithServerError = false;

  /// Matches whose events are refused with this code.
  final Map<String, String> rejectedMatches = {};

  /// Number of requests received, answered or not.
  int requestCount = 0;

  /// Number of sending attempts, including those made while offline.
  int attemptCount = 0;

  /// Applied events, by match, in sequence order.
  final Map<String, List<ScoreEvent>> applied = {};

  @override
  Future<ScoreEventSendResult> send(
    String matchId,
    List<ScoreEvent> events,
  ) async {
    await Future<void>.delayed(Duration.zero);
    attemptCount++;
    if (requestsBeforeCut == 0) {
      online = false;
      requestsBeforeCut = null;
    }
    if (!online) {
      return const ScoreEventsUnreachable(reason: 'offline');
    }
    requestCount++;
    if (requestsBeforeCut != null) {
      requestsBeforeCut = requestsBeforeCut! - 1;
    }
    if (events.isEmpty || events.length > maxBatchSize) {
      return const ScoreEventsRejected(code: 'VALIDATION_FAILED');
    }
    if (failNextWithServerError) {
      failNextWithServerError = false;
      return const ScoreEventsUnreachable(reason: '500');
    }
    final rejection = rejectedMatches[matchId];
    if (rejection != null) {
      return ScoreEventsRejected(code: rejection);
    }

    final result = _apply(matchId, events);
    if (loseNextAnswer) {
      loseNextAnswer = false;
      return const ScoreEventsUnreachable(reason: 'answer lost');
    }
    return result;
  }

  /// Applies the whole batch or nothing.
  ScoreEventSendResult _apply(String matchId, List<ScoreEvent> events) {
    final known = {
      for (final event in applied[matchId] ?? <ScoreEvent>[]) event.id: event,
    };
    final sorted = [...events]
      ..sort((a, b) => a.sequence.compareTo(b.sequence));
    final accepted = <ScoreEvent>[];
    final duplicateIds = <String>[];
    var lastSequence = applied[matchId]?.length ?? 0;

    for (final event in sorted) {
      final previous = known[event.id];
      if (previous != null) {
        if (jsonEncode(previous.toJson()) != jsonEncode(event.toJson())) {
          return const ScoreEventsRejected(code: 'EVENT_ID_CONFLICT');
        }
        duplicateIds.add(event.id);
        continue;
      }
      if (event.sequence != lastSequence + 1) {
        return ScoreEventsSequenceGap(expectedSequence: lastSequence + 1);
      }
      accepted.add(event);
      lastSequence = event.sequence;
    }

    applied.putIfAbsent(matchId, () => []).addAll(accepted);
    return ScoreEventsConfirmed(
      acceptedIds: accepted.map((event) => event.id).toList(),
      duplicateIds: duplicateIds,
      lastSequence: lastSequence,
    );
  }

  /// Points of each side once the undos are applied, as the backend counts.
  ({int a, int b}) score(String matchId) {
    final points = <MatchSide>[];
    for (final event in applied[matchId] ?? <ScoreEvent>[]) {
      if (event.type == ScoreEventType.point) {
        points.add(event.side!);
      } else if (points.isNotEmpty) {
        points.removeLast();
      }
    }
    return (
      a: points.where((side) => side == MatchSide.a).length,
      b: points.where((side) => side == MatchSide.b).length,
    );
  }
}
