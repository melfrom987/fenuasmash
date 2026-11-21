import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/court/badminton_court.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

class CorrectPositionsResult {
  final String leftSideTeamId;
  final String serverName;
  final String receiverName;

  const CorrectPositionsResult({
    required this.leftSideTeamId,
    required this.serverName,
    required this.receiverName,
  });
}

Future<CorrectPositionsResult?> showCorrectPositionsDialog({
  required BuildContext context,
  required bool canEdit,
  required bool isDoubles,
  required List<String> teamAPlayers,
  required List<String> teamBPlayers,
  required String leftSideTeamId,
  required String serverName,
  required String receiverName,
  required Map<String, Color> playerColors,
}) {
  return showDialog<CorrectPositionsResult>(
    context: context,
    builder: (context) {
      return _CorrectPositionsDialog(
        canEdit: canEdit,
        isDoubles: isDoubles,
        teamAPlayers: teamAPlayers,
        teamBPlayers: teamBPlayers,
        leftSideTeamId: leftSideTeamId,
        serverName: serverName,
        receiverName: receiverName,
        playerColors: playerColors,
      );
    },
  );
}

class _CorrectPositionsDialog extends StatefulWidget {
  final bool canEdit;
  final bool isDoubles;
  final List<String> teamAPlayers;
  final List<String> teamBPlayers;
  final String leftSideTeamId;
  final String serverName;
  final String receiverName;
  final Map<String, Color> playerColors;

  const _CorrectPositionsDialog({
    required this.canEdit,
    required this.isDoubles,
    required this.teamAPlayers,
    required this.teamBPlayers,
    required this.leftSideTeamId,
    required this.serverName,
    required this.receiverName,
    required this.playerColors,
  });

  @override
  State<_CorrectPositionsDialog> createState() =>
      _CorrectPositionsDialogState();
}

class _CorrectPositionsDialogState extends State<_CorrectPositionsDialog> {
  late String selectedLeftSideTeamId;
  late String selectedServerName;
  late String selectedReceiverName;

  @override
  void initState() {
    super.initState();
    selectedLeftSideTeamId = widget.leftSideTeamId;
    selectedServerName = widget.serverName;
    selectedReceiverName = widget.receiverName;
  }

  String _teamIdForPlayer(String playerName) {
    if (widget.teamAPlayers.contains(playerName)) {
      return 'teamA';
    }

    return 'teamB';
  }

  List<String> _playersForTeam(String teamId) {
    return teamId == 'teamA' ? widget.teamAPlayers : widget.teamBPlayers;
  }

  String get _servingTeamId => _teamIdForPlayer(selectedServerName);

  String get _receivingTeamId => _servingTeamId == 'teamA' ? 'teamB' : 'teamA';

  List<String> get _receiverCandidates => _playersForTeam(_receivingTeamId);

  void _changeServer(String playerName) {
    if (!widget.canEdit) {
      return;
    }

    final newServingTeamId = _teamIdForPlayer(playerName);
    final newReceivingTeamId = newServingTeamId == 'teamA' ? 'teamB' : 'teamA';

    final receiverCandidates = _playersForTeam(newReceivingTeamId);

    setState(() {
      selectedServerName = playerName;

      if (!receiverCandidates.contains(selectedReceiverName)) {
        selectedReceiverName = receiverCandidates.first;
      }
    });
  }

  void _changeReceiver(String playerName) {
    if (!widget.canEdit) {
      return;
    }

    setState(() {
      selectedReceiverName = playerName;
    });
  }

  void _switchSides() {
    if (!widget.canEdit) {
      return;
    }

    setState(() {
      selectedLeftSideTeamId = selectedLeftSideTeamId == 'teamA'
          ? 'teamB'
          : 'teamA';
    });
  }

  List<String> _previewPlayersForTeam(String teamId) {
    final players = List<String>.from(_playersForTeam(teamId));

    if (!widget.isDoubles) {
      final player = players.first;
      final teamIsServing = teamId == _servingTeamId;
      final servingTeamIsOnLeft = _servingTeamId == selectedLeftSideTeamId;
      final serverIsTop = !servingTeamIsOnLeft;
      final playerIsTop = teamIsServing ? serverIsTop : !serverIsTop;

      return playerIsTop ? [player, ''] : ['', player];
    }

    final selectedPlayer = teamId == _servingTeamId
        ? selectedServerName
        : selectedReceiverName;

    final partner = players.firstWhere((player) => player != selectedPlayer);

    final servingTeamIsOnLeft = _servingTeamId == selectedLeftSideTeamId;
    final serverIsTop = !servingTeamIsOnLeft;

    final selectedPlayerIsTop = teamId == _servingTeamId
        ? serverIsTop
        : !serverIsTop;

    if (selectedPlayerIsTop) {
      return [selectedPlayer, partner];
    }

    return [partner, selectedPlayer];
  }

  List<String> get _previewLeftPlayers {
    return _previewPlayersForTeam(selectedLeftSideTeamId);
  }

  List<String> get _previewRightPlayers {
    final rightTeamId = selectedLeftSideTeamId == 'teamA' ? 'teamB' : 'teamA';
    return _previewPlayersForTeam(rightTeamId);
  }

