import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class TeamColorColumn extends StatelessWidget {
  final String title;
  final List<String> players;
  final Map<String, Color> playerColors;
  final ValueChanged<String> onPlayerTap;
  final VoidCallback onApplySameColor;

  const TeamColorColumn({
    super.key,
    required this.title,
    required this.players,
    required this.playerColors,
    required this.onPlayerTap,
    required this.onApplySameColor,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: const Color(0xFFF7F7FA),
        borderRadius: BorderRadius.circular(responsive.radiusM),
        border: Border.all(color: const Color(0xFFE3E3EA)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  title,
                  style: TextStyle(
                    fontSize: responsive.textL,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
              if (players.length > 1)
                IconButton(
                  tooltip: l10n.sameColor,
                  visualDensity: VisualDensity.compact,
                  constraints: const BoxConstraints(
                    minWidth: 32,
                    minHeight: 32,
                  ),
                  padding: EdgeInsets.zero,
                  onPressed: onApplySameColor,
                  icon: Icon(Icons.format_color_fill, size: responsive.iconM),
                ),
            ],
          ),
          SizedBox(height: responsive.spaceS),
          for (var index = 0; index < players.length; index++) ...[
            _PlayerColorTile(
              playerName: players[index],
              color: playerColors[players[index]] ?? Colors.grey,
              onTap: () {
                onPlayerTap(players[index]);
              },
            ),
            if (index < players.length - 1) SizedBox(height: responsive.spaceS),
          ],
        ],
      ),
    );
  }
}

class _PlayerColorTile extends StatelessWidget {
  final String playerName;
  final Color color;
  final VoidCallback onTap;

  const _PlayerColorTile({
    required this.playerName,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(responsive.radiusS),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(responsive.radiusS),
        child: SizedBox(
          height: 38,
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: responsive.paddingM),
            child: Row(
              children: [
                Container(
                  width: 20,
                  height: 20,
                  decoration: BoxDecoration(
                    color: color,
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.grey.shade400),
                  ),
                ),
                SizedBox(width: responsive.spaceM),
                Expanded(
                  child: Text(
                    playerName,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                Icon(Icons.palette_outlined, size: responsive.iconS),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
