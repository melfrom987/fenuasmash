import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/match/domain/models/match_event.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class DisciplinaryEventResult {
  final String playerName;
  final MatchEventType type;

  const DisciplinaryEventResult({required this.playerName, required this.type});
}

Future<DisciplinaryEventResult?> showDisciplinaryEventDialog({
  required BuildContext context,
  required List<String> players,
}) {
  return showDialog<DisciplinaryEventResult>(
    context: context,
    builder: (context) {
      return _DisciplinaryEventDialog(players: players);
    },
  );
}

class _DisciplinaryEventDialog extends StatefulWidget {
  final List<String> players;

  const _DisciplinaryEventDialog({required this.players});

  @override
  State<_DisciplinaryEventDialog> createState() =>
      _DisciplinaryEventDialogState();
}

class _DisciplinaryEventDialogState extends State<_DisciplinaryEventDialog> {
  String? selectedPlayer;
  MatchEventType? selectedType;

  Future<void> _validate() async {
    if (selectedPlayer == null || selectedType == null) {
      return;
    }

    final confirmed = await _showConfirmation();

    if (!confirmed || !mounted) {
      return;
    }

    Navigator.of(context).pop(
      DisciplinaryEventResult(playerName: selectedPlayer!, type: selectedType!),
    );
  }

  String _typeLabel(AppLocalizations l10n, MatchEventType type) {
    return switch (type) {
      MatchEventType.warning => l10n.warning,
      MatchEventType.yellowCard => l10n.yellowCard,
      MatchEventType.redCard => l10n.redCard,
      MatchEventType.blackCard => l10n.blackCard,
      _ => '',
    };
  }

  Future<bool> _showConfirmation() async {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);
    final typeLabel = _typeLabel(l10n, selectedType!);

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
                        Icons.warning_amber_rounded,
                        size: responsive.iconM,
                        color: Colors.orange,
                      ),
                      SizedBox(width: responsive.spaceS),
                      Expanded(
                        child: Text(
                          l10n.confirmSanction,
                          style: TextStyle(
                            fontSize: responsive.titleS,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ),
                    ],
                  ),

                  SizedBox(height: responsive.spaceM),

                  Row(
                    children: [
                      Expanded(
                        child: _ConfirmationValue(
                          label: l10n.player,
                          value: selectedPlayer!,
                        ),
                      ),
                      SizedBox(width: responsive.spaceM),
                      Expanded(
                        child: _ConfirmationValue(
                          label: l10n.sanction,
                          value: typeLabel,
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
                      color: Colors.orange.withValues(alpha: 0.08),
                      borderRadius: BorderRadius.circular(responsive.radiusS),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          Icons.info_outline,
                          size: responsive.iconS,
                          color: Colors.orange,
                        ),
                        SizedBox(width: responsive.spaceS),
                        Expanded(
                          child: Text(
                            l10n.irreversibleAction,
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
                        child: FilledButton(
                          onPressed: () {
                            Navigator.of(dialogContext).pop(true);
                          },
                          child: Text(l10n.confirm),
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
    final colorScheme = Theme.of(context).colorScheme;

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
                    Icons.warning_amber_rounded,
                    size: responsive.iconM,
                    color: colorScheme.primary,
                  ),
                  SizedBox(width: responsive.spaceS),
                  Expanded(
                    child: Text(
                      l10n.warningOrCard,
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
                items: widget.players.map((player) {
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

              SizedBox(height: responsive.spaceS),

              Row(
                children: [
                  Expanded(
                    child: _SanctionButton(
                      label: l10n.warning,
                      selected: selectedType == MatchEventType.warning,
                      onTap: () {
                        setState(() {
                          selectedType = MatchEventType.warning;
                        });
                      },
                    ),
                  ),

                  SizedBox(width: responsive.spaceXS),

                  Expanded(
                    child: _SanctionButton(
                      label: l10n.yellowCard,
                      color: Colors.amber,
                      selected: selectedType == MatchEventType.yellowCard,
                      onTap: () {
                        setState(() {
                          selectedType = MatchEventType.yellowCard;
                        });
                      },
                    ),
                  ),

                  SizedBox(width: responsive.spaceXS),

                  Expanded(
                    child: _SanctionButton(
                      label: l10n.redCard,
                      color: Colors.red,
                      selected: selectedType == MatchEventType.redCard,
                      onTap: () {
                        setState(() {
                          selectedType = MatchEventType.redCard;
                        });
                      },
                    ),
                  ),

                  SizedBox(width: responsive.spaceXS),

                  Expanded(
                    child: _SanctionButton(
                      label: l10n.blackCard,
                      color: Colors.black,
                      selected: selectedType == MatchEventType.blackCard,
                      onTap: () {
                        setState(() {
                          selectedType = MatchEventType.blackCard;
                        });
                      },
                    ),
                  ),
                ],
              ),

              SizedBox(height: responsive.spaceS),

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
                      onPressed: selectedPlayer == null || selectedType == null
                          ? null
                          : _validate,
                      child: Text(l10n.apply),
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

class _SanctionButton extends StatelessWidget {
  final String label;
  final Color? color;
  final bool selected;
  final VoidCallback onTap;

  const _SanctionButton({
    required this.label,
    required this.selected,
    required this.onTap,
    this.color,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final colorScheme = Theme.of(context).colorScheme;

    return Material(
      color: selected
          ? colorScheme.primary.withValues(alpha: 0.08)
          : const Color(0xFFF7F7FA),
      borderRadius: BorderRadius.circular(responsive.radiusS),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(responsive.radiusS),
        child: Container(
          height: responsive.buttonHeightL,
          padding: EdgeInsets.symmetric(horizontal: responsive.paddingXS),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(responsive.radiusS),
            border: Border.all(
              color: selected ? colorScheme.primary : const Color(0xFFE3E3EA),
              width: selected ? 2 : 1,
            ),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (color != null) ...[
                Container(
                  width: responsive.iconS,
                  height: responsive.iconM,
                  decoration: BoxDecoration(
                    color: color,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
                SizedBox(width: responsive.spaceXS),
              ],
              Flexible(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: responsive.textXS,
                    fontWeight: FontWeight.w800,
                  ),
                ),
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
