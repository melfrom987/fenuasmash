import 'dart:async';

import 'package:badminton_umpire/features/match/data/services/score_event_api.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_outbox.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:flutter/foundation.dart';

/// Why the events of a match cannot be sent until someone looks at them.
@immutable
class ScoreSyncProblem {
  const ScoreSyncProblem.rejected(this.code) : expectedSequence = null;

  const ScoreSyncProblem.sequenceGap(int this.expectedSequence)
    : code = 'SEQUENCE_GAP';

  /// The `code` returned by the backend.
  final String code;

  /// With a sequence gap, the sequence the backend expects.
  final int? expectedSequence;

  @override
  bool operator ==(Object other) =>
      other is ScoreSyncProblem &&
      other.code == code &&
      other.expectedSequence == expectedSequence;

  @override
  int get hashCode => Object.hash(code, expectedSequence);

  @override
  String toString() => 'ScoreSyncProblem($code, $expectedSequence)';
}

/// Overall state of the score events, for the sync indicator.
enum ScoreSyncStatus {
  /// Every event is confirmed by the backend.
  upToDate,

  /// Events were queued after the last sync and are about to be sent.
  pending,

  /// The backend could not be reached: events wait and are sent again later.
  offline,

  /// The backend refused the events of a match: they are kept, but sending
  /// them again will not help.
  blocked,
}

/// Result of one sync.
@immutable
class ScoreSyncReport {
  const ScoreSyncReport({
    required this.confirmedCount,
    required this.pendingCount,
    required this.reachable,
    this.problems = const {},
  });

  /// Events confirmed by the backend during this sync.
  final int confirmedCount;

  /// Events still waiting, all matches together.
  final int pendingCount;

  /// False when the backend could not be reached.
  final bool reachable;

  /// Matches whose events the backend refused, by match id.
  final Map<String, ScoreSyncProblem> problems;

  ScoreSyncStatus get status {
    if (!reachable) {
      return ScoreSyncStatus.offline;
    }
    if (problems.isNotEmpty) {
      return ScoreSyncStatus.blocked;
    }
    return pendingCount == 0
        ? ScoreSyncStatus.upToDate
        : ScoreSyncStatus.pending;
  }
}

/// Sends the queued score events to the backend, match by match, in sequence
/// order and in batches, and removes from the queue every event the backend
/// confirms.
///
/// An event leaves the queue only once confirmed, and the backend ignores an
/// event it already received: a lost answer or a crash during a sync never
/// loses a point and never counts one twice.
class ScoreEventSync {
  ScoreEventSync({required this.outbox, required this.api});

  final ScoreEventOutbox outbox;
  final ScoreEventApi api;

  Future<ScoreSyncReport>? _running;
  bool _runAgain = false;

  /// Sends everything that is waiting. A call during a sync does not start a
  /// second one: the current sync runs once more at the end, to also send the
  /// events queued meanwhile.
  Future<ScoreSyncReport> syncNow() {
    final running = _running;
    if (running != null) {
      _runAgain = true;
      return running;
    }
    final sync = _syncUntilSettled().whenComplete(() => _running = null);
    _running = sync;
    return sync;
  }

  Future<ScoreSyncReport> _syncUntilSettled() async {
    var confirmedCount = 0;
    ScoreSyncReport report;
    do {
      _runAgain = false;
      report = await _syncOnce();
      confirmedCount += report.confirmedCount;
    } while (_runAgain && report.reachable);
    return ScoreSyncReport(
      confirmedCount: confirmedCount,
      pendingCount: report.pendingCount,
      reachable: report.reachable,
      problems: report.problems,
    );
  }

  Future<ScoreSyncReport> _syncOnce() async {
    var confirmedCount = 0;
    final problems = <String, ScoreSyncProblem>{};

    for (final matchId in await outbox.matchesWithPendingEvents()) {
      while (true) {
        final pending = await outbox.pending(matchId);
        if (pending.isEmpty) {
          break;
        }
        final batch = pending.take(maxBatchSize).toList();
        final result = await _send(matchId, batch);

        if (result is ScoreEventsUnreachable) {
          // Nothing more can be sent now: the scheduler tries again later.
          return ScoreSyncReport(
            confirmedCount: confirmedCount,
            pendingCount: await outbox.pendingCount(),
            reachable: false,
            problems: problems,
          );
        }
        if (result is ScoreEventsSequenceGap) {
          problems[matchId] = ScoreSyncProblem.sequenceGap(
            result.expectedSequence,
          );
          break;
        }
        if (result is ScoreEventsRejected) {
          problems[matchId] = ScoreSyncProblem.rejected(result.code);
          break;
        }
        final confirmed = result as ScoreEventsConfirmed;
        final batchIds = batch.map((event) => event.id).toSet();
        final confirmedIds = confirmed.confirmedIds.toSet().intersection(
          batchIds,
        );
        if (confirmedIds.isEmpty) {
          // Never loop on an answer that confirms nothing.
          problems[matchId] = const ScoreSyncProblem.rejected(
            'NOTHING_CONFIRMED',
          );
          break;
        }
        await outbox.confirm(matchId, confirmedIds);
        confirmedCount += confirmedIds.length;
      }
    }

    return ScoreSyncReport(
      confirmedCount: confirmedCount,
      pendingCount: await outbox.pendingCount(),
      reachable: true,
      problems: problems,
    );
  }

  /// An unexpected error while sending counts as unreachable: the events stay
  /// in the queue and are sent again later.
  Future<ScoreEventSendResult> _send(
    String matchId,
    List<ScoreEvent> batch,
  ) async {
    try {
      return await api.send(matchId, batch);
    } catch (error) {
      debugPrint('Could not send score events: $error');
      return ScoreEventsUnreachable(reason: error.toString());
    }
  }
}

/// Runs the sync again after a failure, waiting a little longer each time,
/// and publishes the last report for the sync indicator.
class ScoreEventSyncScheduler {
  ScoreEventSyncScheduler(this._sync, {List<Duration>? retryDelays})
    : _retryDelays = retryDelays ?? defaultRetryDelays;

  /// Waits between two attempts while the backend cannot be reached.
  static const List<Duration> defaultRetryDelays = [
    Duration(seconds: 2),
    Duration(seconds: 5),
    Duration(seconds: 15),
    Duration(seconds: 30),
    Duration(seconds: 60),
  ];

  final ScoreEventSync _sync;
  final List<Duration> _retryDelays;

  /// Last sync result, or null before the first sync.
  final ValueNotifier<ScoreSyncReport?> lastReport = ValueNotifier(null);

  Timer? _retryTimer;
  int _failures = 0;
  bool _disposed = false;

  /// Delay before the next attempt, or null when none is planned.
  Duration? get nextRetryDelay =>
      _retryTimer == null ? null : _retryDelays[_failures - 1];

  /// Sends now, for example after a new point or when the network returns.
  Future<ScoreSyncReport> syncNow() async {
    _retryTimer?.cancel();
    _retryTimer = null;
    final report = await _sync.syncNow();
    if (_disposed) {
      return report;
    }
    lastReport.value = report;
    if (report.reachable) {
      _failures = 0;
    } else {
      _failures = (_failures + 1).clamp(1, _retryDelays.length);
      _retryTimer = Timer(_retryDelays[_failures - 1], () {
        unawaited(syncNow());
      });
    }
    return report;
  }

  void dispose() {
    _disposed = true;
    _retryTimer?.cancel();
    lastReport.dispose();
  }
}
