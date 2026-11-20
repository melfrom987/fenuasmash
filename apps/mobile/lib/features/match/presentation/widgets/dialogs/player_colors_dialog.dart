import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

import '../court/team_color_column.dart';
import 'player_color_dialog.dart';

Future<Map<String, Color>?> showPlayerColorsDialog({
  required BuildContext context,
  required List<String> leftPlayers,
  required List<String> rightPlayers,
  required Map<String, Color> currentColors,
}) {
  final editedColors = Map<String, Color>.from(currentColors);
  final l10n = AppLocalizations.of(context)!;
  final responsive = Responsive.of(context);

  return showDialog<Map<String, Color>>(
    context: context,
    barrierDismissible: false,
    builder: (dialogContext) {
      return StatefulBuilder(
        builder: (context, setDialogState) {
          Future<void> changePlayerColor(String player) async {
            final selectedColor = await showPlayerColorDialog(
              context: context,
              playerName: player,
              currentColor: editedColors[player] ?? Colors.grey,
            );

            if (selectedColor == null) {
              return;
            }

            setDialogState(() {
              editedColors[player] = selectedColor;
            });
          }

          Future<void> applySameColor(List<String> players) async {
            if (players.isEmpty) {
              return;
            }

            final selectedColor = await showPlayerColorDialog(
              context: context,
              playerName: l10n.team,
              currentColor: editedColors[players.first] ?? Colors.grey,
            );

            if (selectedColor == null) {
              return;
            }

            setDialogState(() {
              for (final player in players) {
                editedColors[player] = selectedColor;
              }
            });
          }

          void validateAndClose() {
            Navigator.of(dialogContext).pop(editedColors);
          }

          return Dialog(
            insetPadding: const EdgeInsets.symmetric(
              horizontal: 16,
              vertical: 8,
            ),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 620),
              child: Padding(
                padding: EdgeInsets.fromLTRB(
                  responsive.paddingL,
                  responsive.paddingM,
                  responsive.paddingL,
                  responsive.paddingM,
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            l10n.playerColors,
                            style: TextStyle(
                              fontSize: responsive.titleM,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                        ),
                        IconButton(
                          tooltip: l10n.finish,
                          visualDensity: VisualDensity.compact,
                          onPressed: validateAndClose,
                          icon: const Icon(Icons.close),
                        ),
                      ],
                    ),
                    SizedBox(height: responsive.spaceS),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: TeamColorColumn(
                            title: l10n.leftSide,
                            players: leftPlayers,
                            playerColors: editedColors,
                            onPlayerTap: changePlayerColor,
                            onApplySameColor: () {
                              applySameColor(leftPlayers);
                            },
                          ),
                        ),
                        SizedBox(width: responsive.spaceM),
                        Expanded(
                          child: TeamColorColumn(
                            title: l10n.rightSide,
                            players: rightPlayers,
                            playerColors: editedColors,
                            onPlayerTap: changePlayerColor,
                            onApplySameColor: () {
                              applySameColor(rightPlayers);
                            },
                          ),
                        ),
                      ],
                    ),
                    SizedBox(width: responsive.spaceM),
                    SizedBox(
                      width: double.infinity,
                      height: 36,
                      child: FilledButton.icon(
                        onPressed: validateAndClose,
                        icon: Icon(Icons.check, size: responsive.iconM),
                        label: Text(l10n.finish),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        },
      );
    },
  );
}
