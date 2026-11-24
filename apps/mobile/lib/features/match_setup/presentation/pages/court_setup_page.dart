import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/match/domain/models/match_config.dart';
import 'package:badminton_umpire/features/match/presentation/pages/match_page.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/court/badminton_court.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/dialogs/player_color_dialog.dart';
import 'package:badminton_umpire/features/match_setup/presentation/widgets/court_setup_team_card.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class CourtSetupPage extends StatefulWidget {
  final MatchConfig config;

  const CourtSetupPage({super.key, required this.config});

  @override
  State<CourtSetupPage> createState() => _CourtSetupPageState();
}

class _CourtSetupPageState extends State<CourtSetupPage> {
  String leftSideTeamId = 'teamA';
  String? selectedServerName;
  String? selectedReceiverName;
  late final Map<String, Color> playerColors;

  MatchConfig get config => widget.config;

  String _teamIdForPlayer(String playerName) {
    return config.teamAPlayers.contains(playerName) ? 'teamA' : 'teamB';
  }

  List<String> _playersForTeam(String teamId) {
    return teamId == 'teamA' ? config.teamAPlayers : config.teamBPlayers;
  }

  String _oppositeTeamId(String teamId) {
    return teamId == 'teamA' ? 'teamB' : 'teamA';
  }

  bool _isServer(String playerName) {
    return selectedServerName == playerName;
  }

  bool _isReceiver(String playerName) {
    return selectedReceiverName == playerName;
  }

  bool _serverWouldReplaceCurrentSelection(String playerName) {
    if (selectedReceiverName == null) {
      return false;
    }

    return _teamIdForPlayer(playerName) ==
        _teamIdForPlayer(selectedReceiverName!);
  }

  bool _receiverWouldReplaceCurrentSelection(String playerName) {
    if (selectedServerName == null) {
      return false;
    }

    return _teamIdForPlayer(playerName) ==
        _teamIdForPlayer(selectedServerName!);
  }

  void _selectServer(String playerName) {
    setState(() {
      if (!config.isDoubles) {
        selectedServerName = playerName;

        final oppositeTeamId = _oppositeTeamId(_teamIdForPlayer(playerName));

        selectedReceiverName = _playersForTeam(oppositeTeamId).first;

        return;
      }

      selectedServerName = playerName;

      if (selectedReceiverName != null &&
          _teamIdForPlayer(selectedReceiverName!) ==
              _teamIdForPlayer(playerName)) {
        selectedReceiverName = null;
      }
    });
  }

  void _selectReceiver(String playerName) {
    setState(() {
      if (!config.isDoubles) {
        selectedReceiverName = playerName;

        final oppositeTeamId = _oppositeTeamId(_teamIdForPlayer(playerName));

        selectedServerName = _playersForTeam(oppositeTeamId).first;

        return;
      }

      selectedReceiverName = playerName;

      if (selectedServerName != null &&
          _teamIdForPlayer(selectedServerName!) ==
              _teamIdForPlayer(playerName)) {
        selectedServerName = null;
      }
    });
  }

  void _switchSides() {
    setState(() {
      leftSideTeamId = leftSideTeamId == 'teamA' ? 'teamB' : 'teamA';
    });
  }

  bool get _isConfigurationValid {
    if (selectedServerName == null || selectedReceiverName == null) {
      return false;
    }

    return _teamIdForPlayer(selectedServerName!) !=
        _teamIdForPlayer(selectedReceiverName!);
  }

  List<String> _previewPlayersForTeam(String teamId) {
    final players = List<String>.from(_playersForTeam(teamId));

    if (!config.isDoubles) {
      final player = players.first;

      if (selectedServerName == null) {
        return [player, ''];
      }

      final servingTeamId = _teamIdForPlayer(selectedServerName!);

      final teamIsServing = teamId == servingTeamId;
      final servingTeamIsOnLeft = servingTeamId == leftSideTeamId;

      final serverIsTop = !servingTeamIsOnLeft;
      final playerIsTop = teamIsServing ? serverIsTop : !serverIsTop;

      return playerIsTop ? [player, ''] : ['', player];
    }

    if (selectedServerName == null || selectedReceiverName == null) {
      return players;
    }

    final servingTeamId = _teamIdForPlayer(selectedServerName!);

    final selectedPlayer = teamId == servingTeamId
        ? selectedServerName!
        : selectedReceiverName!;

    final partner = players.firstWhere((player) => player != selectedPlayer);

    final servingTeamIsOnLeft = servingTeamId == leftSideTeamId;

    final serverIsTop = !servingTeamIsOnLeft;

    final selectedPlayerIsTop = teamId == servingTeamId
        ? serverIsTop
        : !serverIsTop;

    return selectedPlayerIsTop
        ? [selectedPlayer, partner]
        : [partner, selectedPlayer];
  }

