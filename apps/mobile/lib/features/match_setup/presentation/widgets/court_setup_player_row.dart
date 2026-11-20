import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/match_setup/presentation/widgets/court_setup_role_button.dart';
import 'package:flutter/material.dart';

class CourtSetupPlayerRow extends StatelessWidget {
  final String playerName;
  final Color playerColor;
  final bool serverSelected;
  final bool receiverSelected;
  final bool serverDiscouraged;
  final bool receiverDiscouraged;
  final VoidCallback onSelectServer;
  final VoidCallback onSelectReceiver;
  final VoidCallback onChangeColor;

  const CourtSetupPlayerRow({
    super.key,
    required this.playerName,
    required this.playerColor,
    required this.serverSelected,
    required this.receiverSelected,
    required this.serverDiscouraged,
    required this.receiverDiscouraged,
    required this.onSelectServer,
    required this.onSelectReceiver,
    required this.onChangeColor,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Row(
      children: [
        InkWell(
          onTap: onChangeColor,
          borderRadius: BorderRadius.circular(100),
          child: Padding(
            padding: EdgeInsets.all(responsive.spaceXS),
            child: Container(
              width: responsive.iconS,
              height: responsive.iconS,
              decoration: BoxDecoration(
                color: playerColor,
                shape: BoxShape.circle,
                border: Border.all(
                  color: Theme.of(context).colorScheme.outline,
                ),
              ),
            ),
          ),
        ),
        SizedBox(width: responsive.spaceXS),
        Expanded(
          child: Text(
            playerName,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: responsive.textM,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
        SizedBox(width: responsive.spaceS),
        CourtSetupRoleButton(
          label: 'S',
          selected: serverSelected,
          discouraged: serverDiscouraged,
          onPressed: onSelectServer,
        ),
        SizedBox(width: responsive.spaceXS),
        CourtSetupRoleButton(
          label: 'R',
          selected: receiverSelected,
          discouraged: receiverDiscouraged,
          onPressed: onSelectReceiver,
        ),
      ],
    );
  }
}
