import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/core/widgets/adaptive_text.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

class MatchControlPanel extends StatelessWidget {
  final int leftScore;
  final int rightScore;

  final int leftSets;
  final int rightSets;

  final List<List<int>> previousSets;

  final bool teamAIsOnLeft;

  final bool canUndo;
  final int shuttleCount;

  final VoidCallback onUndo;
  final VoidCallback? onIncrementShuttle;
  final VoidCallback? onDecrementShuttle;
  final VoidCallback onOpenActions;

  final bool leftIsServing;
  final bool rightIsServing;
  final bool leftHasMatchPoint;
  final bool rightHasMatchPoint;
  final bool leftHasSetPoint;
  final bool rightHasSetPoint;
  final bool leftHasImportantPoint;
  final bool rightHasImportantPoint;

  const MatchControlPanel({
    super.key,
    required this.leftScore,
    required this.rightScore,
    required this.leftSets,
    required this.rightSets,
    required this.previousSets,
    required this.canUndo,
    required this.shuttleCount,
    required this.onUndo,
    required this.onIncrementShuttle,
    required this.onDecrementShuttle,
    required this.onOpenActions,
    required this.leftIsServing,
    required this.rightIsServing,
    required this.leftHasMatchPoint,
    required this.rightHasMatchPoint,
    required this.leftHasSetPoint,
    required this.rightHasSetPoint,
    required this.leftHasImportantPoint,
    required this.rightHasImportantPoint,
    required this.teamAIsOnLeft,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(responsive.radiusM),
        border: Border.all(color: const Color(0xFFE3E3EA)),
      ),
      child: Padding(
        padding: EdgeInsets.symmetric(
          horizontal: responsive.paddingXS,
          vertical: responsive.paddingXS,
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            // =====================================================
            // SCORE
            // =====================================================
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _ScoreDisplay(
                  score: leftScore,
                  isServing: leftIsServing,
                  isImportantPoint: leftHasImportantPoint,
                  hasSetPoint: leftHasSetPoint,
                  hasMatchPoint: leftHasMatchPoint,
                ),

                Padding(
                  padding: EdgeInsets.symmetric(horizontal: responsive.spaceXS),
                  child: Text(
                    '—',
                    style: TextStyle(
                      fontSize: responsive.scoreS,
                      height: 1,
                      color: Colors.grey.shade400,
                    ),
                  ),
                ),

                _ScoreDisplay(
                  score: rightScore,
                  isServing: rightIsServing,
                  isImportantPoint: rightHasImportantPoint,
                  hasSetPoint: rightHasSetPoint,
                  hasMatchPoint: rightHasMatchPoint,
                ),
              ],
            ),

            // =====================================================
            // SETS
            // =====================================================
            Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AdaptiveText(
                  text: '${l10n.sets} : $leftSets — $rightSets',
                  textAlign: TextAlign.center,
                  minFontSize: responsive.textS,
                  maxLines: 1,
                  style: TextStyle(
                    fontSize: responsive.textM,
                    height: 1,
                    fontWeight: FontWeight.w800,
                    color: Colors.grey.shade700,
                  ),
                ),

                if (previousSets.isNotEmpty) ...[
                  SizedBox(height: responsive.spaceXXS),

                  Wrap(
                    alignment: WrapAlignment.center,
                    spacing: responsive.spaceS,
                    runSpacing: 0,
                    children: [
                      for (var index = 0; index < previousSets.length; index++)
                        Text(
                          teamAIsOnLeft
                              ? '${previousSets[index][0]}–${previousSets[index][1]}'
                              : '${previousSets[index][1]}–${previousSets[index][0]}',
                          style: TextStyle(
                            fontSize: responsive.textS,
                            height: 1,
                            fontWeight: FontWeight.w700,
                            color: Colors.grey.shade600,
                          ),
                        ),
                    ],
                  ),
                ],
              ],
            ),

            // =====================================================
            // ANNULER
            // =====================================================
            TextButton.icon(
              style: TextButton.styleFrom(
                visualDensity: VisualDensity.compact,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                minimumSize: Size(0, responsive.scale(min: 30, max: 36)),
                padding: EdgeInsets.symmetric(
                  horizontal: responsive.paddingM,
                  vertical: responsive.paddingXS,
                ),
              ),
              onPressed: canUndo ? onUndo : null,
              icon: Icon(Icons.undo, size: responsive.iconS),
              label: AdaptiveText(
                text: l10n.undoLastPoint,
                textAlign: TextAlign.center,
                minFontSize: responsive.textXS,
                maxLines: 1,
                style: TextStyle(
                  fontSize: responsive.textM,
                  height: 1,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),

            const Divider(height: 1, thickness: 1),

            // =====================================================
            // VOLANTS
            // =====================================================
            Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AdaptiveText(
                  text: l10n.shuttlesUsed,
                  textAlign: TextAlign.center,
                  minFontSize: 6,
                  maxLines: 1,
                  style: TextStyle(
                    fontSize: responsive.textXS,
                    height: 1,
                    fontWeight: FontWeight.w800,
                  ),
                ),

                SizedBox(height: responsive.spaceXXS),

                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    // Bouton - légèrement plus grand
                    IconButton.outlined(
                      visualDensity: VisualDensity.compact,
                      constraints: BoxConstraints(
                        minWidth: responsive.scale(min: 32, max: 40),
                        minHeight: responsive.scale(min: 32, max: 40),
                      ),
                      padding: EdgeInsets.all(responsive.paddingXS),
                      onPressed: shuttleCount > 1 && onDecrementShuttle != null
                          ? onDecrementShuttle
                          : null,
                      icon: Icon(Icons.remove, size: responsive.iconS),
                    ),

                    Padding(
                      padding: EdgeInsets.symmetric(
                        horizontal: responsive.spaceS,
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            Symbols.badminton,
                            size: responsive.iconM,
                            color: colorScheme.primary,
                          ),

                          SizedBox(width: responsive.spaceXS),

                          Text(
                            '$shuttleCount',
                            style: TextStyle(
                              fontSize: responsive.textL,
                              height: 1,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                        ],
                      ),
                    ),

                    // Bouton + légèrement plus grand
                    IconButton.filled(
                      visualDensity: VisualDensity.compact,
                      constraints: BoxConstraints(
                        minWidth: responsive.scale(min: 32, max: 40),
                        minHeight: responsive.scale(min: 32, max: 40),
                      ),
                      padding: EdgeInsets.all(responsive.paddingXS),
                      onPressed: onIncrementShuttle,
                      icon: Icon(Icons.add, size: responsive.iconS),
                    ),
                  ],
                ),
              ],
            ),

            // =====================================================
            // ACTIONS
            // =====================================================
            FilledButton.tonalIcon(
              style: FilledButton.styleFrom(
                visualDensity: VisualDensity.compact,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                minimumSize: Size(0, responsive.scale(min: 22, max: 28)),
                padding: EdgeInsets.symmetric(
                  horizontal: responsive.paddingS,
                  vertical: responsive.spaceXXS,
                ),
              ),
              onPressed: onOpenActions,
              icon: Icon(Icons.more_horiz, size: responsive.iconXS),
              label: AdaptiveText(
                text: l10n.actions,
                textAlign: TextAlign.center,
                minFontSize: 6,
                maxLines: 1,
                style: TextStyle(
                  fontSize: responsive.textS,
                  height: 1,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ScoreDisplay extends StatelessWidget {
  final int score;
  final bool isServing;
  final bool hasSetPoint;
  final bool hasMatchPoint;
  final bool isImportantPoint;

  const _ScoreDisplay({
    required this.score,
    required this.isServing,
    required this.isImportantPoint,
    required this.hasSetPoint,
    required this.hasMatchPoint,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);

    final label = hasMatchPoint
        ? l10n.matchPoint
        : hasSetPoint
        ? l10n.setPoint
        : null;

    final scoreColor = isImportantPoint
        ? Colors.red
        : isServing
        ? Colors.green
        : Colors.black87;

    return SizedBox(
      width: responsive.scale(min: 50, max: 72),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          AnimatedDefaultTextStyle(
            duration: const Duration(milliseconds: 180),
            style: TextStyle(
              fontSize: responsive.scoreM,
              height: 0.95,
              color: scoreColor,
              fontWeight: isServing ? FontWeight.w900 : FontWeight.w500,
            ),
            child: Text('$score'),
          ),

          if (label != null)
            SizedBox(
              height: responsive.scale(min: 10, max: 14),
              child: FittedBox(
                fit: BoxFit.scaleDown,
                child: Text(
                  label,
                  maxLines: 1,
                  style: TextStyle(
                    fontSize: responsive.textXXS,
                    height: 1,
                    fontWeight: FontWeight.w900,
                    color: Colors.red,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
