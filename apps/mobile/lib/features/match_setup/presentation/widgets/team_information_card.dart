import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class TeamInformationCard extends StatelessWidget {
  final String title;
  final bool isDoubles;
  final TextEditingController player1Controller;
  final TextEditingController player2Controller;

  const TeamInformationCard({
    super.key,
    required this.title,
    required this.isDoubles,
    required this.player1Controller,
    required this.player2Controller,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Card(
      child: Padding(
        padding: EdgeInsets.all(responsive.paddingM),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: TextStyle(
                fontSize: responsive.titleS,
                fontWeight: FontWeight.w800,
              ),
            ),
            SizedBox(height: responsive.spaceM),
            TextField(
              controller: player1Controller,
              decoration: InputDecoration(
                labelText: '${l10n.playerOne} *',
                border: OutlineInputBorder(),
              ),
            ),
            if (isDoubles) ...[
              SizedBox(height: responsive.spaceM),
              TextField(
                controller: player2Controller,
                decoration: InputDecoration(
                  labelText: '${l10n.playerTwo} *',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
