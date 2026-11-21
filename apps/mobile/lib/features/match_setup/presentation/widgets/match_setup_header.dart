import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class MatchSetupHeader extends StatelessWidget {
  final VoidCallback onHome;
  const MatchSetupHeader({super.key, required this.onHome});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);

    return Row(
      children: [
        IconButton(
          tooltip: l10n.home,
          onPressed: onHome,
          icon: Icon(Icons.home_outlined, size: responsive.iconM),
        ),
        SizedBox(width: responsive.spaceS),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                l10n.newMatch,
                style: TextStyle(
                  fontSize: responsive.titleXL,
                  fontWeight: FontWeight.bold,
                ),
              ),
              SizedBox(height: responsive.spaceS),
              Text(
                l10n.matchSetupDescription,
                style: TextStyle(
                  fontSize: responsive.textL,
                  color: Colors.grey.shade600,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
