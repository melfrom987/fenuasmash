import 'package:badminton_umpire/features/court_match/application/court_match_controller.dart';
import 'package:badminton_umpire/features/court_match/data/court_match_api.dart';
import 'package:badminton_umpire/features/court_match/domain/court_match.dart';
import 'package:badminton_umpire/features/court_match/presentation/court_match_card.dart';
import 'package:badminton_umpire/features/home/presentation/home_page.dart';
import 'package:badminton_umpire/features/match_setup/presentation/pages/court_setup_page.dart';
import 'package:badminton_umpire/features/match_setup/presentation/pages/match_setup_page.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../pairing/helpers.dart';
import 'helpers.dart';

void main() {
  late MemorySessionStore store;
  late TabletSessionController pairing;
  late FakeCourtMatchApi api;
  late CourtMatchController controller;

  setUp(() async {
    store = MemorySessionStore()..saved = sessionOnCourt(3);
    pairing = TabletSessionController(store: store, api: FakePairingApi());
    await pairing.load();
    api = FakeCourtMatchApi();
    controller = CourtMatchController(pairing: pairing, api: api);
  });

  tearDown(() => controller.dispose());

  void useScreen(WidgetTester tester, Size size) {
    tester.view.physicalSize = size;
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
  }

  Widget app(Widget home) {
    return MaterialApp(
      locale: const Locale('fr'),
      localizationsDelegates: AppLocalizations.localizationsDelegates,
      supportedLocales: AppLocalizations.supportedLocales,
      home: home,
    );
  }

  Widget card({
    ValueChanged<CourtMatch>? onOpen,
    bool Function(CourtMatch)? isResumable,
  }) {
    return app(
      Scaffold(
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: CourtMatchCard(
            controller: controller,
            onOpen: onOpen ?? (_) {},
            isResumable: isResumable,
          ),
        ),
      ),
    );
  }

  group('CourtMatchCard', () {
    testWidgets('shows nothing when the tablet is not paired', (tester) async {
      await pairing.unpair();

      await tester.pumpWidget(card());
      await tester.pump();

      expect(find.byKey(const Key('courtMatchCard')), findsNothing);
      expect(api.calls, isEmpty);
    });

    testWidgets('says when the court is free', (tester) async {
      await tester.pumpWidget(card());
      await tester.pump();

      expect(find.text('Aucun match sur le terrain'), findsOneWidget);
      expect(
        find.text(
          'Le match appelé par l’organisateur s’affichera ici tout seul.',
        ),
        findsOneWidget,
      );
    });

    testWidgets('a called match appears by itself and opens', (tester) async {
      CourtMatch? opened;
      await tester.pumpWidget(card(onOpen: (match) => opened = match));
      await tester.pump();
      expect(find.text('Aucun match sur le terrain'), findsOneWidget);

      api.next = CourtMatchFound(calledMatch());
      await tester.pump(controller.interval);

      expect(find.text('Match appelé'), findsOneWidget);
      expect(find.text('Double mixte Senior · Tour 2'), findsOneWidget);
      expect(
        find.text('Touche pour saisir les joueurs et commencer.'),
        findsOneWidget,
      );

      await tester.tap(find.byKey(const Key('courtMatchCard')));
      expect(opened, calledMatch());
    });

    testWidgets('shows the status and the score known by the server', (
      tester,
    ) async {
      api.next = CourtMatchFound(
        CourtMatch.fromJson(
          matchJson(
            status: 'IN_PROGRESS',
            round: null,
            games: [
              {'a': 21, 'b': 18},
              {'a': 3, 'b': 5},
            ],
          ),
        ),
      );

      await tester.pumpWidget(card(isResumable: (_) => true));
      await tester.pump();

      expect(find.text('Match en cours'), findsOneWidget);
      expect(find.text('Match du tournoi · 21-18 3-5'), findsOneWidget);
      expect(find.text('Touche pour reprendre le match.'), findsOneWidget);
    });

    testWidgets('without network, says the match may be out of date', (
      tester,
    ) async {
      api.next = CourtMatchFound(calledMatch());
      await tester.pumpWidget(card());
      await tester.pump();
      expect(find.byKey(const Key('courtMatchOffline')), findsNothing);

      api.next = const CourtMatchUnreachable();
      await tester.pump(controller.interval);

      expect(find.text('Match appelé'), findsOneWidget);
      final offline = tester.widget<Text>(
        find.byKey(const Key('courtMatchOffline')),
      );
      expect(offline.data, startsWith('Hors ligne : affiché tel qu’à '));
    });

    testWidgets('stops reading the court once closed', (tester) async {
      await tester.pumpWidget(card());
      await tester.pump();
      await tester.pumpWidget(app(const SizedBox()));
      final calls = api.calls.length;

      await tester.pump(controller.interval * 3);

      expect(api.calls, hasLength(calls));
    });
  });

  group('MatchSetupPage', () {
    Future<void> openSetup(WidgetTester tester, CourtMatch match) async {
      useScreen(tester, const Size(1280, 800));
      await tester.pumpWidget(
        app(MatchSetupPage(courtMatch: match, courtNumber: 3)),
      );
      await tester.pumpAndSettle();
    }

    Finder field(String label) => find.byWidgetPredicate(
      (widget) => widget is TextField && widget.decoration?.labelText == label,
    );

    String text(WidgetTester tester, Finder finder) =>
        tester.widget<TextField>(finder).controller!.text;

    testWidgets('is filled in with what the server knows', (tester) async {
      await openSetup(tester, calledMatch());

      expect(text(tester, field('Nom du tournoi')), 'Open de Papeete');
      expect(text(tester, field('Numéro du terrain *')), '3');
      final texts = tester
          .widgetList<TextField>(find.byType(TextField))
          .map((field) => field.controller?.text);
      expect(texts, containsAll(['Senior', 'Tour 2']));
      // Mixed doubles: two players per side, typed by the umpire
      expect(field('Joueur 2 *'), findsNWidgets(2));
      expect(text(tester, field('Joueur 1 *').first), isEmpty);
    });

    testWidgets('a match started from the court keeps its server id', (
      tester,
    ) async {
      await openSetup(tester, calledMatch(discipline: 'MS'));
      expect(field('Joueur 2 *'), findsNothing);

      await tester.enterText(field('Joueur 1 *').at(0), 'Teva');
      await tester.enterText(field('Joueur 1 *').at(1), 'Hina');
      await tester.enterText(field('Nom de l’arbitre *'), 'Moana');
      await tester.pump();
      final start = find.byType(FilledButton).last;
      await tester.ensureVisible(start);
      await tester.tap(start);
      await tester.pumpAndSettle();

      final setup = tester.widget<CourtSetupPage>(find.byType(CourtSetupPage));
      expect(setup.config.serverMatchId, matchId);
      expect(setup.config.courtNumber, '3');
      expect(setup.config.isDoubles, isFalse);
      expect(setup.config.tournamentName, 'Open de Papeete');
      expect(setup.config.teamAPlayers, ['Teva']);
    });

    testWidgets('a match created on the tablet has no server id', (
      tester,
    ) async {
      useScreen(tester, const Size(1280, 800));
      await tester.pumpWidget(app(const MatchSetupPage()));
      await tester.pumpAndSettle();

      expect(text(tester, field('Nom du tournoi')), isEmpty);
      expect(text(tester, field('Numéro du terrain *')), isEmpty);
    });
  });

  group('HomePage', () {
    for (final size in const [
      Size(1280, 800),
      Size(800, 360),
      Size(360, 740),
    ]) {
      testWidgets('shows the match of the court at $size', (tester) async {
        useScreen(tester, size);
        api.next = CourtMatchFound(calledMatch());

        await tester.pumpWidget(
          app(HomePage(pairing: pairing, courtMatch: controller)),
        );
        await tester.pump();
        await tester.pump();

        expect(find.text('Match appelé'), findsOneWidget);
        expect(tester.takeException(), isNull);
      });
    }

    testWidgets('opening the match fills in the setup screen', (tester) async {
      useScreen(tester, const Size(1280, 800));
      api.next = CourtMatchFound(calledMatch());
      await tester.pumpWidget(
        app(HomePage(pairing: pairing, courtMatch: controller)),
      );
      await tester.pump();
      await tester.pump();

      await tester.tap(find.byKey(const Key('courtMatchCard')));
      await tester.pumpAndSettle();

      final setup = tester.widget<MatchSetupPage>(find.byType(MatchSetupPage));
      expect(setup.courtMatch, calledMatch());
      expect(setup.courtNumber, 3);
    });
  });
}
