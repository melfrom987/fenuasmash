import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/core/settings/settings.dart';
import 'package:badminton_umpire/core/utils/formatters.dart';
import 'package:badminton_umpire/core/widgets/adaptive_text.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

class MatchFinishedOverlay extends StatelessWidget {
  final String winnerName;

  final int teamASets;
  final int teamBSets;

  final List<List<int>> previousSets;

  final List<String> teamAPlayers;
  final List<String> teamBPlayers;

  final Duration matchDuration;

  final int shuttleCount;

  final String umpireName;
  final String? serviceJudgeName;

  final DateTime? matchStartedAt;
  final DateTime? matchFinishedAt;

  final VoidCallback onBack;
  final VoidCallback onNewMatch;

  final bool canUndo;
  final VoidCallback onUndo;

  const MatchFinishedOverlay({
    super.key,
    required this.winnerName,
    required this.teamAPlayers,
    required this.teamBPlayers,
    required this.teamASets,
    required this.teamBSets,
    required this.previousSets,
    required this.matchDuration,
    required this.shuttleCount,
    required this.umpireName,
    required this.serviceJudgeName,
    required this.matchStartedAt,
    required this.matchFinishedAt,
    required this.onBack,
    required this.onNewMatch,
    required this.canUndo,
    required this.onUndo,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;
    final colorScheme = Theme.of(context).colorScheme;
    final showUmpireAnnouncements =
        appSettingsController.showUmpireAnnouncements;

    final teamAWon = teamASets > teamBSets;

    final matchScores = previousSets
        .map((set) {
          final teamAScore = set[0];
          final teamBScore = set[1];

          if (teamAWon) {
            return '$teamAScore–$teamBScore';
          }

          return '$teamBScore–$teamAScore';
        })
        .join(', ');

    return Positioned.fill(
      child: ColoredBox(
        color: Colors.black.withValues(alpha: 0.75),
        child: SafeArea(
          child: Padding(
            padding: EdgeInsets.all(responsive.spaceS),
            child: Container(
              width: double.infinity,
              height: double.infinity,
              padding: EdgeInsets.symmetric(
                horizontal: responsive.paddingM,
                vertical: responsive.paddingS,
              ),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(responsive.radiusM),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.20),
                    blurRadius: 20,
                    offset: const Offset(0, 6),
                  ),
                ],
              ),
              child: Column(
                children: [
                  _Header(
                    responsive: responsive,
                    colorScheme: colorScheme,
                    title: l10n.matchFinished,
                  ),

                  SizedBox(height: responsive.spaceS),

                  Expanded(
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // =====================================================
                        // GAUCHE : 1/3
                        // =====================================================
                        Expanded(
                          flex: 4,
                          child: _ResultCard(
                            teamAPlayers: teamAPlayers,
                            teamBPlayers: teamBPlayers,
                            teamASets: teamASets,
                            teamBSets: teamBSets,
                            previousSets: previousSets,
                          ),
                        ),

                        SizedBox(width: responsive.spaceS),

                        // =====================================================
                        // DROITE : 2/3
                        // =====================================================
                        Expanded(
                          flex: 5,
                          child: Column(
                            children: [
                              if (showUmpireAnnouncements) ...[
                                Expanded(
                                  flex: 3,
                                  child: _AnnouncementCard(
                                    announcement: l10n.matchAnnouncement(
                                      winnerName,
                                      matchScores,
                                    ),
                                  ),
                                ),
                                SizedBox(height: responsive.spaceXXS),
                              ],
                              Expanded(
                                flex: showUmpireAnnouncements ? 4 : 1,
                                child: _MatchInformationCard(
                                  matchDuration: matchDuration,
                                  shuttleCount: shuttleCount,
                                  umpireName: umpireName,
                                  serviceJudgeName: serviceJudgeName,
                                  matchStartedAt: matchStartedAt,
                                  matchFinishedAt: matchFinishedAt,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),

                  SizedBox(height: responsive.spaceS),

                  _Actions(
                    canUndo: canUndo,
                    onUndo: onUndo,
                    onBack: onBack,
                    onNewMatch: onNewMatch,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

// =============================================================================
// HEADER
// =============================================================================

class _Header extends StatelessWidget {
  final Responsive responsive;
  final ColorScheme colorScheme;
  final String title;

  const _Header({
    required this.responsive,
    required this.colorScheme,
    required this.title,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: responsive.scale(min: 20, max: 46),
          height: responsive.scale(min: 20, max: 46),
          decoration: BoxDecoration(
            color: colorScheme.primary.withValues(alpha: 0.10),
            shape: BoxShape.circle,
          ),
          child: Icon(
            Icons.emoji_events,
            size: responsive.iconM,
            color: colorScheme.primary,
          ),
        ),

        SizedBox(width: responsive.spaceS),

        Flexible(
          child: AdaptiveText(
            text: title,
            textAlign: TextAlign.center,
            minFontSize: 8,
            maxLines: 1,
            style: TextStyle(
              fontSize: responsive.titleS,
              fontWeight: FontWeight.w900,
            ),
          ),
        ),
      ],
    );
  }
}

// =============================================================================
// RÉSULTAT
// =============================================================================

class _ResultCard extends StatelessWidget {
  final List<String> teamAPlayers;
  final List<String> teamBPlayers;

  final int teamASets;
  final int teamBSets;

  final List<List<int>> previousSets;

  const _ResultCard({
    required this.teamAPlayers,
    required this.teamBPlayers,
    required this.teamASets,
    required this.teamBSets,
    required this.previousSets,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    final teamAWon = teamASets > teamBSets;

    final winnerPlayers = teamAWon ? teamAPlayers : teamBPlayers;
    final loserPlayers = teamAWon ? teamBPlayers : teamAPlayers;

    final winnerSets = teamAWon ? teamASets : teamBSets;
    final loserSets = teamAWon ? teamBSets : teamASets;

    return Container(
      width: double.infinity,
      padding: EdgeInsets.all(responsive.paddingXS),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(responsive.radiusS),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        children: [
          _TeamResultName(
            players: winnerPlayers,
            color: Colors.red,
            responsive: responsive,
            isWinner: true,
          ),

          SizedBox(height: responsive.spaceXS),

          Expanded(
            child: _SetsTable(previousSets: previousSets, teamAWon: teamAWon),
          ),

          SizedBox(height: responsive.spaceXS),

          _TeamResultName(
            players: loserPlayers,
            color: Colors.grey.shade700,
            responsive: responsive,
            isWinner: false,
          ),

          SizedBox(height: responsive.spaceXS),

          AdaptiveText(
            text: '$winnerSets — $loserSets',
            textAlign: TextAlign.center,
            minFontSize: 10,
            maxLines: 1,
            style: TextStyle(
              fontSize: responsive.titleS,
              fontWeight: FontWeight.w900,
            ),
          ),
        ],
      ),
    );
  }
}

class _TeamResultName extends StatelessWidget {
  final List<String> players;
  final Color color;
  final Responsive responsive;
  final bool isWinner;

  const _TeamResultName({
    required this.players,
    required this.color,
    required this.responsive,
    required this.isWinner,
  });

  @override
  Widget build(BuildContext context) {
    final name = players.join(' / ');

    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        if (isWinner) ...[
          Icon(
            Icons.emoji_events_outlined,
            size: responsive.iconS,
            color: color,
          ),
          SizedBox(width: responsive.spaceXS),
        ],

        Flexible(
          child: AdaptiveText(
            text: name,
            textAlign: TextAlign.center,
            minFontSize: 7,
            maxLines: 2,
            style: TextStyle(
              fontSize: responsive.textM,
              height: 1.05,
              fontWeight: FontWeight.w900,
              color: color,
            ),
          ),
        ),
      ],
    );
  }
}

class _SetsTable extends StatelessWidget {
  final List<List<int>> previousSets;
  final bool teamAWon;

  const _SetsTable({required this.previousSets, required this.teamAWon});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(responsive.radiusS),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Row(
        children: List.generate(3, (index) {
          return Expanded(
            child: _SetColumn(
              setNumber: index + 1,
              set: index < previousSets.length ? previousSets[index] : null,
              teamAWon: teamAWon,
              showRightBorder: index < 2,
            ),
          );
        }),
      ),
    );
  }
}

class _SetColumn extends StatelessWidget {
  final int setNumber;
  final List<int>? set;
  final bool teamAWon;
  final bool showRightBorder;

  const _SetColumn({
    required this.setNumber,
    required this.set,
    required this.teamAWon,
    required this.showRightBorder,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    final winnerScore = set == null
        ? null
        : teamAWon
        ? set![0]
        : set![1];

    final loserScore = set == null
        ? null
        : teamAWon
        ? set![1]
        : set![0];

    final winnerWonSet =
        winnerScore != null && loserScore != null && winnerScore > loserScore;

    final loserWonSet =
        winnerScore != null && loserScore != null && loserScore > winnerScore;

    return Container(
      decoration: BoxDecoration(
        border: showRightBorder
            ? Border(right: BorderSide(color: Colors.grey.shade200))
            : null,
      ),
      child: Column(
        children: [
          Container(
            width: double.infinity,
            padding: EdgeInsets.symmetric(vertical: responsive.spaceXXS),
            decoration: BoxDecoration(
              color: Colors.grey.shade100,
              border: Border(bottom: BorderSide(color: Colors.grey.shade200)),
            ),
            child: AdaptiveText(
              text: 'SET $setNumber',
              textAlign: TextAlign.center,
              minFontSize: 6,
              maxLines: 1,
              style: TextStyle(
                fontSize: responsive.textXXS,
                fontWeight: FontWeight.w800,
                color: Colors.grey.shade600,
              ),
            ),
          ),

          Expanded(
            child: _SetScore(
              score: winnerScore,
              isSetWinner: winnerWonSet,
              winnerColor: Colors.red,
            ),
          ),

          Divider(height: 1, thickness: 1, color: Colors.grey.shade200),

          Expanded(
            child: _SetScore(
              score: loserScore,
              isSetWinner: loserWonSet,
              winnerColor: Colors.red,
            ),
          ),
        ],
      ),
    );
  }
}

class _SetScore extends StatelessWidget {
  final int? score;
  final bool isSetWinner;
  final Color winnerColor;

  const _SetScore({
    required this.score,
    required this.isSetWinner,
    required this.winnerColor,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Center(
      child: Text(
        score?.toString() ?? '—',
        style: TextStyle(
          fontSize: responsive.titleM,
          fontWeight: FontWeight.w900,
          color: score == null
              ? Colors.grey.shade300
              : isSetWinner
              ? winnerColor
              : Colors.grey.shade700,
        ),
      ),
    );
  }
}

// =============================================================================
// ANNONCE ARBITRE
// =============================================================================

class _AnnouncementCard extends StatelessWidget {
  final String announcement;

  const _AnnouncementCard({required this.announcement});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: responsive.paddingM,
        vertical: responsive.paddingXS,
      ),
      decoration: BoxDecoration(
        color: colorScheme.primary.withValues(alpha: 0.07),
        borderRadius: BorderRadius.circular(responsive.radiusS),
        border: Border.all(color: colorScheme.primary.withValues(alpha: 0.20)),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        mainAxisSize: MainAxisSize.max,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.record_voice_over_outlined,
                size: responsive.iconS,
                color: colorScheme.primary,
              ),

              SizedBox(width: responsive.spaceXS),

              Flexible(
                child: AdaptiveText(
                  text: l10n.umpireAnnouncement,
                  textAlign: TextAlign.center,
                  minFontSize: responsive.textXS,
                  maxLines: 1,
                  style: TextStyle(
                    fontSize: responsive.textS,
                    fontWeight: FontWeight.w900,
                    color: colorScheme.primary,
                  ),
                ),
              ),
            ],
          ),

          SizedBox(height: responsive.spaceXS),

          Expanded(
            child: AdaptiveText(
              text: '« $announcement »',
              textAlign: TextAlign.center,
              minFontSize: responsive.textS,
              maxLines: 5,
              style: TextStyle(
                fontSize: responsive.textM,
                height: 1.05,
                fontWeight: FontWeight.w700,
                color: Colors.red,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// =============================================================================
// INFORMATIONS DU MATCH
// =============================================================================

class _MatchInformationCard extends StatelessWidget {
  final Duration matchDuration;

  final int shuttleCount;

  final String umpireName;
  final String? serviceJudgeName;

  final DateTime? matchStartedAt;
  final DateTime? matchFinishedAt;

  const _MatchInformationCard({
    required this.matchDuration,
    required this.shuttleCount,
    required this.umpireName,
    required this.serviceJudgeName,
    required this.matchStartedAt,
    required this.matchFinishedAt,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    final hasServiceJudge =
        serviceJudgeName != null && serviceJudgeName!.trim().isNotEmpty;

    return Container(
      width: double.infinity,
      padding: EdgeInsets.all(responsive.paddingS),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(responsive.radiusS),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final textScale = MediaQuery.textScalerOf(context).scale(1);

          final useThreeColumns = constraints.maxWidth < 520 || textScale > 1.1;

          return Column(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              if (!useThreeColumns) ...[
                // =====================================================
                // MODE NORMAL : 4 + 2
                // =====================================================
                Row(
                  children: [
                    Expanded(
                      flex: 2,
                      child: _InfoItem(
                        icon: Icons.play_circle_outline,
                        label: l10n.startTime,
                        value: _formatTime(matchStartedAt),
                      ),
                    ),

                    _VerticalDivider(),

                    Expanded(
                      flex: 2,
                      child: _InfoItem(
                        icon: Icons.stop_circle_outlined,
                        label: l10n.endTime,
                        value: _formatTime(matchFinishedAt),
                      ),
                    ),

                    _VerticalDivider(),

                    Expanded(
                      flex: 3,
                      child: _InfoItem(
                        icon: Icons.timer_outlined,
                        label: l10n.duration,
                        value: AppFormatters.durationClock(matchDuration),
                      ),
                    ),

                    _VerticalDivider(),

                    Expanded(
                      flex: 2,
                      child: _InfoItem(
                        icon: Symbols.badminton,
                        label: l10n.shuttles,
                        value: '$shuttleCount',
                      ),
                    ),
                  ],
                ),

                SizedBox(height: responsive.spaceXS),

                Divider(height: 1, color: Colors.grey.shade200),

                SizedBox(height: responsive.spaceXS),

                Row(
                  children: [
                    Expanded(
                      child: _InfoItem(
                        icon: Icons.person_outline,
                        label: l10n.umpire,
                        value: umpireName,
                        valueMaxLines: 2,
                      ),
                    ),

                    _VerticalDivider(),

                    Expanded(
                      child: _InfoItem(
                        icon: Icons.person_search_outlined,
                        label: l10n.serviceJudge,
                        value: hasServiceJudge ? serviceJudgeName! : '—',
                        valueMaxLines: 2,
                      ),
                    ),
                  ],
                ),
              ] else ...[
                Row(
                  children: [
                    Expanded(
                      child: _InfoItem(
                        icon: Icons.play_circle_outline,
                        label: l10n.startTime,
                        value: _formatTime(matchStartedAt),
                      ),
                    ),

                    _VerticalDivider(),

                    Expanded(
                      child: _InfoItem(
                        icon: Icons.stop_circle_outlined,
                        label: l10n.endTime,
                        value: _formatTime(matchFinishedAt),
                      ),
                    ),

                    _VerticalDivider(),

                    Expanded(
                      flex: 2,
                      child: _InfoItem(
                        icon: Icons.timer_outlined,
                        label: l10n.duration,
                        value: AppFormatters.durationClock(matchDuration),
                      ),
                    ),
                  ],
                ),

                SizedBox(height: responsive.spaceXS),

                Divider(height: 1, color: Colors.grey.shade200),

                SizedBox(height: responsive.spaceXS),

                Row(
                  children: [
                    Expanded(
                      flex: 2,
                      child: _InfoItem(
                        icon: Symbols.badminton,
                        label: l10n.shuttles,
                        value: '$shuttleCount',
                      ),
                    ),

                    _VerticalDivider(),

                    Expanded(
                      flex: 3,
                      child: _InfoItem(
                        icon: Icons.person_outline,
                        label: l10n.umpire,
                        value: umpireName,
                        valueMaxLines: 2,
                      ),
                    ),

                    _VerticalDivider(),

                    Expanded(
                      flex: 3,
                      child: _InfoItem(
                        icon: Icons.person_search_outlined,
                        label: l10n.serviceJudge,
                        value: hasServiceJudge ? serviceJudgeName! : '—',
                        valueMaxLines: 2,
                      ),
                    ),
                  ],
                ),
              ],
            ],
          );
        },
      ),
    );
  }

  String _formatTime(DateTime? dateTime) {
    if (dateTime == null) {
      return '—';
    }

    final hours = dateTime.hour.toString().padLeft(2, '0');

    final minutes = dateTime.minute.toString().padLeft(2, '0');

    return '$hours:$minutes';
  }
}
// =============================================================================
// INFORMATION COMPACTE
// =============================================================================

class _InfoItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final int valueMaxLines;

  const _InfoItem({
    required this.icon,
    required this.label,
    required this.value,
    this.valueMaxLines = 1,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final colorScheme = Theme.of(context).colorScheme;

    return Padding(
      padding: EdgeInsets.symmetric(horizontal: responsive.spaceXXS),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: responsive.iconS, color: colorScheme.primary),

          SizedBox(width: responsive.spaceXS),

          Expanded(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                AdaptiveText(
                  text: label,
                  textAlign: TextAlign.left,
                  minFontSize: 8,
                  maxLines: 1,
                  style: TextStyle(
                    fontSize: responsive.textXS,
                    fontWeight: FontWeight.w600,
                    color: Colors.grey.shade500,
                  ),
                ),

                AdaptiveText(
                  text: value,
                  textAlign: TextAlign.left,
                  minFontSize: 7,
                  maxLines: valueMaxLines,
                  style: TextStyle(
                    fontSize: responsive.textS,
                    height: 1.0,
                    fontWeight: FontWeight.w900,
                    color: Colors.grey.shade800,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// =============================================================================
// DIVIDER VERTICAL
// =============================================================================

class _VerticalDivider extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Container(
      width: 1,
      height: responsive.scale(min: 28, max: 36),
      margin: EdgeInsets.symmetric(horizontal: responsive.spaceXS),
      color: Colors.grey.shade200,
    );
  }
}

// =============================================================================
// ACTIONS
// =============================================================================

class _Actions extends StatelessWidget {
  final bool canUndo;

  final VoidCallback onUndo;
  final VoidCallback onBack;
  final VoidCallback onNewMatch;

  const _Actions({
    required this.canUndo,
    required this.onUndo,
    required this.onBack,
    required this.onNewMatch,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Wrap(
      alignment: WrapAlignment.end,
      crossAxisAlignment: WrapCrossAlignment.center,
      spacing: responsive.spaceS,
      runSpacing: responsive.spaceXS,
      children: [
        if (canUndo)
          TextButton.icon(
            onPressed: onUndo,
            icon: Icon(Icons.undo, size: responsive.iconS),
            label: AdaptiveText(
              text: l10n.undoLastPoint,
              textAlign: TextAlign.center,
              minFontSize: 8,
              maxLines: 1,
              style: TextStyle(fontSize: responsive.textS),
            ),
          ),

        OutlinedButton.icon(
          style: OutlinedButton.styleFrom(
            minimumSize: Size(0, responsive.buttonHeightS),
          ),
          onPressed: onBack,
          icon: Icon(Icons.arrow_back, size: responsive.iconS),
          label: AdaptiveText(
            text: l10n.back,
            textAlign: TextAlign.center,
            minFontSize: 8,
            maxLines: 1,
            style: TextStyle(fontSize: responsive.textS),
          ),
        ),

        FilledButton.icon(
          style: FilledButton.styleFrom(
            minimumSize: Size(0, responsive.buttonHeightS),
          ),
          onPressed: onNewMatch,
          icon: Icon(Icons.refresh, size: responsive.iconS),
          label: AdaptiveText(
            text: l10n.newMatch,
            textAlign: TextAlign.center,
            minFontSize: 8,
            maxLines: 1,
            style: TextStyle(fontSize: responsive.textS),
          ),
        ),
      ],
    );
  }
}
