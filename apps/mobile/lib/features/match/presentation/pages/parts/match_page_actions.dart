part of '../match_page.dart';

extension _MatchPageActions on _MatchPageState {
  void _openActionsMenu() {
    showMatchActionsMenu(
      context,
      onCorrectPositions: _correctPlayerPositions,
      onWarningOrCard: _showWarningOrCardDialog,
      onRetirement: _showRetirementDialog,
      onEditPlayerColors: _editPlayerColors,
    );
  }

  Future<void> _correctPlayerPositions() async {
    final config = widget.config;

    final result = await showCorrectPositionsDialog(
      context: context,
      canEdit: matchController.canCorrectPositions,
      isDoubles: config.isDoubles,
      teamAPlayers: config.teamAPlayers,
      teamBPlayers: config.teamBPlayers,
      leftSideTeamId: matchController.leftSideTeamId,
      serverName: matchController.serverName,
      receiverName: matchController.receiverName,
      playerColors: playerColors,
    );

    if (!mounted || result == null) {
      return;
    }

    matchController.correctInitialPositions(
      newLeftSideTeamId: result.leftSideTeamId,
      newServerName: result.serverName,
      newReceiverName: result.receiverName,
    );
  }

  Future<void> _showWarningOrCardDialog() async {
    final config = widget.config;

    final result = await showDisciplinaryEventDialog(
      context: context,
      players: [...config.teamAPlayers, ...config.teamBPlayers],
    );

    if (result == null) {
      return;
    }

    matchController.addDisciplinaryEvent(
      playerName: result.playerName,
      type: result.type,
    );
  }

  Future<void> _showRetirementDialog() async {
    final config = widget.config;

    final playerName = await showRetirementDialog(
      context: context,
      teamAPlayers: config.teamAPlayers,
      teamBPlayers: config.teamBPlayers,
    );

    if (playerName == null) {
      return;
    }

    matchController.retirePlayer(playerName);
  }

  void _undoLastPoint() {
    final l10n = AppLocalizations.of(context)!;

    if (!matchController.canUndo) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(l10n.noPointToUndo)));

      return;
    }

    matchController.undoLastPoint();
  }
}
