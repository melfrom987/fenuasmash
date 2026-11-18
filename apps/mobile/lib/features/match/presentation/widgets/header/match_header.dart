import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/core/utils/formatters.dart';
import 'package:badminton_umpire/core/widgets/adaptive_text.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

class MatchHeader extends StatelessWidget {
  final String tournamentName;
  final String courtNumber;

  final String category;
  final String? ageCategory;
  final String? tournamentPhase;
  final String? poolGroup;

  final Duration matchDuration;

  final VoidCallback onOpenActions;

  const MatchHeader({
    super.key,
    required this.tournamentName,
    required this.courtNumber,
    required this.category,
    required this.matchDuration,
    required this.onOpenActions,
    this.ageCategory,
    this.tournamentPhase,
    this.poolGroup,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      width: double.infinity,
      constraints: BoxConstraints(minHeight: responsive.headerHeight),
      padding: EdgeInsets.symmetric(
        horizontal: responsive.paddingS,
        vertical: responsive.paddingXS,
      ),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(responsive.radiusM),
        border: Border.all(color: const Color(0xFFE3E3EA)),
      ),
      child: Row(
        children: [
          _HeaderInformation(
            icon: Icons.location_on_outlined,
            label: l10n.court,
            value: courtNumber,
          ),

          const _HeaderDivider(),

          Expanded(
            flex: 2,
            child: _HeaderInformation(
              icon: Icons.emoji_events_outlined,
              label: l10n.defaultTournamentName,
              value: tournamentName,
            ),
          ),

          const _HeaderDivider(),

          Expanded(
            flex: 2,
            child: _MatchInformation(
              category: category,
              ageCategory: ageCategory,
              tournamentPhase: tournamentPhase,
              poolGroup: poolGroup,
            ),
          ),

          const _HeaderDivider(),

          _HeaderInformation(
            icon: Icons.timer_outlined,
            label: l10n.duration,
            value: AppFormatters.durationClock(matchDuration),
          ),

          SizedBox(width: responsive.spaceS),

          IconButton.filledTonal(
            tooltip: l10n.matchActions,
            onPressed: onOpenActions,
            visualDensity: VisualDensity.compact,
            icon: Icon(Icons.more_horiz, size: responsive.iconM),
          ),
        ],
      ),
    );
  }
}

class _MatchInformation extends StatelessWidget {
  final String category;
  final String? ageCategory;
  final String? tournamentPhase;
  final String? poolGroup;

  const _MatchInformation({
    required this.category,
    required this.ageCategory,
    required this.tournamentPhase,
    required this.poolGroup,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    final secondaryParts = <String>[
      if (ageCategory?.trim().isNotEmpty == true) ageCategory!.trim(),

      if (poolGroup?.trim().isNotEmpty == true) poolGroup!.trim(),

      if (tournamentPhase?.trim().isNotEmpty == true) tournamentPhase!.trim(),
    ];

    final secondaryText = secondaryParts.join(' • ');

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(
          Symbols.badminton,
          size: responsive.iconM,
          color: Theme.of(context).colorScheme.primary,
        ),

        SizedBox(width: responsive.spaceXXS),

        Expanded(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              AdaptiveText(
                text: l10n.category,
                textAlign: TextAlign.left,
                minFontSize: 6,
                maxLines: 1,
                style: TextStyle(
                  fontSize: responsive.textXS,
                  height: 1,
                  color: Colors.grey.shade600,
                ),
              ),

              SizedBox(height: responsive.spaceXXS),

              AdaptiveText(
                text: category.isEmpty ? l10n.notProvided : category,
                textAlign: TextAlign.left,
                minFontSize: 7,
                maxLines: 1,
                style: TextStyle(
                  fontSize: responsive.textS,
                  height: 1,
                  fontWeight: FontWeight.w700,
                ),
              ),

              if (secondaryText.isNotEmpty) ...[
                SizedBox(height: responsive.spaceXXS),

                AdaptiveText(
                  text: secondaryText,
                  textAlign: TextAlign.left,
                  minFontSize: 6,
                  maxLines: 1,
                  style: TextStyle(
                    fontSize: responsive.textXS,
                    height: 1,
                    color: Colors.grey.shade600,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }
}

class _HeaderInformation extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;

  const _HeaderInformation({
    required this.icon,
    required this.label,
    required this.value,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(
          icon,
          size: responsive.iconM,
          color: Theme.of(context).colorScheme.primary,
        ),

        SizedBox(width: responsive.spaceXXS),

        Flexible(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              AdaptiveText(
                text: label,
                textAlign: TextAlign.left,
                minFontSize: 6,
                maxLines: 1,
                style: TextStyle(
                  fontSize: responsive.textXS,
                  height: 1,
                  color: Colors.grey.shade600,
                ),
              ),

              SizedBox(height: responsive.spaceXXS),

              AdaptiveText(
                text: value.isEmpty ? l10n.notProvided : value,
                textAlign: TextAlign.left,
                minFontSize: 7,
                maxLines: 1,
                style: TextStyle(
                  fontSize: responsive.textS,
                  height: 1,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _HeaderDivider extends StatelessWidget {
  const _HeaderDivider();

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Container(
      width: responsive.scale(min: 1, max: 1.5),
      height: responsive.scale(min: 28, max: 38),
      margin: EdgeInsets.symmetric(horizontal: responsive.spaceM),
      color: const Color(0xFFE3E3EA),
    );
  }
}
