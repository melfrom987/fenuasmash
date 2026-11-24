import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/core/settings/settings.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/dialogs/umpire_announcement_dialog.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

class NextSetSetupOverlay extends StatefulWidget {
  final List<String> leftPlayers;
  final List<String> rightPlayers;
  final String servingTeamId;
  final String leftTeamId;
  final bool isDoubles;
  final int nextSetNumber;

  final void Function({
    required String serverName,
    required String receiverName,
  })
  onStartNextSet;

  const NextSetSetupOverlay({
    super.key,
    required this.leftPlayers,
    required this.rightPlayers,
    required this.servingTeamId,
    required this.leftTeamId,
    required this.onStartNextSet,
    required this.isDoubles,
    required this.nextSetNumber,
  });

  @override
  State<NextSetSetupOverlay> createState() {
    return _NextSetSetupOverlayState();
  }
}

class _NextSetSetupOverlayState extends State<NextSetSetupOverlay> {
  String? selectedServer;
  String? selectedReceiver;

  bool get _servingTeamIsOnLeft {
    return widget.servingTeamId == widget.leftTeamId;
  }

  String get _singleServerName {
    return _servingTeamIsOnLeft
        ? widget.leftPlayers.first
        : widget.rightPlayers.first;
  }

  String get _singleReceiverName {
    return _servingTeamIsOnLeft
        ? widget.rightPlayers.first
        : widget.leftPlayers.first;
  }

  bool get _isValid {
    if (!widget.isDoubles) {
      return true;
    }

    return selectedServer != null && selectedReceiver != null;
  }

