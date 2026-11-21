import 'package:badminton_umpire/features/home/presentation/home_page.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:badminton_umpire/features/pairing/data/tablet_pairing_api.dart';
import 'package:badminton_umpire/features/pairing/presentation/pairing_page.dart';
import 'package:badminton_umpire/features/pairing/presentation/tablet_pairing_card.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'helpers.dart';

void main() {
  late MemorySessionStore store;
  late FakePairingApi api;
  late TabletSessionController controller;

  setUp(() async {
    store = MemorySessionStore();
    api = FakePairingApi();
    controller = TabletSessionController(store: store, api: api);
    await controller.load();
  });

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

  Widget card() => app(
    Scaffold(
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: TabletPairingCard(controller: controller),
      ),
    ),
  );

  Future<void> openPairing(WidgetTester tester) async {
    useScreen(tester, const Size(1280, 800));
    await tester.pumpWidget(card());
    await tester.tap(find.byKey(const Key('tabletPairingCard')));
    await tester.pumpAndSettle();
  }

  Future<void> fill(
    WidgetTester tester, {
    required String code,
    String server = 'http://10.0.0.5:8081',
  }) async {
    await tester.enterText(find.byKey(const Key('pairingCode')), code);
    await tester.enterText(find.byKey(const Key('pairingServer')), server);
    await tester.tap(find.text('Associer'));
    await tester.pumpAndSettle();
  }

  group('PairingPage', () {
    testWidgets('pairs the tablet and goes back to the home card', (
      tester,
    ) async {
      api.pairResult = Paired(sessionOnCourt(3));
      await openPairing(tester);

      await fill(tester, code: 'k7m-p4x');

      expect(api.pairCalls.single.code, 'K7MP4X');
      expect(api.pairCalls.single.uri, Uri.parse('http://10.0.0.5:8081/api'));
      expect(find.byType(PairingPage), findsNothing);
      expect(find.text('Terrain 3'), findsOneWidget);
      expect(find.text('Tablette associée au terrain 3.'), findsOneWidget);
    });

    testWidgets('checks the code and the address before sending', (
      tester,
    ) async {
      await openPairing(tester);

      await fill(tester, code: 'K7M', server: 'ftp://server');

      expect(find.text('Le code fait 6 caractères.'), findsOneWidget);
      expect(
        find.text('Adresse invalide, par exemple http://192.168.1.10:8081.'),
        findsOneWidget,
      );
      expect(api.pairCalls, isEmpty);
    });

    final errors = <PairingResult, String>{
      const InvalidPairingCode():
          'Code incorrect, expiré ou déjà utilisé. Demande un nouveau code '
          'à l’organisateur.',
      const TooManyPairingAttempts(Duration(seconds: 610)):
          'Trop de codes incorrects. Réessaie dans 11 min.',
      const PairingCourtInactive():
          'Ce terrain est désactivé. Demande à l’organisateur de le réactiver.',
      const PairingServerUnreachable():
          'Serveur injoignable. Vérifie le Wi-Fi et l’adresse du serveur.',
      const PairingFailed('FORBIDDEN'): 'Association impossible (FORBIDDEN).',
    };
    for (final MapEntry(key: result, value: message) in errors.entries) {
      testWidgets('explains ${result.runtimeType}', (tester) async {
        api.pairResult = result;
        await openPairing(tester);

        await fill(tester, code: 'K7MP4X');

        expect(find.byType(PairingPage), findsOneWidget);
        expect(find.byKey(const Key('pairingError')), findsOneWidget);
        expect(find.text(message), findsOneWidget);
      });
    }
  });

  group('TabletPairingCard', () {
    testWidgets('shows each state', (tester) async {
      useScreen(tester, const Size(1280, 800));
      await tester.pumpWidget(card());
      expect(find.text('Associer à un terrain'), findsOneWidget);

      api.pairResult = Paired(sessionOnCourt(2));
      await controller.pair(Uri.parse('http://x/api'), 'K7MP4X');
      await tester.pump();
      expect(find.text('Terrain 2'), findsOneWidget);

      api.checkResult = (status: TabletCheck.revoked, courtNumber: null);
      await controller.verify();
      await tester.pump();
      expect(find.text('Association retirée'), findsOneWidget);
    });

    testWidgets('unpairs after confirmation', (tester) async {
      store.saved = sessionOnCourt(2);
      await controller.load();
      useScreen(tester, const Size(1280, 800));
      await tester.pumpWidget(card());

      await tester.tap(find.byKey(const Key('tabletPairingMenu')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Dissocier').last);
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Dissocier'));
      await tester.pumpAndSettle();

      expect(controller.state, TabletPairingState.unpaired);
      expect(find.text('Associer à un terrain'), findsOneWidget);
    });
  });

  group('HomePage', () {
    for (final size in const [
      Size(1280, 800),
      Size(800, 360),
      Size(360, 740),
    ]) {
      testWidgets('fits with the pairing card on $size', (tester) async {
        useScreen(tester, size);

        await tester.pumpWidget(app(HomePage(pairing: controller)));
        await tester.pumpAndSettle();

        expect(find.byType(TabletPairingCard), findsOneWidget);
        expect(tester.takeException(), isNull);
      });
    }
  });
}
