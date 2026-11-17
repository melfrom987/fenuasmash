import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class StartMatchButton extends StatelessWidget {
  final bool enabled;
  final VoidCallback onPressed;

  const StartMatchButton({
    super.key,
    required this.enabled,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);

    return FilledButton(
      onPressed: enabled ? onPressed : null,
      style: FilledButton.styleFrom(
        minimumSize: Size.fromHeight(responsive.buttonHeightL),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(responsive.radiusL),
        ),
      ),
      child: Text(
        l10n.startMatch,
        style: TextStyle(
          fontSize: responsive.titleM,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