  List<String> get _previewLeftPlayers {
    return _previewPlayersForTeam(leftSideTeamId);
  }

  List<String> get _previewRightPlayers {
    return _previewPlayersForTeam(_oppositeTeamId(leftSideTeamId));
  }

  Future<void> _startMatch() async {
    if (!_isConfigurationValid) {
      return;
    }

    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (context) {
          return MatchPage(
            config: config,
            serverName: selectedServerName!,
            receiverName: selectedReceiverName!,
            leftSideTeam: leftSideTeamId,
            autoStartWarmup: true,
            initialPlayerColors: Map<String, Color>.from(playerColors),
          );
        },
      ),
    );
  }

  @override
  void initState() {
    super.initState();

    playerColors = {
      for (final player in config.teamAPlayers) player: const Color(0xFF7E57C2),
      for (final player in config.teamBPlayers) player: const Color(0xFF42A5F5),
    };

    SystemChrome.setPreferredOrientations([
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);
  }

  @override
  void dispose() {
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
      DeviceOrientation.portraitDown,
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);

    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: EdgeInsets.all(responsive.paddingXL),
          child: Column(
            children: [
              Row(
                children: [
                  IconButton(
                    visualDensity: VisualDensity.compact,
                    onPressed: () => Navigator.of(context).pop(),
                    icon: Icon(Icons.arrow_back, size: responsive.iconM),
                  ),
                  SizedBox(width: responsive.spaceXS),
                  Text(
                    '${l10n.court} ${config.courtNumber}',
                    style: TextStyle(
                      fontSize: responsive.titleS,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ],
              ),
              SizedBox(height: responsive.spaceXS),
              Expanded(
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Expanded(flex: 6, child: _buildCourtPanel()),
                    SizedBox(width: responsive.spaceL),
                    Expanded(flex: 4, child: _buildConfigurationPanel()),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCourtPanel() {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Column(
      children: [
        Expanded(
          child: BadmintonCourt(
            leftPlayers: _previewLeftPlayers,
            rightPlayers: _previewRightPlayers,
            serverName: selectedServerName ?? '',
            receiverName: selectedReceiverName ?? '',
            playerColors: playerColors,
            onPlayerTap: _changePlayerColor,
          ),
        ),
        SizedBox(height: responsive.spaceM),
        SizedBox(
          width: double.infinity,
          height: responsive.buttonHeightS,
          child: OutlinedButton.icon(
            onPressed: _switchSides,
            icon: const Icon(Icons.swap_horiz),
            label: Text(l10n.switchSides),
          ),
        ),
      ],
    );
  }

  Future<void> _changePlayerColor(String playerName) async {
    final selectedColor = await showPlayerColorDialog(
      context: context,
      playerName: playerName,
      currentColor: playerColors[playerName] ?? Colors.grey,
    );

    if (!mounted || selectedColor == null) {
      return;
    }

    setState(() {
      playerColors[playerName] = selectedColor;
    });
  }

  Widget _buildConfigurationPanel() {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Column(
      children: [
        Expanded(
          child: CourtSetupTeamCard(
            players: config.teamAPlayers,
            playerColors: playerColors,
            isServer: _isServer,
            isReceiver: _isReceiver,
            serverDiscouraged: _serverWouldReplaceCurrentSelection,
            receiverDiscouraged: _receiverWouldReplaceCurrentSelection,
            onSelectServer: _selectServer,
            onSelectReceiver: _selectReceiver,
            onChangeColor: _changePlayerColor,
          ),
        ),
        SizedBox(height: responsive.spaceS),
        Expanded(
          child: CourtSetupTeamCard(
            players: config.teamBPlayers,
            playerColors: playerColors,
            isServer: _isServer,
            isReceiver: _isReceiver,
            serverDiscouraged: _serverWouldReplaceCurrentSelection,
            receiverDiscouraged: _receiverWouldReplaceCurrentSelection,
            onSelectServer: _selectServer,
            onSelectReceiver: _selectReceiver,
            onChangeColor: _changePlayerColor,
          ),
        ),
        SizedBox(height: responsive.spaceS),
        SizedBox(
          width: double.infinity,
          height: responsive.buttonHeightS,
          child: FilledButton(
            onPressed: _isConfigurationValid ? _startMatch : null,
            child: Text(l10n.startWarmup),
          ),
        ),
      ],
    );
  }
}
