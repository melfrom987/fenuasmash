import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/court_match/application/court_match_controller.dart';
import 'package:badminton_umpire/features/court_match/domain/court_match.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

/// Home screen card: the match the organizer called to the court of the
/// tablet. It updates by itself, and only while the app is in the foreground.
/// Nothing is shown when the tablet is not paired.
class CourtMatchCard extends StatefulWidget {
  const CourtMatchCard({
    super.key,
    required this.controller,
    required this.onOpen,
    this.isResumable,
  });

  final CourtMatchController controller;

  /// Opens the match: setup screen, or the match already started on the
  /// tablet.
  final ValueChanged<CourtMatch> onOpen;

  /// Whether the tablet already started this match.
  final bool Function(CourtMatch match)? isResumable;

  @override
  State<CourtMatchCard> createState() => _CourtMatchCardState();
}

class _CourtMatchCardState extends State<CourtMatchCard> {
  late final AppLifecycleListener _lifecycle;

  @override
  void initState() {
    super.initState();
    widget.controller.start();
    _lifecycle = AppLifecycleListener(
      onShow: widget.controller.start,
      onHide: widget.controller.stop,
    );
  }

  @override
  void dispose() {
    _lifecycle.dispose();
    widget.controller.stop();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: widget.controller,
      builder: (context, _) {
        final controller = widget.controller;
        final l10n = AppLocalizations.of(context)!;
        final responsive = Responsive.of(context);
        final scheme = Theme.of(context).colorScheme;
        final match = controller.match;

        final (icon, color, title, lines) = switch (controller.state) {
          CourtMatchState.idle => (null, null, '', const <String>[]),
          CourtMatchState.loading => (
            Icons.sports_tennis,
            Colors.grey.shade600,
            l10n.courtMatchLoading,
            const <String>[],
          ),
          CourtMatchState.free => (
            Icons.hourglass_empty,
            Colors.grey.shade700,
            l10n.courtMatchFreeTitle,
            [l10n.courtMatchFreeDescription],
          ),
          CourtMatchState.match => (
            Icons.campaign_outlined,
            scheme.primary,
            switch (match!.status) {
              CourtMatchStatus.called => l10n.courtMatchCalled,
              CourtMatchStatus.warmup => l10n.courtMatchWarmup,
              CourtMatchStatus.inProgress => l10n.courtMatchInProgress,
            },
            [
              _describe(match, l10n),
              (widget.isResumable?.call(match) ?? false)
                  ? l10n.courtMatchResumeHint
                  : l10n.courtMatchOpenHint,
            ],
          ),
        };
        if (icon == null || color == null) {
          return const SizedBox.shrink();
        }
        final offline = controller.offline
            ? _offlineText(context, l10n, controller.updatedAt)
            : null;

        return Padding(
          padding: EdgeInsets.only(bottom: responsive.spaceM),
          child: Material(
            color: color.withValues(alpha: 0.06),
            borderRadius: BorderRadius.circular(responsive.radiusL),
            child: InkWell(
              key: const Key('courtMatchCard'),
              onTap: match == null ? null : () => widget.onOpen(match),
              borderRadius: BorderRadius.circular(responsive.radiusL),
              child: Container(
                padding: EdgeInsets.all(responsive.paddingL),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(responsive.radiusL),
                  border: Border.all(color: color.withValues(alpha: 0.35)),
                ),
                child: Row(
                  children: [
                    Icon(icon, color: color, size: responsive.iconL),
                    SizedBox(width: responsive.spaceM),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            title,
                            style: TextStyle(
                              fontSize: responsive.titleS,
                              fontWeight: FontWeight.w900,
                              color: color,
                            ),
                          ),
                          for (final line in lines) ...[
                            SizedBox(height: responsive.spaceXXS),
                            Text(
                              line,
                              style: TextStyle(
                                fontSize: responsive.textS,
                                color: Colors.grey.shade700,
                              ),
                            ),
                          ],
                          if (offline != null) ...[
                            SizedBox(height: responsive.spaceXXS),
                            Text(
                              offline,
                              key: const Key('courtMatchOffline'),
                              style: TextStyle(
                                fontSize: responsive.textS,
                                color: scheme.error,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                    if (match != null) Icon(Icons.chevron_right, color: color),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  static String _describe(CourtMatch match, AppLocalizations l10n) {
    return [
      match.eventName ?? l10n.courtMatchUnknownEvent,
      if (match.round != null) l10n.courtMatchRound(match.round!),
      if (match.games.isNotEmpty)
        match.games.map((game) => '${game.a}-${game.b}').join(' '),
    ].join(' · ');
  }

  static String _offlineText(
    BuildContext context,
    AppLocalizations l10n,
    DateTime? updatedAt,
  ) {
    if (updatedAt == null) {
      return l10n.courtMatchOfflineNever;
    }
    final time = MaterialLocalizations.of(
      context,
    ).formatTimeOfDay(TimeOfDay.fromDateTime(updatedAt));
    return l10n.courtMatchOffline(time);
  }
}
