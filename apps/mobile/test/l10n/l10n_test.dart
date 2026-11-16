import 'package:fenuasmash/app/app.dart';
import 'package:fenuasmash/core/config/app_config.dart';
import 'package:fenuasmash/l10n/l10n.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('resolveLocale', () {
    const supported = [Locale('en'), Locale('fr')];

    test('uses the device language when supported', () {
      expect(
        resolveLocale([const Locale('en', 'US')], supported),
        supported[0],
      );
      expect(
        resolveLocale([const Locale('fr', 'PF')], supported),
        supported[1],
      );
    });

    test('uses the first supported language in the device list', () {
      expect(
        resolveLocale([const Locale('de'), const Locale('en')], supported),
        supported[0],
      );
    });

    test('falls back to French', () {
      expect(resolveLocale([const Locale('de')], supported), fallbackLocale);
      expect(resolveLocale(null, supported), fallbackLocale);
      expect(resolveLocale([], supported), fallbackLocale);
    });
  });

  Future<void> startAppIn(WidgetTester tester, List<Locale> locales) async {
    tester.platformDispatcher.localesTestValue = locales;
    addTearDown(tester.platformDispatcher.clearLocalesTestValue);
    await tester.pumpWidget(FenuaSmashApp(config: AppConfig.fromEnvironment()));
    await tester.pumpAndSettle();
  }

  testWidgets('shows French on a French device', (tester) async {
    await startAppIn(tester, [const Locale('fr', 'PF')]);

    expect(find.text('Continuer'), findsOneWidget);
  });

  testWidgets('shows English on an English device', (tester) async {
    await startAppIn(tester, [const Locale('en', 'US')]);

    expect(find.text('Continue'), findsOneWidget);
  });

  testWidgets('shows French on a device in another language', (tester) async {
    await startAppIn(tester, [const Locale('de', 'DE')]);

    expect(find.text('Continuer'), findsOneWidget);
  });
}
