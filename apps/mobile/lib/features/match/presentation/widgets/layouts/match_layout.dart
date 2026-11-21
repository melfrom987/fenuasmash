import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/court/badminton_court.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/controls/match_control_panel.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/controls/point_button.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/header/match_header.dart';
import 'package:flutter/material.dart';

class MatchLayout extends StatelessWidget {
  final String courtNumber;
  final String tournamentName;
  final String category;

  final String? ageCategory;
  final String? tournamentPhase;
  final String? poolGroup;

  final Duration matchDuration;

  final bool teamAIsOnLeft;

  final List<String> leftPlayers;
  final List<String> rightPlayers;

  final String leftTeamName;
  final String rightTeamName;

  final String serverName;
  final String receiverName;

  final Map<String, Color> playerColors;

  final int leftScore;
  final int rightScore;
  final int leftSets;
  final int rightSets;
  final int shuttleCount;

  final List<List<int>> previousSets;

  final bool controlsLocked;
  final bool canUndo;

  final VoidCallback onLeftPoint;
  final VoidCallback onRightPoint;
  final VoidCallback onUndo;

  final VoidCallback? onIncrementShuttle;
  final VoidCallback? onDecrementShuttle;

  final VoidCallback onOpenActions;

  /// Shows whether the points reached the server; hidden when null.
  final Widget? syncIndicator;

  final bool leftIsServing;
  final bool rightIsServing;

  final bool leftHasMatchPoint;
  final bool rightHasMatchPoint;

  final bool leftHasSetPoint;
  final bool rightHasSetPoint;

  final bool leftHasImportantPoint;
  final bool rightHasImportantPoint;

  final ValueChanged<String> onPlayerTap;

  const MatchLayout({
    super.key,
    required this.courtNumber,
    required this.tournamentName,
    required this.category,
    required this.matchDuration,
    required this.leftPlayers,
    required this.rightPlayers,
    required this.leftTeamName,
    required this.rightTeamName,
    required this.serverName,
    required this.receiverName,
    required this.playerColors,
    required this.leftScore,
    required this.rightScore,
    required this.leftSets,
    required this.rightSets,
    required this.shuttleCount,
    required this.previousSets,
    required this.controlsLocked,
    required this.canUndo,
    required this.onLeftPoint,
    required this.onRightPoint,
    required this.onUndo,
    required this.onIncrementShuttle,
    required this.onDecrementShuttle,
    required this.onOpenActions,
    this.syncIndicator,
    required this.onPlayerTap,
    required this.leftIsServing,
    required this.rightIsServing,
    required this.leftHasMatchPoint,
    required this.rightHasMatchPoint,
    required this.leftHasSetPoint,
    required this.rightHasSetPoint,
    required this.leftHasImportantPoint,
    required this.rightHasImportantPoint,
    required this.teamAIsOnLeft,
    this.ageCategory,
    this.tournamentPhase,
    this.poolGroup,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    final sideButtonWidth = responsive.scale(min: 40, max: 85);

    final controlPanelWidth = responsive.scale(min: 150, max: 290);

    return Padding(
      padding: EdgeInsets.all(responsive.paddingS),
      child: Column(
        children: [
          MatchHeader(
            courtNumber: courtNumber,
            tournamentName: tournamentName,
            category: category,
            ageCategory: ageCategory,
            poolGroup: poolGroup,
            tournamentPhase: tournamentPhase,
            matchDuration: matchDuration,
            onOpenActions: onOpenActions,
            syncIndicator: syncIndicator,
          ),

          SizedBox(height: responsive.spaceXS),

          Expanded(
            child: Row(
              children: [
                Expanded(
                  child: Row(
                    children: [
                      SizedBox(
                        width: sideButtonWidth,
                        child: PointButton(
                          label: leftTeamName,
                          onPressed: controlsLocked ? null : onLeftPoint,
                        ),
                      ),

                      SizedBox(width: responsive.spaceXS),

                      Expanded(
                        child: BadmintonCourt(
                          leftPlayers: leftPlayers,
                          rightPlayers: rightPlayers,
                          serverName: serverName,
                          receiverName: receiverName,
                          playerColors: playerColors,
                          onPlayerTap: onPlayerTap,
                        ),
                      ),

                      SizedBox(width: responsive.spaceXS),

                      SizedBox(
                        width: sideButtonWidth,
                        child: PointButton(
                          label: rightTeamName,
                          onPressed: controlsLocked ? null : onRightPoint,
                        ),
                      ),
                    ],
                  ),
                ),

                SizedBox(width: responsive.spaceS),

                SizedBox(
                  width: controlPanelWidth,
                  child: MatchControlPanel(
                    leftScore: leftScore,
                    rightScore: rightScore,
                    leftSets: leftSets,
                    rightSets: rightSets,
                    previousSets: previousSets,
                    shuttleCount: shuttleCount,
                    canUndo: canUndo,
                    onUndo: onUndo,
                    onIncrementShuttle: onIncrementShuttle,
                    onDecrementShuttle: onDecrementShuttle,
                    onOpenActions: onOpenActions,
                    leftIsServing: leftIsServing,
                    rightIsServing: rightIsServing,
                    leftHasSetPoint: leftHasSetPoint,
                    rightHasSetPoint: rightHasSetPoint,
                    leftHasMatchPoint: leftHasMatchPoint,
                    rightHasMatchPoint: rightHasMatchPoint,
                    leftHasImportantPoint: leftHasImportantPoint,
                    rightHasImportantPoint: rightHasImportantPoint,
                    teamAIsOnLeft: teamAIsOnLeft,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
