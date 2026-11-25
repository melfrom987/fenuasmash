import 'dart:async';

import 'package:badminton_umpire/features/match/data/services/paired_score_event_api.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_outbox.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_sync.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';

/// The queue of score events not confirmed by the server, for the whole app.
/// One instance only, so that the match page and the sync never write the
/// queue file at the same time.
final scoreEventOutbox = ScoreEventOutbox();

/// Sends the queued score events to the server the tablet is paired with,
/// for the whole app: points left unsent by a previous match keep being sent
/// after its page is closed.
final scoreSync = ScoreEventSyncScheduler(
  ScoreEventSync(
    outbox: scoreEventOutbox,
    api: PairedScoreEventApi(pairing: tabletSessionController),
  ),
);

/// Sends what is waiting now, then again each time the tablet is paired (or
/// paired again), so that points scored before a restart or while unpaired
/// are not forgotten.
void startScoreSync({
  TabletSessionController? pairing,
  ScoreEventSyncScheduler? scheduler,
}) {
  final session = pairing ?? tabletSessionController;
  final sync = scheduler ?? scoreSync;
  var watched = session.session;
  session.addListener(() {
    final current = session.session;
    if (current != null && current != watched) {
      unawaited(sync.syncNow());
    }
    watched = current;
  });
  if (watched != null) {
    unawaited(sync.syncNow());
  }
}
