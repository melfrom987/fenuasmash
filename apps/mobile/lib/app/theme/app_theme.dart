import 'package:flutter/material.dart';

import 'app_colors.dart';

/// Fonts bundled in `assets/fonts`, so that the app works offline.
abstract final class AppFonts {
  /// Headings: rounded and modern.
  static const heading = 'Outfit';

  /// Body, interface and scores: readable at small sizes, fixed-width figures.
  static const body = 'Inter';
}

/// Text styles that the Material text theme does not cover.
abstract final class AppTextStyles {
  /// Scores and any number that changes live: tabular figures, so that the
  /// layout does not move when a score goes from 9 to 10.
  static TextStyle score({required double fontSize}) {
    return TextStyle(
      fontFamily: AppFonts.body,
      fontSize: fontSize,
      fontWeight: FontWeight.w600,
      fontFeatures: const [FontFeature.tabularFigures()],
      color: AppColors.sunset.shade700,
      height: 1,
    );
  }
}

/// The light theme of the app, built from the design system.
abstract final class AppTheme {
  static ThemeData light() {
    final colorScheme = ColorScheme(
      brightness: Brightness.light,
      primary: AppColors.lagon.shade600,
      onPrimary: Colors.white,
      primaryContainer: AppColors.lagon.shade100,
      onPrimaryContainer: AppColors.lagon.shade800,
      secondary: AppColors.ocean.shade700,
      onSecondary: Colors.white,
      secondaryContainer: AppColors.ocean.shade100,
      onSecondaryContainer: AppColors.ocean.shade800,
      tertiary: AppColors.sunset.shade700,
      onTertiary: Colors.white,
      tertiaryContainer: AppColors.sunset.shade100,
      onTertiaryContainer: AppColors.sunset.shade800,
      error: AppColors.red600,
      onError: Colors.white,
      errorContainer: AppColors.red100,
      onErrorContainer: AppColors.red800,
      surface: Colors.white,
      onSurface: AppColors.sable.shade900,
      onSurfaceVariant: AppColors.sable.shade600,
      surfaceContainerLowest: Colors.white,
      surfaceContainerLow: AppColors.sable.shade50,
      surfaceContainer: AppColors.sable.shade100,
      surfaceContainerHigh: AppColors.sable.shade100,
      surfaceContainerHighest: AppColors.sable.shade200,
      outline: AppColors.sable.shade300,
      outlineVariant: AppColors.sable.shade200,
      inverseSurface: AppColors.ocean.shade950,
      onInverseSurface: AppColors.ocean.shade50,
      inversePrimary: AppColors.lagon.shade300,
    );
    final textTheme = _textTheme(ThemeData(colorScheme: colorScheme).textTheme);
    return ThemeData(
      colorScheme: colorScheme,
      scaffoldBackgroundColor: AppColors.sable.shade50,
      fontFamily: AppFonts.body,
      textTheme: textTheme,
      iconTheme: IconThemeData(color: AppColors.sable.shade900, size: 20),
      dividerTheme: DividerThemeData(color: AppColors.sable.shade200),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(foregroundColor: AppColors.lagon.shade700),
      ),
    );
  }

  /// Outfit for headings and titles, Inter for the rest. Only two weights:
  /// regular (400) and semibold (600).
  static TextTheme _textTheme(TextTheme base) {
    TextStyle heading(TextStyle? style) => style!.copyWith(
      fontFamily: AppFonts.heading,
      fontWeight: FontWeight.w600,
    );
    TextStyle semibold(TextStyle? style) =>
        style!.copyWith(fontFamily: AppFonts.body, fontWeight: FontWeight.w600);
    TextStyle regular(TextStyle? style) =>
        style!.copyWith(fontFamily: AppFonts.body, fontWeight: FontWeight.w400);
    return base.copyWith(
      displayLarge: heading(base.displayLarge),
      displayMedium: heading(base.displayMedium),
      displaySmall: heading(base.displaySmall),
      headlineLarge: heading(base.headlineLarge),
      headlineMedium: heading(base.headlineMedium),
      headlineSmall: heading(base.headlineSmall),
      titleLarge: heading(base.titleLarge),
      titleMedium: semibold(base.titleMedium),
      titleSmall: semibold(base.titleSmall),
      bodyLarge: regular(base.bodyLarge),
      bodyMedium: regular(base.bodyMedium),
      bodySmall: regular(base.bodySmall),
      labelLarge: semibold(base.labelLarge),
      labelMedium: semibold(base.labelMedium),
      labelSmall: semibold(base.labelSmall),
    );
  }
}
