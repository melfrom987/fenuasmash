import 'package:flutter/painting.dart';

/// One color scale of the design system, from 50 (lightest) to 950 (darkest).
class ColorScale {
  const ColorScale({
    required this.shade50,
    required this.shade100,
    required this.shade200,
    required this.shade300,
    required this.shade400,
    required this.shade500,
    required this.shade600,
    required this.shade700,
    required this.shade800,
    required this.shade900,
    required this.shade950,
  });

  final Color shade50;
  final Color shade100;
  final Color shade200;
  final Color shade300;
  final Color shade400;
  final Color shade500;
  final Color shade600;
  final Color shade700;
  final Color shade800;
  final Color shade900;
  final Color shade950;
}

/// Colors of the FenuaSmash design system (`docs/design-system.md`), the same
/// as the `@theme` block of the web app. Each color has one role.
abstract final class AppColors {
  /// The lagoon. Primary color: buttons, links, free court.
  static const lagon = ColorScale(
    shade50: Color(0xFFECFBFA),
    shade100: Color(0xFFCFF4F1),
    shade200: Color(0xFFA0E8E3),
    shade300: Color(0xFF64D5D0),
    shade400: Color(0xFF2BBCBA),
    shade500: Color(0xFF0FA3A3),
    shade600: Color(0xFF0B8287),
    shade700: Color(0xFF0E676D),
    shade800: Color(0xFF115358),
    shade900: Color(0xFF12454A),
    shade950: Color(0xFF052A2F),
  );

  /// The ocean at night. Dark backgrounds.
  static const ocean = ColorScale(
    shade50: Color(0xFFEEF4FB),
    shade100: Color(0xFFD9E6F5),
    shade200: Color(0xFFB6CDEB),
    shade300: Color(0xFF86ABDC),
    shade400: Color(0xFF5684C8),
    shade500: Color(0xFF3767B3),
    shade600: Color(0xFF2A5197),
    shade700: Color(0xFF23417A),
    shade800: Color(0xFF1E3461),
    shade900: Color(0xFF142746),
    shade950: Color(0xFF0B1A30),
  );

  /// The sunset. Match in progress, scores.
  static const sunset = ColorScale(
    shade50: Color(0xFFFFF4ED),
    shade100: Color(0xFFFFE5D3),
    shade200: Color(0xFFFFC7A6),
    shade300: Color(0xFFFFA06E),
    shade400: Color(0xFFFB7A3F),
    shade500: Color(0xFFF25C1F),
    shade600: Color(0xFFDE4314),
    shade700: Color(0xFFB83213),
    shade800: Color(0xFF922A17),
    shade900: Color(0xFF762516),
    shade950: Color(0xFF401009),
  );

  /// Tiare and hibiscus flowers. Live, badges, highlights.
  static const hibiscus = ColorScale(
    shade50: Color(0xFFFDF2F6),
    shade100: Color(0xFFFCE7EF),
    shade200: Color(0xFFFBCFE0),
    shade300: Color(0xFFF8A8C5),
    shade400: Color(0xFFF2729F),
    shade500: Color(0xFFE8467C),
    shade600: Color(0xFFD6285C),
    shade700: Color(0xFFB91B48),
    shade800: Color(0xFF991A3E),
    shade900: Color(0xFF811A38),
    shade950: Color(0xFF4F081C),
  );

  /// The sand. Warm neutrals: backgrounds, borders, text.
  static const sable = ColorScale(
    shade50: Color(0xFFFAF8F5),
    shade100: Color(0xFFF3EFE8),
    shade200: Color(0xFFE7E0D4),
    shade300: Color(0xFFD3C8B6),
    shade400: Color(0xFFB3A58E),
    shade500: Color(0xFF958670),
    shade600: Color(0xFF786B58),
    shade700: Color(0xFF615648),
    shade800: Color(0xFF4A4238),
    shade900: Color(0xFF2F2A24),
    shade950: Color(0xFF1A1714),
  );

  /// Errors only: the Tailwind red used by the web app.
  static const red100 = Color(0xFFFEE2E2);
  static const red600 = Color(0xFFDC2626);
  static const red800 = Color(0xFF991B1B);
}

/// Background and text of a badge: the text uses the 800 shade of its color.
class BadgeColors {
  const BadgeColors({required this.background, required this.foreground});

  final Color background;
  final Color foreground;

  static final freeCourt = BadgeColors(
    background: AppColors.lagon.shade100,
    foreground: AppColors.lagon.shade800,
  );

  static final inProgress = BadgeColors(
    background: AppColors.sunset.shade100,
    foreground: AppColors.sunset.shade800,
  );

  static final live = BadgeColors(
    background: AppColors.hibiscus.shade100,
    foreground: AppColors.hibiscus.shade800,
  );

  static final disabledCourt = BadgeColors(
    background: AppColors.sable.shade200,
    foreground: AppColors.sable.shade700,
  );

  static const error = BadgeColors(
    background: AppColors.red100,
    foreground: AppColors.red800,
  );
}
