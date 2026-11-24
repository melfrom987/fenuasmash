import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:flutter/foundation.dart';
import 'package:path_provider/path_provider.dart';

/// Score events waiting to be confirmed by the backend, stored on the device.
///
/// The queue is a separate file from the current match, so that it survives
/// app restarts, the end of a match and the start of the next one: an event
/// only leaves the queue once the backend has confirmed it.
///
/// Events are kept per match. For each match, the queue also remembers the
/// highest sequence the backend confirmed, so that an event already confirmed
/// is never queued again when the match page saves all its events.
class ScoreEventOutbox {
  ScoreEventOutbox({Future<Directory> Function()? directory})
    : _directory = directory ?? getApplicationDocumentsDirectory;

  static const String fileName = 'score_event_outbox.json';

  final Future<Directory> Function() _directory;

  /// Operations run one after the other, so that two saves never mix.
  Future<void> _lastOperation = Future.value();

  /// Adds the events of a match that are not queued or confirmed yet. Calling
  /// it again with the same events does nothing, so the match page can pass
  /// all its events after each point.
  Future<void> enqueue(String matchId, Iterable<ScoreEvent> events) {
    return _run((queues) {
      final queue = queues.putIfAbsent(matchId, _MatchQueue.new);
      final queuedIds = queue.events.map((event) => event.id).toSet();
      var changed = false;
      for (final event in events) {
        if (event.sequence <= queue.confirmedSequence ||
            queuedIds.contains(event.id)) {
          continue;
        }
        queue.events.add(event);
        queuedIds.add(event.id);
        changed = true;
      }
      queue.events.sort((a, b) => a.sequence.compareTo(b.sequence));
      return changed;
    });
  }

  /// Events of a match still waiting for confirmation, in sequence order.
  Future<List<ScoreEvent>> pending(String matchId) async {
    final queues = await _runRead();
    return List.unmodifiable(queues[matchId]?.events ?? const <ScoreEvent>[]);
  }

  /// Ids of the matches that still have events to send.
  Future<List<String>> matchesWithPendingEvents() async {
    final queues = await _runRead();
    return [
      for (final entry in queues.entries)
        if (entry.value.events.isNotEmpty) entry.key,
    ];
  }

  /// Number of events waiting, all matches together.
  Future<int> pendingCount() async {
    final queues = await _runRead();
    return queues.values.fold<int>(
      0,
      (total, queue) => total + queue.events.length,
    );
  }

  /// Removes the events the backend confirmed (applied or already received)
  /// and remembers up to which sequence the match is confirmed.
  Future<void> confirm(String matchId, Iterable<String> confirmedIds) {
    final ids = confirmedIds.toSet();
    return _run((queues) {
      final queue = queues[matchId];
      if (queue == null || ids.isEmpty) {
        return false;
      }
      final confirmed = queue.events.where((event) => ids.contains(event.id));
      if (confirmed.isEmpty) {
        return false;
      }
      for (final event in confirmed) {
        if (event.sequence > queue.confirmedSequence) {
          queue.confirmedSequence = event.sequence;
        }
      }
      queue.events.removeWhere((event) => ids.contains(event.id));
      return true;
    });
  }

  Future<File> _file() async {
    final directory = await _directory();
    return File('${directory.path}/$fileName');
  }

  /// Runs [change] on the stored queues and saves them when it returns true.
  Future<void> _run(bool Function(Map<String, _MatchQueue> queues) change) {
    final operation = _lastOperation.then((_) async {
      final file = await _file();
      final queues = await _read(file);
      if (change(queues)) {
        await _write(file, queues);
      }
    });
    _lastOperation = operation.catchError((Object _) {});
    return operation;
  }

  Future<Map<String, _MatchQueue>> _runRead() {
    final operation = _lastOperation.then((_) async => _read(await _file()));
    _lastOperation = operation.then<void>((_) {}).catchError((Object _) {});
    return operation;
  }

  Future<Map<String, _MatchQueue>> _read(File file) async {
    if (!await file.exists()) {
      return {};
    }
    final content = await file.readAsString();
    if (content.trim().isEmpty) {
      return {};
    }
    try {
      final json = Map<String, dynamic>.from(jsonDecode(content) as Map);
      final matches = Map<String, dynamic>.from(json['matches'] as Map);
      return matches.map(
        (matchId, queue) => MapEntry(
          matchId,
          _MatchQueue.fromJson(Map<String, dynamic>.from(queue as Map)),
        ),
      );
    } catch (error) {
      // Never drop unsent points silently: keep the unreadable file aside.
      final backup = File(
        '${file.path}.unreadable-${DateTime.now().millisecondsSinceEpoch}',
      );
      await file.rename(backup.path);
      debugPrint(
        'Score event queue unreadable, moved to ${backup.path}: $error',
      );
      return {};
    }
  }

  /// Writes to a temporary file first, then replaces the queue in one step,
  /// so that a crash while saving never leaves a half-written queue.
  Future<void> _write(File file, Map<String, _MatchQueue> queues) async {
    final temporary = File('${file.path}.tmp');
    final json = {
      'version': 1,
      'matches': queues.map(
        (matchId, queue) => MapEntry(matchId, queue.toJson()),
      ),
    };
    await temporary.writeAsString(jsonEncode(json), flush: true);
    await temporary.rename(file.path);
  }
}

class _MatchQueue {
  _MatchQueue({this.confirmedSequence = 0, List<ScoreEvent>? events})
    : events = events ?? [];

  /// Highest sequence the backend confirmed for this match.
  int confirmedSequence;

  final List<ScoreEvent> events;

  Map<String, dynamic> toJson() {
    return {
      'confirmedSequence': confirmedSequence,
      'events': events.map((event) => event.toJson()).toList(),
    };
  }

  factory _MatchQueue.fromJson(Map<String, dynamic> json) {
    return _MatchQueue(
      confirmedSequence: json['confirmedSequence'] as int? ?? 0,
      events: (json['events'] as List? ?? [])
          .map(
            (event) =>
                ScoreEvent.fromJson(Map<String, dynamic>.from(event as Map)),
          )
          .toList(),
    );
  }
}
