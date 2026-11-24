import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/core/utils/formatters.dart';
import 'package:badminton_umpire/features/history/domain/models/match_history_entry.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

class MatchHistoryDetailsPage extends StatelessWidget {
  final MatchHistoryEntry match;

  const MatchHistoryDetailsPage({super.key, required this.match});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    final teamA = match.teamAPlayers.join(' / ');
    final teamB = match.teamBPlayers.join(' / ');

    final startTime = AppFormatters.time(match.startedAt);
    final endTime = AppFormatters.time(match.finishedAt);

    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F8),
      appBar: AppBar(
        title: Text(l10n.matchDetails),
        backgroundColor: const Color(0xFFF5F5F8),
      ),
      body: ListView(
        padding: EdgeInsets.all(responsive.paddingL),
        children: [
          _Section(
            title: match.tournamentName,
            children: [
              _Information(
                icon: Icons.calendar_today_outlined,
                label: AppFormatters.dateTime(match.playedAt),
              ),
              _Information(
                icon: Icons.location_on_outlined,
                label: '${l10n.court} ${match.courtNumber}',
              ),
              _Information(icon: Symbols.badminton, label: match.category),
              if (match.ageCategory?.trim().isNotEmpty == true)
                _Information(
                  icon: Icons.category_outlined,
                  label: match.ageCategory!,
                ),

              if (match.poolGroup?.trim().isNotEmpty == true)
                _Information(
                  icon: Icons.groups_outlined,
                  label: match.poolGroup!,
                ),

              if (match.tournamentPhase?.trim().isNotEmpty == true)
                _Information(
                  icon: Icons.account_tree_outlined,
                  label: match.tournamentPhase!,
                ),
              _Information(
                icon: Icons.play_circle_outline,
                label: '${l10n.startTime} : $startTime',
              ),
              _Information(
                icon: Icons.stop_circle_outlined,
                label: '${l10n.endTime} : $endTime',
              ),
              _Information(
                icon: Icons.timer_outlined,
                label:
                    '${l10n.duration} : ${AppFormatters.durationClock(match.duration)}',
              ),
            ],
          ),

          SizedBox(height: responsive.spaceM),

          _Section(
            title: l10n.match,
            children: [
              _MatchTeam(
                name: teamA,
                sets: match.teamASets,
                winner: match.teamASets > match.teamBSets,
              ),
              _MatchTeam(
                name: teamB,
                sets: match.teamBSets,
                winner: match.teamBSets > match.teamASets,
              ),
              SizedBox(height: responsive.spaceS),
              Text(
                match.sets.map((set) => '${set[0]}–${set[1]}').join('  •  '),
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: responsive.titleS,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),

          SizedBox(height: responsive.spaceM),

          _Section(
            title: l10n.umpireInformation,
            children: [
              _Information(icon: Icons.person_outline, label: match.umpireName),
              if (match.serviceJudgeName != null &&
                  match.serviceJudgeName!.trim().isNotEmpty)
                _Information(
                  icon: Icons.person_outline,
                  label: match.serviceJudgeName!,
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Section extends StatelessWidget {
  final String title;
  final List<Widget> children;

  const _Section({required this.title, required this.children});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Container(
      padding: EdgeInsets.all(responsive.paddingL),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(responsive.radiusL),
        border: Border.all(color: const Color(0xFFE3E3EA)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            title,
            style: TextStyle(
              fontSize: responsive.titleS,
              fontWeight: FontWeight.w900,
            ),
          ),
          SizedBox(height: responsive.spaceM),
          ...children,
        ],
      ),
    );
  }
}

class _Information extends StatelessWidget {
  final IconData icon;
  final String label;

  const _Information({required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Padding(
      padding: EdgeInsets.only(bottom: responsive.spaceS),
      child: Row(
        children: [
          Icon(icon, size: responsive.iconS, color: Colors.grey.shade600),
          SizedBox(width: responsive.spaceS),
          Expanded(
            child: Text(
              label,
              style: TextStyle(
                fontSize: responsive.textM,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MatchTeam extends StatelessWidget {
  final String name;
  final int sets;
  final bool winner;

  const _MatchTeam({
    required this.name,
    required this.sets,
    required this.winner,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Padding(
      padding: EdgeInsets.symmetric(vertical: responsive.spaceXS),
      child: Row(
        children: [
          if (winner) ...[
            Icon(
              Icons.emoji_events,
              size: responsive.iconS,
              color: primaryColor,
            ),
            SizedBox(width: responsive.spaceXS),
          ],
          Expanded(
            child: Text(
              name,
              style: TextStyle(
                fontSize: responsive.textM,
                fontWeight: winner ? FontWeight.w900 : FontWeight.w600,
              ),
            ),
          ),
          Text(
            '$sets',
            style: TextStyle(
              fontSize: responsive.titleM,
              fontWeight: FontWeight.w900,
            ),
          ),
        ],
      ),
    );
  }
}
