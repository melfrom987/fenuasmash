import 'package:fenuasmash/app/app.dart';
import 'package:fenuasmash/core/config/app_config.dart';
import 'package:fenuasmash/core/widgets/page_body.dart';
import 'package:fenuasmash/features/matches/screens/match_list_screen.dart';
import 'package:fenuasmash/features/scoring/screens/score_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// Logical sizes of the devices the app runs on. Any overflow makes the test
/// fail, so every screen is checked on each of them.
const devices = {
  'phone portrait': Size(360, 800),
  'phone landscape': Size(800, 360),
  'small phone landscape': Size(640, 320),
  'tablet portrait': Size(800, 1280),
  'tablet landscape': Size(1280, 800),
};

void main() {
  Future<void> startOn(WidgetTester tester, Size size) async {
    tester.view.physicalSize = size;
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(FenuaSmashApp(config: AppConfig.fromEnvironment()));
    await tester.pumpAndSettle();
  }

  for (final MapEntry(key: name, value: size) in devices.entries) {
    testWidgets('every screen fits on a $name', (tester) async {
      await startOn(tester, size);

      await tester.tap(find.byType(FilledButton));
      await tester.pumpAndSettle();
      expect(find.byType(MatchListScreen), findsOneWidget);

      await tester.tap(find.byType(ListTile));
      await tester.pumpAndSettle();
      expect(find.byType(ScoreScreen), findsOneWidget);

      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('the pairing form stays narrow on a tablet', (tester) async {
    await startOn(tester, devices['tablet landscape']!);

    final button = tester.getSize(find.byType(FilledButton));
    expect(button.width, lessThanOrEqualTo(PageBody.formMaxWidth));
  });

  testWidgets('the pairing form uses the full width of a phone', (
    tester,
  ) async {
    await startOn(tester, devices['phone portrait']!);

    final button = tester.getSize(find.byType(FilledButton));
    expect(button.width, 360 - 2 * 16);
  });

  testWidgets('the match list stays readable on a tablet', (tester) async {
    await startOn(tester, devices['tablet landscape']!);
    await tester.tap(find.byType(FilledButton));
    await tester.pumpAndSettle();

    final tile = tester.getSize(find.byType(ListTile));
    expect(tile.width, PageBody.listMaxWidth - 2 * 32);
  });
}