  @override
  void initState() {
    super.initState();

    if (!widget.isDoubles) {
      selectedServer = _singleServerName;
      selectedReceiver = _singleReceiverName;
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);

    return Positioned.fill(
      child: ColoredBox(
        color: Colors.black.withValues(alpha: 0.72),
        child: SafeArea(
          child: Center(
            child: Container(
              width: responsive.dialogWidthM,
              padding: EdgeInsets.all(responsive.paddingM),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(responsive.radiusL),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    l10n.nextSet,
                    style: TextStyle(
                      fontSize: responsive.titleM,
                      fontWeight: FontWeight.w900,
                    ),
                  ),

                  SizedBox(height: responsive.spaceS),

                  Text(
                    widget.isDoubles
                        ? l10n.chooseServerReceiver
                        : l10n.checkServerReceiver,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: responsive.textM,
                      color: Colors.grey.shade600,
                    ),
                  ),

                  SizedBox(height: responsive.spaceM),

                  if (widget.isDoubles)
                    _buildDoublesSelection(context)
                  else
                    _buildSinglesConfirmation(context),

                  SizedBox(height: responsive.spaceM),

                  SizedBox(
                    width: double.infinity,
                    height: responsive.buttonHeightM,
                    child: FilledButton.icon(
                      onPressed: _isValid ? _confirmAndStartSet : null,
                      icon: Icon(
                        widget.isDoubles ? Icons.play_arrow : Icons.check,
                        size: responsive.iconM,
                      ),
                      label: Text(
                        widget.isDoubles
                            ? l10n.startSet
                            : l10n.confirmAndStartSet,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildDoublesSelection(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: _PlayerColumn(
            sideTitle: l10n.left,
            roleTitle: _servingTeamIsOnLeft ? l10n.server : l10n.receiver,
            players: widget.leftPlayers,
            selectedPlayer: _servingTeamIsOnLeft
                ? selectedServer
                : selectedReceiver,
            onSelected: (player) {
              setState(() {
                if (_servingTeamIsOnLeft) {
                  selectedServer = player;
                } else {
                  selectedReceiver = player;
                }
              });
            },
          ),
        ),

        SizedBox(width: responsive.spaceS),

        Expanded(
          child: _PlayerColumn(
            sideTitle: l10n.right,
            roleTitle: _servingTeamIsOnLeft ? l10n.receiver : l10n.server,
            players: widget.rightPlayers,
            selectedPlayer: _servingTeamIsOnLeft
                ? selectedReceiver
                : selectedServer,
            onSelected: (player) {
              setState(() {
                if (_servingTeamIsOnLeft) {
                  selectedReceiver = player;
                } else {
                  selectedServer = player;
                }
              });
            },
          ),
        ),
      ],
    );
  }

  Widget _buildSinglesConfirmation(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);
    final colorScheme = Theme.of(context).colorScheme;

    return Row(
      children: [
        Expanded(
          child: _SingleRoleCard(
            sideLabel: l10n.left,
            title: l10n.server,
            playerName: _singleServerName,
            icon: Symbols.badminton,
            color: colorScheme.primary,
          ),
        ),

        SizedBox(width: responsive.spaceM),

        Expanded(
          child: _SingleRoleCard(
            sideLabel: l10n.right,
            title: l10n.receiver,
            playerName: _singleReceiverName,
            icon: Icons.person_outline,
            color: colorScheme.secondary,
          ),
        ),
      ],
    );
  }

  Future<void> _confirmAndStartSet() async {
    if (!_isValid) {
      return;
    }

    if (!appSettingsController.showUmpireAnnouncements) {
      _startSet();
      return;
    }

    final l10n = AppLocalizations.of(context)!;

    final announcement = widget.nextSetNumber == 2
        ? l10n.secondGameStartAnnouncement
        : l10n.finalGameStartAnnouncement;

    final confirmed = await showUmpireAnnouncementDialog(
      context: context,
      announcement: announcement,
      buttonLabel: l10n.startSet,
    );

    if (!confirmed || !mounted) {
      return;
    }

    _startSet();
  }

  void _startSet() {
    widget.onStartNextSet(
      serverName: selectedServer!,
      receiverName: selectedReceiver!,
    );
  }
}

class _PlayerColumn extends StatelessWidget {
  final String sideTitle;
  final String roleTitle;
  final List<String> players;
  final String? selectedPlayer;
  final ValueChanged<String> onSelected;

  const _PlayerColumn({
    required this.sideTitle,
    required this.roleTitle,
    required this.players,
    required this.selectedPlayer,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    return Container(
      padding: const EdgeInsets.all(9),
      decoration: BoxDecoration(
        color: const Color(0xFFF7F7FA),
        borderRadius: BorderRadius.circular(responsive.radiusM),
        border: Border.all(color: const Color(0xFFE3E3EA)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Text(
                sideTitle,
                style: TextStyle(
                  fontSize: responsive.textS,
                  color: Colors.grey.shade600,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const Spacer(),
              Text(
                roleTitle,
                style: TextStyle(
                  fontSize: responsive.textM,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ],
          ),
          SizedBox(height: responsive.spaceS),
          for (var index = 0; index < players.length; index++) ...[
            _PlayerChoice(
              playerName: players[index],
              isSelected: selectedPlayer == players[index],
              onTap: () {
                onSelected(players[index]);
              },
            ),
            if (index < players.length - 1)
              SizedBox(height: responsive.spaceXS),
          ],
        ],
      ),
    );
  }
}

class _PlayerChoice extends StatelessWidget {
  final String playerName;
  final bool isSelected;
  final VoidCallback onTap;

  const _PlayerChoice({
    required this.playerName,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    final responsive = Responsive.of(context);

    return Material(
      color: isSelected ? colorScheme.primaryContainer : Colors.white,
      borderRadius: BorderRadius.circular(11),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(11),
        child: SizedBox(
          height: 38,
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: responsive.paddingS),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    playerName,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: responsive.textM,
                      fontWeight: isSelected
                          ? FontWeight.w800
                          : FontWeight.w600,
                    ),
                  ),
                ),
                Icon(
                  isSelected
                      ? Icons.radio_button_checked
                      : Icons.radio_button_unchecked,
                  size: responsive.iconM,
                  color: isSelected
                      ? colorScheme.primary
                      : Colors.grey.shade500,
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _SingleRoleCard extends StatelessWidget {
  final String sideLabel;
  final String title;
  final String playerName;
  final IconData icon;
  final Color color;

  const _SingleRoleCard({
    required this.sideLabel,
    required this.title,
    required this.playerName,
    required this.icon,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Container(
      padding: EdgeInsets.all(responsive.paddingM),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(responsive.radiusM),
        border: Border.all(color: color.withValues(alpha: 0.25)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            sideLabel.toUpperCase(),
            style: TextStyle(
              fontSize: responsive.textXS,
              fontWeight: FontWeight.w900,
              color: Colors.grey.shade700,
              letterSpacing: 0.8,
            ),
          ),

          SizedBox(height: responsive.spaceXS),

          Icon(icon, size: responsive.iconM, color: color),

          SizedBox(height: responsive.spaceS),

          Text(
            title.toUpperCase(),
            style: TextStyle(
              fontSize: responsive.textXS,
              fontWeight: FontWeight.w900,
              color: color,
            ),
          ),

          SizedBox(height: responsive.spaceXS),

          Text(
            playerName,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: responsive.textM,
              fontWeight: FontWeight.w900,
            ),
          ),
        ],
      ),
    );
  }
}