  void _apply() {
    Navigator.of(context).pop(
      CorrectPositionsResult(
        leftSideTeamId: selectedLeftSideTeamId,
        serverName: selectedServerName,
        receiverName: selectedReceiverName,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);
    final colorScheme = Theme.of(context).colorScheme;

    final dialogWidth = responsive.scale(min: 560, max: 820);
    final dialogHeight = responsive.scale(min: 300, max: 430);
    final courtHeight = responsive.scale(min: 190, max: 280);

    return Dialog(
      insetPadding: EdgeInsets.symmetric(
        horizontal: responsive.spaceM,
        vertical: responsive.spaceS,
      ),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxWidth: dialogWidth,
          maxHeight: dialogHeight,
        ),
        child: Padding(
          padding: EdgeInsets.fromLTRB(
            responsive.paddingM,
            responsive.paddingS,
            responsive.paddingM,
            responsive.paddingM,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Row(
                children: [
                  Icon(Icons.swap_horiz, size: responsive.iconM),
                  SizedBox(width: responsive.spaceS),
                  Expanded(
                    child: Text(
                      l10n.correctPlayerPositions,
                      style: TextStyle(
                        fontSize: responsive.titleM,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ),
                  IconButton(
                    visualDensity: VisualDensity.compact,
                    onPressed: () => Navigator.of(context).pop(),
                    icon: Icon(Icons.close, size: responsive.iconM),
                  ),
                ],
              ),

              if (!widget.canEdit) ...[
                SizedBox(height: responsive.spaceXS),
                Container(
                  width: double.infinity,
                  padding: EdgeInsets.symmetric(
                    horizontal: responsive.paddingS,
                    vertical: responsive.paddingXS,
                  ),
                  decoration: BoxDecoration(
                    color: colorScheme.errorContainer,
                    borderRadius: BorderRadius.circular(responsive.radiusS),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        Icons.lock_outline,
                        size: responsive.iconS,
                        color: colorScheme.onErrorContainer,
                      ),
                      SizedBox(width: responsive.spaceS),
                      Expanded(
                        child: Text(
                          l10n.positionsOnlyAtZero,
                          style: TextStyle(
                            fontSize: responsive.textS,
                            fontWeight: FontWeight.w600,
                            color: colorScheme.onErrorContainer,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],

              SizedBox(height: responsive.spaceS),

              Expanded(
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Expanded(
                      flex: 6,
                      child: Column(
                        children: [
                          Expanded(
                            child: ConstrainedBox(
                              constraints: BoxConstraints(
                                maxHeight: courtHeight,
                              ),
                              child: BadmintonCourt(
                                leftPlayers: _previewLeftPlayers,
                                rightPlayers: _previewRightPlayers,
                                serverName: selectedServerName,
                                receiverName: selectedReceiverName,
                                playerColors: widget.playerColors,
                                onPlayerTap: null,
                              ),
                            ),
                          ),
                          SizedBox(height: responsive.spaceS),
                          SizedBox(
                            width: double.infinity,
                            height: responsive.buttonHeightS,
                            child: OutlinedButton.icon(
                              onPressed: widget.canEdit ? _switchSides : null,
                              icon: Icon(
                                Icons.swap_horiz,
                                size: responsive.iconS,
                              ),
                              label: Text(
                                l10n.switchSides,
                                style: TextStyle(fontSize: responsive.textS),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),

                    SizedBox(width: responsive.spaceM),

                    Expanded(
                      flex: 4,
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          DropdownButtonFormField<String>(
                            initialValue: selectedServerName,
                            isExpanded: true,
                            decoration: InputDecoration(
                              labelText: l10n.server,
                              prefixIcon: Icon(
                                Symbols.badminton,
                                size: responsive.iconS,
                              ),
                              isDense: true,
                              contentPadding: EdgeInsets.symmetric(
                                horizontal: responsive.paddingS,
                                vertical: responsive.paddingS,
                              ),
                            ),
                            items:
                                [
                                  ...widget.teamAPlayers,
                                  ...widget.teamBPlayers,
                                ].map((player) {
                                  return DropdownMenuItem<String>(
                                    value: player,
                                    child: Text(
                                      player,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(
                                        fontSize: responsive.textM,
                                      ),
                                    ),
                                  );
                                }).toList(),
                            onChanged: widget.canEdit
                                ? (player) {
                                    if (player != null) {
                                      _changeServer(player);
                                    }
                                  }
                                : null,
                          ),

                          SizedBox(height: responsive.spaceM),

                          DropdownButtonFormField<String>(
                            initialValue: selectedReceiverName,
                            isExpanded: true,
                            decoration: InputDecoration(
                              labelText: l10n.receiver,
                              prefixIcon: Icon(
                                Icons.person_outline,
                                size: responsive.iconS,
                              ),
                              isDense: true,
                              contentPadding: EdgeInsets.symmetric(
                                horizontal: responsive.paddingS,
                                vertical: responsive.paddingS,
                              ),
                            ),
                            items: _receiverCandidates.map((player) {
                              return DropdownMenuItem<String>(
                                value: player,
                                child: Text(
                                  player,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(fontSize: responsive.textM),
                                ),
                              );
                            }).toList(),
                            onChanged: widget.canEdit
                                ? (player) {
                                    if (player != null) {
                                      _changeReceiver(player);
                                    }
                                  }
                                : null,
                          ),

                          const Spacer(),

                          Row(
                            children: [
                              Expanded(
                                child: SizedBox(
                                  height: responsive.buttonHeightS,
                                  child: OutlinedButton(
                                    onPressed: () =>
                                        Navigator.of(context).pop(),
                                    child: Text(
                                      l10n.cancel,
                                      style: TextStyle(
                                        fontSize: responsive.textS,
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                              SizedBox(width: responsive.spaceS),
                              Expanded(
                                child: SizedBox(
                                  height: responsive.buttonHeightS,
                                  child: FilledButton(
                                    onPressed: widget.canEdit ? _apply : null,
                                    child: Text(
                                      l10n.apply,
                                      style: TextStyle(
                                        fontSize: responsive.textS,
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
