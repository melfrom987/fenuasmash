import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class MatchInformationCard extends StatelessWidget {
  final TextEditingController tournamentController;
  final TextEditingController courtNumberController;
  final TextEditingController umpireNameController;
  final TextEditingController serviceJudgeNameController;

  const MatchInformationCard({
    super.key,
    required this.tournamentController,
    required this.courtNumberController,
    required this.umpireNameController,
    required this.serviceJudgeNameController,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);

    final tournamentField = TextField(
      controller: tournamentController,
      textCapitalization: TextCapitalization.words,
      decoration: InputDecoration(
        labelText: l10n.tournamentName,
        prefixIcon: const Icon(Icons.emoji_events_outlined),
      ),
    );

    final courtField = TextField(
      controller: courtNumberController,
      decoration: InputDecoration(
        labelText: '${l10n.courtNumber} *',
        prefixIcon: const Icon(Icons.location_on_outlined),
      ),
    );

    final umpireField = TextField(
      controller: umpireNameController,
      textCapitalization: TextCapitalization.words,
      decoration: InputDecoration(
        labelText: '${l10n.umpireName} *',
        prefixIcon: const Icon(Icons.person_outline),
      ),
    );

    final serviceJudgeField = TextField(
      controller: serviceJudgeNameController,
      textCapitalization: TextCapitalization.words,
      decoration: InputDecoration(
        labelText: l10n.serviceJudgeName,
        hintText: l10n.optional,
        prefixIcon: const Icon(Icons.person_search_outlined),
      ),
    );

    return Card(
      elevation: 0,
      margin: EdgeInsets.zero,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(responsive.radiusL),
        side: const BorderSide(color: Color(0xFFE4E4EA)),
      ),
      child: Padding(
        padding: EdgeInsets.all(responsive.paddingXL),
        child: LayoutBuilder(
          builder: (context, constraints) {
            final canUseTwoColumns =
                constraints.maxWidth >= responsive.teamColumnsMinWidth;

            if (canUseTwoColumns) {
              return Column(
                children: [
                  Row(
                    children: [
                      Expanded(flex: 2, child: tournamentField),

                      SizedBox(width: responsive.spaceM),

                      Expanded(child: courtField),
                    ],
                  ),

                  SizedBox(height: responsive.spaceM),

                  Row(
                    children: [
                      Expanded(child: umpireField),

                      SizedBox(width: responsive.spaceM),

                      Expanded(child: serviceJudgeField),
                    ],
                  ),

                  SizedBox(height: responsive.spaceS),

                  _RequiredFieldsHint(text: l10n.requiredField),
                ],
              );
            }

            return Column(
              children: [
                tournamentField,

                SizedBox(height: responsive.spaceM),

                courtField,

                SizedBox(height: responsive.spaceM),

                umpireField,

                SizedBox(height: responsive.spaceM),

                serviceJudgeField,

                SizedBox(height: responsive.spaceS),

                _RequiredFieldsHint(text: l10n.requiredField),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _RequiredFieldsHint extends StatelessWidget {
  final String text;

  const _RequiredFieldsHint({required this.text});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    return Align(
      alignment: Alignment.centerLeft,
      child: Text(
        '* $text',
        style: TextStyle(
          fontSize: responsive.textM,
          color: Colors.grey.shade600,
        ),
      ),
    );
  }
}
