import 'package:fenuasmash/app/app.dart';
import 'package:fenuasmash/core/config/app_config.dart';
import 'package:fenuasmash/core/routing/app_routes.dart';
import 'package:fenuasmash/features/matches/screens/match_list_screen.dart';
import 'package:fenuasmash/features/pairing/screens/pairing_screen.dart';
import 'package:fenuasmash/features/scoring/screens/score_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  Future<void> startApp(WidgetTester tester) async {
    await tester.pumpWidget(FenuaSmashApp(config: AppConfig.fromEnvironment()));
    await tester.pumpAndSettle();
  }

  testWidgets('starts on the pairing screen', (tester) async {
    await startApp(tester);

    expect(find.byType(PairingScreen), findsOneWidget);
  });

  testWidgets('goes from pairing to matches to score, and back', (
    tester,
  ) async {
    await startApp(tester);

    await tester.tap(find.byType(FilledButton));
    await tester.pumpAndSettle();
    expect(find.byType(MatchListScreen), findsOneWidget);
    expect(find.byType(BackButton), findsNothing);

    await tester.tap(find.byType(ListTile));
    await tester.pumpAndSettle();
    expect(find.byType(ScoreScreen), findsOneWidget);
    expect(
      tester.widget<ScoreScreen>(find.byType(ScoreScreen)).matchId,
      'example',
    );

    await tester.pageBack();
    await tester.pumpAndSettle();
    expect(find.byType(MatchListScreen), findsOneWidget);
  });

  test('builds score paths with an encoded match id', () {
    expect(AppRoutes.score('abc'), '/matches/abc/score');
    expect(AppRoutes.score('a/b'), '/matches/a%2Fb/score');
  });
}
