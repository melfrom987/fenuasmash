import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/core/widgets/adaptive_text.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class PlayerCard extends StatelessWidget {
  final String playerName;
  final bool isServer;
  final bool isReceiver;
  final Color shirtColor;
  final VoidCallback? onTap;

  const PlayerCard({
    super.key,
    required this.playerName,
    required this.isServer,
    required this.isReceiver,
    required this.shirtColor,
    this.onTap,
  });

  ({String firstName, String lastName}) _splitPlayerName(String fullName) {
    final parts = fullName.trim().split(RegExp(r'\s+'));

    if (parts.length <= 1) {
      return (firstName: '', lastName: fullName.trim());
    }

    return (firstName: parts.first, lastName: parts.skip(1).join(' '));
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);

    final name = _splitPlayerName(playerName);

    final isHighlighted = isServer || isReceiver;

    final borderColor = isServer
        ? const Color(0xFFFF9800)
        : isReceiver
        ? const Color(0xFF2979FF)
        : Colors.white.withValues(alpha: 0.75);

    final roleColor = isServer
        ? const Color(0xFFB86600)
        : const Color(0xFF1457C0);

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(responsive.radiusS),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 220),
          constraints: BoxConstraints(
            minWidth: responsive.scale(min: 78, max: 110),
            maxWidth: responsive.scale(min: 120, max: 190),
          ),
          padding: EdgeInsets.symmetric(
            horizontal: responsive.paddingS,
            vertical: responsive.paddingXS,
          ),
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: isHighlighted ? 1 : 0.9),
            borderRadius: BorderRadius.circular(responsive.radiusS),
            border: Border.all(
              color: borderColor,
              width: isHighlighted ? 3 : 1,
            ),
            boxShadow: isHighlighted
                ? [
                    BoxShadow(
                      color: Colors.black.withValues(alpha: 0.18),
                      blurRadius: responsive.spaceS,
                      offset: Offset(0, responsive.spaceXS),
                    ),
                  ]
                : null,
          ),
          child: Row(
            children: [
              Container(
                width: responsive.scale(min: 5, max: 8),
                height: responsive.scale(min: 28, max: 42),
                decoration: BoxDecoration(
                  color: shirtColor,
                  borderRadius: BorderRadius.circular(responsive.radiusS),
                ),
              ),

              SizedBox(width: responsive.spaceS),

              Expanded(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (name.firstName.isNotEmpty)
                      AdaptiveText(
                        text: name.firstName,
                        textAlign: TextAlign.left,
                        minFontSize: responsive.textS,
                        maxLines: 1,
                        style: TextStyle(
                          color: Colors.black87,
                          fontSize: responsive.textL,
                          height: 1,
                          fontWeight: FontWeight.w600,
                        ),
                      ),

                    AdaptiveText(
                      text: name.lastName.toUpperCase(),
                      textAlign: TextAlign.left,
                      minFontSize: responsive.textS,
                      maxLines: 1,
                      style: TextStyle(
                        color: Colors.black87,
                        fontSize: responsive.textL,
                        height: 1.05,
                        fontWeight: FontWeight.w900,
                      ),
                    ),

                    if (isHighlighted) ...[
                      SizedBox(height: responsive.spaceXXS),
                      AdaptiveText(
                        text: isServer
                            ? l10n.server.toUpperCase()
                            : l10n.receiver.toUpperCase(),
                        textAlign: TextAlign.left,
                        minFontSize: responsive.textS,
                        maxLines: 1,
                        style: TextStyle(
                          fontSize: responsive.textXXS,
                          height: 1,
                          letterSpacing: 0.5,
                          fontWeight: FontWeight.w900,
                          color: roleColor,
                        ),
                      ),
                    ],
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
