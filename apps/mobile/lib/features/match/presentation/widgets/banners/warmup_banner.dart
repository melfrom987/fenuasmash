import 'package:badminton_umpire/features/match/presentation/widgets/banners/countdown_banner.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

class WarmupBanner extends StatelessWidget {
  final Duration remaining;
  final bool started;

  final VoidCallback onStart;
  final VoidCallback onSkip;

  const WarmupBanner({
    super.key,
    required this.remaining,
    required this.started,
    required this.onStart,
    required this.onSkip,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    final showAnnouncement =
        started && remaining.inSeconds <= 20 && remaining.inSeconds > 0;

    return CountdownBanner(
      icon: Symbols.badminton,
      title: l10n.warmup,
      remaining: remaining,
      started: started,
      startLabel: l10n.startWarmup,
      skipLabel: l10n.skipWarmup,
      announcement: showAnnouncement ? l10n.readyToPlayAnnouncement : null,
      onStart: onStart,
      onSkip: onSkip,
    );
  }
}
