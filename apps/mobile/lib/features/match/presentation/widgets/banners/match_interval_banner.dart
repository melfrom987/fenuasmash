import 'package:badminton_umpire/core/settings/settings.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/banners/countdown_banner.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class MatchIntervalBanner extends StatelessWidget {
  final MatchPhase phase;
  final Duration remaining;

  final String courtNumber;

  final VoidCallback onStart;
  final VoidCallback onSkip;

  const MatchIntervalBanner({
    super.key,
    required this.phase,
    required this.remaining,
    required this.courtNumber,
    required this.onStart,
    required this.onSkip,
  });

  bool get _isRunning =>
      phase == MatchPhase.midSetIntervalRunning ||
      phase == MatchPhase.betweenSetsIntervalRunning;

  bool get _isBetweenSets =>
      phase == MatchPhase.betweenSetsIntervalReady ||
      phase == MatchPhase.betweenSetsIntervalRunning;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    final showTwentySeconds =
        appSettingsController.showUmpireAnnouncements &&
        _isRunning &&
        remaining.inSeconds <= 20 &&
        remaining.inSeconds > 0;

    return CountdownBanner(
      icon: Icons.timer_outlined,
      title: _isBetweenSets ? l10n.endOfSet : l10n.midSetInterval,
      remaining: remaining,
      started: _isRunning,
      startLabel: l10n.start,
      skipLabel: _isBetweenSets ? l10n.finishInterval : l10n.resumeMatch,
      announcement: showTwentySeconds
          ? l10n.twentySecondsAnnouncement(courtNumber)
          : null,
      onStart: onStart,
      onSkip: onSkip,
    );
  }
}
