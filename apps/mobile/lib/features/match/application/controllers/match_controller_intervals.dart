part of 'match_controller.dart';

extension MatchControllerIntervals on MatchController {
  void startCurrentInterval() {
    if (!intervalIsReady) {
      return;
    }

    intervalTwentySecondsReached = false;

    late final Duration duration;

    if (phase == MatchPhase.midSetIntervalReady) {
      phase = MatchPhase.midSetIntervalRunning;

      duration = BadmintonRules.midSetInterval;
    } else if (phase == MatchPhase.betweenSetsIntervalReady) {
      phase = MatchPhase.betweenSetsIntervalRunning;

      duration = BadmintonRules.betweenSetsInterval;
    } else {
      return;
    }

    intervalRemaining = duration;

    intervalEndsAt = DateTime.now().add(duration);

    notifyMatchChanged();

    _startIntervalTimer();
  }

  void finishIntervalEarly() {
    if (!intervalIsRunning && !intervalIsReady) {
      return;
    }

    _intervalTimer?.cancel();
    _intervalTimer = null;

    intervalRemaining = Duration.zero;
    intervalEndsAt = null;

    intervalTwentySecondsReached = false;

    _completeCurrentInterval();
  }

  void _completeCurrentInterval() {
    _intervalTimer?.cancel();
    _intervalTimer = null;

    intervalRemaining = Duration.zero;
    intervalEndsAt = null;

    intervalTwentySecondsReached = false;

    if (isMidSetInterval) {
      phase = MatchPhase.playing;

      notifyMatchChanged();
      return;
    }

    if (isBetweenSetsInterval) {
      phase = MatchPhase.preparingNextSet;

      notifyMatchChanged();
    }
  }

  void _startIntervalTimer() {
    _intervalTimer?.cancel();

    _intervalTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      final end = intervalEndsAt;

      if (end == null) {
        return;
      }

      final remaining = end.difference(DateTime.now());

      if (remaining <= Duration.zero) {
        _completeCurrentInterval();
        return;
      }

      intervalRemaining = remaining;

      if (remaining.inSeconds <= BadmintonRules.intervalAnnouncementSeconds) {
        intervalTwentySecondsReached = true;
      }

      notifyMatchChanged();
    });
  }

  void resumeIntervalIfNeeded() {
    if (!intervalIsRunning) {
      return;
    }

    final end = intervalEndsAt;

    if (end == null) {
      _completeCurrentInterval();
      return;
    }

    final remaining = end.difference(DateTime.now());

    if (remaining <= Duration.zero) {
      _completeCurrentInterval();
      return;
    }

    intervalRemaining = remaining;

    if (remaining.inSeconds <= 20) {
      intervalTwentySecondsReached = true;
    }

    _startIntervalTimer();

    notifyMatchChanged();
  }

  // ---------------------------------------------------------
  // SET SUIVANT
  // ---------------------------------------------------------

  void startNextSet({
    required String selectedServerName,
    required String selectedReceiverName,
  }) {
    if (phase != MatchPhase.preparingNextSet) {
      return;
    }

    if (!nextSetServerCandidates.contains(selectedServerName)) {
      return;
    }

    if (!nextSetReceiverCandidates.contains(selectedReceiverName)) {
      return;
    }

    final nextServingTeamId = getTeamIdForPlayer(selectedServerName);

    if (nextServingTeamId == null) {
      return;
    }

    servingTeamId = nextServingTeamId;

    serverName = selectedServerName;
    receiverName = selectedReceiverName;

    _placeInitialServerAndReceiver();

    phase = MatchPhase.playing;

    notifyMatchChanged();
  }
}
