import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/match_setup/presentation/widgets/court_setup_player_row.dart';
import 'package:flutter/material.dart';

class CourtSetupTeamCard extends StatelessWidget {
  final List<String> players;
  final Map<String, Color> playerColors;
  final bool Function(String playerName) isServer;
  final bool Function(String playerName) isReceiver;
  final bool Function(String playerName) serverDiscouraged;
  final bool Function(String playerName) receiverDiscouraged;
  final void Function(String playerName) onSelectServer;
  final void Function(String playerName) onSelectReceiver;
  final void Function(String playerName) onChangeColor;

  const CourtSetupTeamCard({
    super.key,
    required this.players,
    required this.playerColors,
    required this.isServer,
    required this.isReceiver,
    required this.serverDiscouraged,
    required this.receiverDiscouraged,
    required this.onSelectServer,
    required this.onSelectReceiver,
    required this.onChangeColor,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      width: double.infinity,
      padding: EdgeInsets.symmetric(
        horizontal: responsive.paddingS,
        vertical: responsive.paddingXS,
      ),
      decoration: BoxDecoration(
        color: colorScheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(responsive.radiusM),
        border: Border.all(color: colorScheme.outlineVariant),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          for (final player in players)
            CourtSetupPlayerRow(
              playerName: player,
              playerColor: playerColors[player] ?? Colors.grey,
              serverSelected: isServer(player),
              receiverSelected: isReceiver(player),
              serverDiscouraged: serverDiscouraged(player),
              receiverDiscouraged: receiverDiscouraged(player),
              onSelectServer: () => onSelectServer(player),
              onSelectReceiver: () => onSelectReceiver(player),
              onChangeColor: () => onChangeColor(player),
            ),
        ],
      ),
    );
  }
}
