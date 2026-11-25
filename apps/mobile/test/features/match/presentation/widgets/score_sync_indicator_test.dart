import 'package:badminton_umpire/features/match/data/services/score_event_sync.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/header/match_header.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/header/score_sync_indicator.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

const wideScreen = Size(1280, 800);
const narrowScreen = Size(800, 360);

void main() {
  void useScreen(WidgetTester tester, Size size) {
    tester.view.physicalSize = size;
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
  }

  Widget app(Widget child, {Locale locale = const Locale('fr')}) {
    return MaterialApp(
      locale: locale,
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: Scaffold(body: Center(child: child)),
    );
  }

  group('ScoreSyncIndicator', () {
    const cases = {
      ScoreSyncStatus.upToDate: (
        'À jour',
        'Up to date',
        Icons.cloud_done_outlined,
      ),
      ScoreSyncStatus.pending: (
        '3 en attente',
        '3 pending',
        Icons.cloud_upload_outlined,
      ),
      ScoreSyncStatus.offline: (
        'Hors ligne · 3 en attente',
        'Offline · 3 pending',
        Icons.cloud_off_outlined,
      ),
      ScoreSyncStatus.blocked: (
        'Envoi bloqué',
        'Sending blocked',
        Icons.error_outline,
      ),
    };

    for (final MapEntry(key: status, value: (french, english, icon))
        in cases.entries) {
      testWidgets('shows ${status.name} in French and English', (tester) async {
        useScreen(tester, wideScreen);
        final indicator = ScoreSyncIndicator(status: status, pendingCount: 3);

        await tester.pumpWidget(app(indicator));
        expect(find.text(french), findsOneWidget);
        expect(find.byIcon(icon), findsOneWidget);

        await tester.pumpWidget(app(indicator, locale: const Locale('en')));
        await tester.pumpAndSettle();
        expect(find.text(english), findsOneWidget);
      });
    }

    testWidgets('builds its state from a sync report', (tester) async {
      useScreen(tester, wideScreen);
      const report = ScoreSyncReport(
        confirmedCount: 0,
        pendingCount: 2,
        reachable: false,
      );

      await tester.pumpWidget(app(ScoreSyncIndicator.fromReport(report)));

      expect(find.text('Hors ligne · 2 en attente'), findsOneWidget);
    });

    testWidgets('uses a short form on narrow screens', (tester) async {
      useScreen(tester, narrowScreen);

      await tester.pumpWidget(
        app(
          const ScoreSyncIndicator(
            status: ScoreSyncStatus.offline,
            pendingCount: 3,
          ),
        ),
      );
      expect(find.text('3'), findsOneWidget);
      expect(find.textContaining('Hors ligne'), findsNothing);

      await tester.pumpWidget(
        app(
          const ScoreSyncIndicator(
            status: ScoreSyncStatus.upToDate,
            pendingCount: 0,
          ),
        ),
      );
      expect(find.byType(Text), findsNothing);
      expect(find.byIcon(Icons.cloud_done_outlined), findsOneWidget);
    });

    testWidgets('is announced with its full explanation', (tester) async {
      useScreen(tester, narrowScreen);
      final semantics = tester.ensureSemantics();

      await tester.pumpWidget(
        app(
          const ScoreSyncIndicator(
            status: ScoreSyncStatus.blocked,
            pendingCount: 1,
          ),
        ),
      );

      expect(
        find.bySemanticsLabel(RegExp('^Envoi bloqué. Le serveur refuse')),
        findsOneWidget,
      );
      semantics.dispose();
    });

    testWidgets('tells why the sending is blocked', (tester) async {
      useScreen(tester, narrowScreen);
      final semantics = tester.ensureSemantics();

      await tester.pumpWidget(
        app(
          const ScoreSyncIndicator(
            status: ScoreSyncStatus.blocked,
            pendingCount: 1,
            problem: ScoreSyncProblem.rejected('MATCH_NOT_IN_PROGRESS'),
          ),
        ),
      );

      expect(
        find.bySemanticsLabel(
          RegExp("^Envoi bloqué. Le match n'est pas en cours sur le serveur"),
        ),
        findsOneWidget,
      );
      semantics.dispose();
    });

    testWidgets('explains a blocked sending and sends again on a tap', (
      tester,
    ) async {
      useScreen(tester, wideScreen);
      var retries = 0;

      await tester.pumpWidget(
        app(
          ScoreSyncIndicator(
            status: ScoreSyncStatus.blocked,
            pendingCount: 2,
            problem: const ScoreSyncProblem.rejected('MATCH_NOT_IN_PROGRESS'),
            onRetry: () => retries++,
          ),
        ),
      );
      await tester.tap(find.byType(ScoreSyncIndicator));
      await tester.pumpAndSettle();

      expect(find.text('Points non envoyés'), findsOneWidget);
      expect(
        find.textContaining("Le match n'est pas en cours sur le serveur"),
        findsOneWidget,
      );
      expect(find.textContaining('Aucun point n'), findsOneWidget);
      expect(
        find.text("Code pour l'organisateur : MATCH_NOT_IN_PROGRESS"),
        findsOneWidget,
      );

      await tester.tap(find.text('Réessayer'));
      await tester.pumpAndSettle();

      expect(retries, 1);
      expect(find.text('Points non envoyés'), findsNothing);
    });

    testWidgets('closes the explanation without sending', (tester) async {
      useScreen(tester, wideScreen);

      await tester.pumpWidget(
        app(
          const ScoreSyncIndicator(
            status: ScoreSyncStatus.blocked,
            pendingCount: 2,
            problem: ScoreSyncProblem.rejected('TABLET_REVOKED'),
          ),
        ),
      );
      await tester.tap(find.byType(ScoreSyncIndicator));
      await tester.pumpAndSettle();

      expect(find.text('Réessayer'), findsNothing);
      await tester.tap(find.text('Fermer'));
      await tester.pumpAndSettle();
      expect(find.text('Points non envoyés'), findsNothing);
    });

    testWidgets('opens nothing when the sending is not blocked', (
      tester,
    ) async {
      useScreen(tester, wideScreen);

      await tester.pumpWidget(
        app(
          const ScoreSyncIndicator(
            status: ScoreSyncStatus.offline,
            pendingCount: 2,
            problem: ScoreSyncProblem.rejected('MATCH_NOT_IN_PROGRESS'),
          ),
        ),
      );
      await tester.tap(find.byType(ScoreSyncIndicator));
      await tester.pumpAndSettle();

      expect(find.byType(AlertDialog), findsNothing);
    });

    testWidgets('shows the problem of the current match first', (tester) async {
      useScreen(tester, wideScreen);
      const report = ScoreSyncReport(
        confirmedCount: 0,
        pendingCount: 3,
        reachable: true,
        problems: {
          'previous': ScoreSyncProblem.rejected('MATCH_NOT_FOUND'),
          'current': ScoreSyncProblem.rejected('MATCH_NOT_IN_PROGRESS'),
        },
      );

      await tester.pumpWidget(
        app(ScoreSyncIndicator.fromReport(report, matchId: 'current')),
      );
      expect(
        tester
            .widget<ScoreSyncIndicator>(find.byType(ScoreSyncIndicator))
            .problem,
        const ScoreSyncProblem.rejected('MATCH_NOT_IN_PROGRESS'),
      );

      await tester.pumpWidget(
        app(ScoreSyncIndicator.fromReport(report, matchId: 'other')),
      );
      expect(
        tester
            .widget<ScoreSyncIndicator>(find.byType(ScoreSyncIndicator))
            .problem,
        const ScoreSyncProblem.rejected('MATCH_NOT_FOUND'),
      );
    });
  });

  group('MatchHeader', () {
    Widget header({Widget? syncIndicator}) {
      return MatchHeader(
        tournamentName: 'Championnat de Polynésie',
        courtNumber: '3',
        category: 'Double mixte',
        ageCategory: 'Senior',
        tournamentPhase: 'Demi-finale',
        matchDuration: const Duration(minutes: 42, seconds: 7),
        onOpenActions: () {},
        syncIndicator: syncIndicator,
      );
    }

    testWidgets('shows no indicator when there is no sync', (tester) async {
      await tester.pumpWidget(app(header()));

      expect(find.byType(ScoreSyncIndicator), findsNothing);
    });

    for (final size in const [Size(640, 360), narrowScreen, wideScreen]) {
      testWidgets('fits with the indicator on a ${size.width} px wide screen', (
        tester,
      ) async {
        useScreen(tester, size);

        await tester.pumpWidget(
          app(
            header(
              syncIndicator: const ScoreSyncIndicator(
                status: ScoreSyncStatus.offline,
                pendingCount: 128,
              ),
            ),
          ),
        );

        expect(tester.takeException(), isNull);
        final isWide = size.width >= ScoreSyncIndicator.compactWidth;
        expect(
          find.text('Hors ligne · 128 en attente'),
          isWide ? findsOneWidget : findsNothing,
        );
        expect(find.text('128'), isWide ? findsNothing : findsOneWidget);
      });
    }
  });
}
