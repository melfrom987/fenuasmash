import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:flutter/material.dart';

class CourtSetupRoleButton extends StatelessWidget {
  final String label;
  final bool selected;
  final bool discouraged;
  final VoidCallback onPressed;

  const CourtSetupRoleButton({
    super.key,
    required this.label,
    required this.selected,
    required this.discouraged,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final colorScheme = Theme.of(context).colorScheme;

    return Opacity(
      opacity: discouraged && !selected ? 0.4 : 1,
      child: SizedBox(
        width: responsive.buttonHeightS,
        height: responsive.buttonHeightS,
        child: Material(
          color: selected
              ? colorScheme.primaryContainer
              : colorScheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(responsive.radiusS),
          child: InkWell(
            onTap: onPressed,
            borderRadius: BorderRadius.circular(responsive.radiusS),
            child: Center(
              child: Text(
                label,
                style: TextStyle(
                  fontSize: responsive.textM,
                  fontWeight: FontWeight.w900,
                  color: selected
                      ? colorScheme.onPrimaryContainer
                      : colorScheme.onSurfaceVariant,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
