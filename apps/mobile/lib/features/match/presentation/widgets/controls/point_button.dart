import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:flutter/material.dart';

class PointButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;

  const PointButton({super.key, required this.label, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return SizedBox.expand(
      child: FilledButton(
        onPressed: onPressed,
        style: FilledButton.styleFrom(
          padding: EdgeInsets.symmetric(
            horizontal: responsive.paddingXS,
            vertical: responsive.paddingXS,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(responsive.radiusS),
          ),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.add, size: responsive.iconM),

            SizedBox(height: responsive.spaceXS),

            Flexible(
              child: Text(
                label,
                maxLines: 3,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: responsive.textS,
                  fontWeight: FontWeight.w800,
                  height: 1.1,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
