import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

Future<String?> showRetirementDialog({
  required BuildContext context,
  required List<String> teamAPlayers,
  required List<String> teamBPlayers,
}) {
  return showDialog<String>(
    context: context,
    builder: (context) {
      return _RetirementDialog(
        teamAPlayers: teamAPlayers,
        teamBPlayers: teamBPlayers,
      );
    },
  );
}

class _RetirementDialog extends StatefulWidget {
  final List<String> teamAPlayers;
  final List<String> teamBPlayers;

  const _RetirementDialog({
    required this.teamAPlayers,
    required this.teamBPlayers,
  });

  @override
  State<_RetirementDialog> createState() => _RetirementDialogState();
}

class _RetirementDialogState extends State<_RetirementDialog> {
  String? selectedPlayer;

  List<String> get allPlayers => [
    ...widget.teamAPlayers,
    ...widget.teamBPlayers,
  ];

  String getWinnerName(String retiringPlayer) {
    if (widget.teamAPlayers.contains(retiringPlayer)) {
      return widget.teamBPlayers.join(' / ');
    }

    return widget.teamAPlayers.join(' / ');
  }

  Future<void> _validateRetirement() async {
    if (selectedPlayer == null) {
      return;
    }

    final winnerName = getWinnerName(selectedPlayer!);

    final confirmed = await _showConfirmation(winnerName);

    if (!confirmed || !mounted) {
      return;
    }

    Navigator.of(context).pop(selectedPlayer);
  }

  Future<bool> _showConfirmation(String winnerName) async {
    final responsive = Responsive.of(context);

    final l10n = AppLocalizations.of(context)!;

    final errorColor = Theme.of(context).colorScheme.error;

    final result = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return Dialog(
          insetPadding: EdgeInsets.symmetric(
            horizontal: responsive.spaceM,
            vertical: responsive.spaceS,
          ),
          child: ConstrainedBox(
            constraints: BoxConstraints(maxWidth: responsive.dialogWidthM),
            child: Padding(
              padding: EdgeInsets.all(responsive.paddingM),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      Icon(
                        Icons.flag_outlined,
                        size: responsive.iconM,
                        color: errorColor,
                      ),

                      SizedBox(width: responsive.spaceS),

                      Expanded(
                        child: Text(
                          l10n.confirmRetirement,
                          style: TextStyle(
                            fontSize: responsive.titleS,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ),
                    ],
                  ),

                  SizedBox(height: responsive.spaceM),

                  // Joueur qui abandonne
                  // et futur vainqueur
                  Row(
                    children: [
                      Expanded(
                        child: _ConfirmationValue(
                          label: l10n.retirement,
                          value: selectedPlayer!,
                        ),
                      ),

                      Padding(
                        padding: EdgeInsets.symmetric(
                          horizontal: responsive.spaceS,
                        ),
                        child: Icon(
                          Icons.arrow_forward_rounded,
                          size: responsive.iconM,
                          color: Colors.grey.shade500,
                        ),
                      ),

                      Expanded(
                        child: _ConfirmationValue(
                          label: l10n.winner,
                          value: winnerName,
                        ),
                      ),
                    ],
                  ),

                  SizedBox(height: responsive.spaceS),

                  Container(
                    width: double.infinity,
                    padding: EdgeInsets.symmetric(
                      horizontal: responsive.paddingS,
                      vertical: responsive.paddingXS,
                    ),
                    decoration: BoxDecoration(
                      color: errorColor.withValues(alpha: 0.07),
                      borderRadius: BorderRadius.circular(responsive.radiusS),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          Icons.warning_amber_rounded,
                          size: responsive.iconS,
                          color: errorColor,
                        ),

                        SizedBox(width: responsive.spaceS),

                        Expanded(
                          child: Text(
                            l10n.retirementConfirmationMessage(winnerName),
                            style: TextStyle(
                              fontSize: responsive.textS,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),

                  SizedBox(height: responsive.spaceM),

                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton(
                          onPressed: () {
                            Navigator.of(dialogContext).pop(false);
                          },
                          child: Text(l10n.cancel),
                        ),
                      ),

                      SizedBox(width: responsive.spaceS),

                      Expanded(
                        child: FilledButton.icon(
                          style: FilledButton.styleFrom(
                            backgroundColor: errorColor,
                          ),
                          onPressed: () {
                            Navigator.of(dialogContext).pop(true);
                          },
                          icon: Icon(
                            Icons.flag_outlined,
                            size: responsive.iconS,
                          ),
                          label: Text(l10n.confirmRetirement),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );

    return result ?? false;
  }

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    final l10n = AppLocalizations.of(context)!;

    final errorColor = Theme.of(context).colorScheme.error;

    return Dialog(
      insetPadding: EdgeInsets.symmetric(
        horizontal: responsive.spaceM,
        vertical: responsive.spaceS,
      ),
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: responsive.dialogWidthM),
        child: Padding(
          padding: EdgeInsets.all(responsive.paddingM),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.flag_outlined,
                    size: responsive.iconM,
                    color: errorColor,
                  ),

                  SizedBox(width: responsive.spaceS),

                  Expanded(
                    child: Text(
                      l10n.retirement,
                      style: TextStyle(
                        fontSize: responsive.titleM,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ),

                  IconButton(
                    visualDensity: VisualDensity.compact,
                    onPressed: () {
                      Navigator.of(context).pop();
                    },
                    icon: Icon(Icons.close, size: responsive.iconM),
                  ),
                ],
              ),

              SizedBox(height: responsive.spaceS),

              Text(
                l10n.selectRetiringPlayer,
                style: TextStyle(
                  fontSize: responsive.textS,
                  color: Colors.grey.shade700,
                ),
              ),

              SizedBox(height: responsive.spaceS),

              DropdownButtonFormField<String>(
                initialValue: selectedPlayer,
                isExpanded: true,
                decoration: InputDecoration(
                  labelText: l10n.player,
                  prefixIcon: Icon(
                    Icons.person_outline,
                    size: responsive.iconS,
                  ),
                ),
                items: allPlayers.map((player) {
                  return DropdownMenuItem<String>(
                    value: player,
                    child: Text(
                      player,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  );
                }).toList(),
                onChanged: (value) {
                  setState(() {
                    selectedPlayer = value;
                  });
                },
              ),

              SizedBox(height: responsive.spaceM),

              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () {
                        Navigator.of(context).pop();
                      },
                      child: Text(l10n.cancel),
                    ),
                  ),

                  SizedBox(width: responsive.spaceS),

                  Expanded(
                    child: FilledButton(
                      style: FilledButton.styleFrom(
                        backgroundColor: errorColor,
                      ),
                      onPressed: selectedPlayer == null
                          ? null
                          : _validateRetirement,
                      child: Text(l10n.continueLabel),
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

class _ConfirmationValue extends StatelessWidget {
  final String label;
  final String value;

  const _ConfirmationValue({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          label.toUpperCase(),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: responsive.textXXS,
            color: Colors.grey.shade600,
            fontWeight: FontWeight.w800,
          ),
        ),
        SizedBox(height: responsive.spaceXXS),
        Text(
          value,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: responsive.textM,
            fontWeight: FontWeight.w900,
          ),
        ),
      ],
    );
  }
}
