import 'package:fenuasmash/app/theme/app_colors.dart';
import 'package:fenuasmash/app/theme/app_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  final theme = AppTheme.light();

  test('uses the design system colors', () {
    expect(theme.colorScheme.primary, AppColors.lagon.shade600);
    expect(theme.colorScheme.onPrimary, Colors.white);
    expect(theme.scaffoldBackgroundColor, AppColors.sable.shade50);
    expect(theme.colorScheme.onSurface, AppColors.sable.shade900);
    expect(theme.colorScheme.onSurfaceVariant, AppColors.sable.shade600);
    expect(theme.colorScheme.error, AppColors.red600);
  });

  test('uses Outfit for headings and Inter for the rest', () {
    final text = theme.textTheme;
    for (final style in [
      text.displayLarge,
      text.headlineMedium,
      text.titleLarge,
    ]) {
      expect(style!.fontFamily, AppFonts.heading);
      expect(style.fontWeight, FontWeight.w600);
    }
    for (final style in [text.bodyLarge, text.bodyMedium, text.bodySmall]) {
      expect(style!.fontFamily, AppFonts.body);
      expect(style.fontWeight, FontWeight.w400);
    }
    expect(text.labelLarge!.fontFamily, AppFonts.body);
    expect(text.labelLarge!.fontWeight, FontWeight.w600);
  });

  test('only uses the two bundled weights', () {
    final text = theme.textTheme;
    for (final style in [
      text.displayLarge,
      text.displayMedium,
      text.displaySmall,
      text.headlineLarge,
      text.headlineMedium,
      text.headlineSmall,
      text.titleLarge,
      text.titleMedium,
      text.titleSmall,
      text.bodyLarge,
      text.bodyMedium,
      text.bodySmall,
      text.labelLarge,
      text.labelMedium,
      text.labelSmall,
    ]) {
      expect(style!.fontWeight, anyOf(FontWeight.w400, FontWeight.w600));
    }
  });

  test('scores use tabular figures', () {
    final style = AppTextStyles.score(fontSize: 96);

    expect(style.fontFeatures, contains(const FontFeature.tabularFigures()));
    expect(style.fontFamily, AppFonts.body);
    expect(style.color, AppColors.sunset.shade700);
  });

  test('bundles the fonts and their licenses', () async {
    TestWidgetsFlutterBinding.ensureInitialized();
    for (final file in [
      'Inter-Regular.ttf',
      'Inter-SemiBold.ttf',
      'Outfit-Regular.ttf',
      'Outfit-SemiBold.ttf',
    ]) {
      final data = await rootBundle.load('assets/fonts/$file');
      expect(data.lengthInBytes, greaterThan(0), reason: file);
    }
    for (final font in ['Inter', 'Outfit']) {
      final license = await rootBundle.loadString('assets/fonts/OFL-$font.txt');
      expect(license, contains('SIL Open Font License'), reason: font);
    }
  });
}
