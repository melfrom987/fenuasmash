import 'package:flutter/widgets.dart';
import 'package:hugeicons/hugeicons.dart';

/// Icon sizes of the design system.
abstract final class AppIconSize {
  /// Inline with text.
  static const double inline = 16;

  /// Default size.
  static const double regular = 20;

  /// Primary actions.
  static const double primary = 24;
}

/// A Hugeicons icon (Stroke Rounded) with the design system defaults: 20 px,
/// stroke width 1.75, and the color of the surrounding text.
///
/// ```dart
/// AppIcon(HugeIcons.strokeRoundedBadminton, semanticLabel: l10n.score)
/// ```
///
/// Next to a text, the icon is decorative: leave [semanticLabel] empty. Alone
/// in a button, give it a translated label.
class AppIcon extends StatelessWidget {
  const AppIcon(
    this.icon, {
    super.key,
    this.size = AppIconSize.regular,
    this.color,
    this.semanticLabel,
  });

  final List<List<dynamic>> icon;
  final double size;
  final Color? color;
  final String? semanticLabel;

  static const double strokeWidth = 1.75;

  @override
  Widget build(BuildContext context) {
    final iconWidget = HugeIcon(
      icon: icon,
      size: size,
      strokeWidth: strokeWidth,
      color: color ?? DefaultTextStyle.of(context).style.color,
    );
    final label = semanticLabel;
    if (label == null) {
      return ExcludeSemantics(child: iconWidget);
    }
    return Semantics(
      label: label,
      child: ExcludeSemantics(child: iconWidget),
    );
  }
}
