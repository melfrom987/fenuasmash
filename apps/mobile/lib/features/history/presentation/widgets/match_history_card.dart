import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/core/utils/formatters.dart';
import 'package:badminton_umpire/features/history/domain/models/match_history_entry.dart';
import 'package:flutter/material.dart';

class MatchHistoryCard extends StatelessWidget {
  final MatchHistoryEntry match;
  final VoidCallback onTap;
  final VoidCallback? onLongPress;

  final bool selectionMode;
  final bool selected;

  const MatchHistoryCard({
    super.key,
    required this.match,
    required this.onTap,
    this.onLongPress,
    this.selectionMode = false,
    this.selected = false,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final colorScheme = Theme.of(context).colorScheme;

    final teamA = match.teamAPlayers.join(' / ');
    final teamB = match.teamBPlayers.join(' / ');

    final scores = match.sets.map((set) => '${set[0]}–${set[1]}').join('  •  ');

    final backgroundColor = selected
        ? colorScheme.primaryContainer.withValues(alpha: 0.35)
        : Colors.white;

    final borderColor = selected
        ? colorScheme.primary
        : const Color(0xFFE3E3EA);

    return Material(
      color: backgroundColor,
      borderRadius: BorderRadius.circular(responsive.radiusM),
      child: InkWell(
        onTap: onTap,
        onLongPress: onLongPress,
        borderRadius: BorderRadius.circular(responsive.radiusM),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 150),
          padding: EdgeInsets.all(responsive.paddingM),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(responsive.radiusM),
            border: Border.all(color: borderColor, width: selected ? 2 : 1),
          ),
          child: Column(
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      match.category,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: responsive.textM,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ),

                  if (selectionMode)
                    Checkbox(
                      value: selected,
                      onChanged: (_) => onTap(),
                      visualDensity: VisualDensity.compact,
                    )
                  else
                    Icon(
                      Icons.chevron_right,
                      size: responsive.iconM,
                      color: Colors.grey.shade500,
                    ),
                ],
              ),

              SizedBox(height: responsive.spaceS),

              _TeamResult(
                name: teamA,
                sets: match.teamASets,
                winner: match.teamASets > match.teamBSets,
              ),

              SizedBox(height: responsive.spaceXS),

              _TeamResult(
                name: teamB,
                sets: match.teamBSets,
                winner: match.teamBSets > match.teamASets,
              ),

              SizedBox(height: responsive.spaceS),

              Divider(height: 1, color: Colors.grey.shade200),

              SizedBox(height: responsive.spaceS),

              Row(
                children: [
                  Expanded(
                    child: Text(
                      scores,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: responsive.textS,
                        color: Colors.grey.shade700,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),

                  SizedBox(width: responsive.spaceS),

                  Icon(
                    Icons.location_on_outlined,
                    size: responsive.iconXS,
                    color: colorScheme.primary,
                  ),

                  SizedBox(width: responsive.spaceXXS),

                  Text(
                    match.courtNumber,
                    style: TextStyle(
                      fontSize: responsive.textS,
                      fontWeight: FontWeight.w700,
                    ),
                  ),

                  SizedBox(width: responsive.spaceS),

                  Icon(
                    Icons.timer_outlined,
                    size: responsive.iconXS,
                    color: Colors.grey.shade600,
                  ),

                  SizedBox(width: responsive.spaceXXS),

                  Text(
                    AppFormatters.durationShort(match.duration),
                    style: TextStyle(
                      fontSize: responsive.textS,
                      color: Colors.grey.shade700,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TeamResult extends StatelessWidget {
  final String name;
  final int sets;
  final bool winner;

  const _TeamResult({
    required this.name,
    required this.sets,
    required this.winner,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Row(
      children: [
        if (winner) ...[
          Icon(Icons.emoji_events, size: responsive.iconS, color: primaryColor),
          SizedBox(width: responsive.spaceXS),
        ],

        Expanded(
          child: Text(
            name,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: responsive.textM,
              fontWeight: winner ? FontWeight.w900 : FontWeight.w600,
            ),
          ),
        ),

        SizedBox(width: responsive.spaceM),

        Text(
          '$sets',
          style: TextStyle(
            fontSize: responsive.titleM,
            fontWeight: FontWeight.w900,
            color: winner ? primaryColor : Colors.black87,
          ),
        ),
      ],
    );
  }
}
