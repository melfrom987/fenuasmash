import 'dart:io';

import 'package:badminton_umpire/features/match/data/services/score_event_api.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_outbox.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_sync.dart';
import 'package:badminton_umpire/features/match/domain/models/match_draft.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:badminton_umpire/features/match/presentation/pages/match_page.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/header/score_sync_indicator.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/layouts/match_layout.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../helpers/match_test_helpers.dart';

const _serverMatchId = '9b2f6a52-7c1e-4d5e-8a3b-1f0c2d4e6a8b';

/// The queue in memory: the page runs under fake time, which does not wait
/// for files. The queue on disk is covered by `score_event_outbox_test.dart`.
class _MemoryOutbox extends ScoreEventOutbox {
  final Map<String, List<ScoreEvent>> queued = {};

  @override
  Future<void> enqueue(String matchId, Iterable<ScoreEvent> events) async {
    final queue = queued.putIfAbsent(matchId, () => []);
    final ids = queue.map((event) => event.id).toSet();
    queue.addAll(events.where((event) => !ids.contains(event.id)));
  }
}

/// Counts the syncs the page asks for. Sending itself is covered by the
/// tests of [ScoreEventSync] and `offline_sync_test.dart`.
class _CountingSync extends ScoreEventSync {
  _CountingSync(ScoreEventOutbox outbox) : super(outbox: outbox, api: _NoApi());

  int count = 0;

  @override
  Future<ScoreSyncReport> syncNow() async {
    count++;
    return const ScoreSyncReport(
      confirmedCount: 0,
      pendingCount: 0,
      reachable: true,
    );
  }
}

class _NoApi implements ScoreEventApi {
  @override
  Future<ScoreEventSendResult> send(String matchId, List<ScoreEvent> events) {
    throw UnimplementedError();
  }
}

void main() {
  late Directory directory;
  late _MemoryOutbox outbox;
  late _CountingSync sync;
  late ScoreEventSyncScheduler scheduler;

  const pathProvider = MethodChannel('plugins.flutter.io/path_provider');

  setUp(() async {
    // The page also saves the current match and the history
    directory = await Directory.systemTemp.createTemp('match_page_test');
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(pathProvider, (_) async => directory.path);
    outbox = _MemoryOutbox();
    sync = _CountingSync(outbox);
    scheduler = ScoreEventSyncScheduler(sync);
  });

  tearDown(() async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(pathProvider, null);
    scheduler.dispose();
    await directory.delete(recursive: true);
  });

  /// A started match, as resumed from the home page.
  MatchDraft startedMatch({String? serverMatchId}) {
    final json = draftOf(
      createSinglesController(),
      savedAt: DateTime.now(),
    ).toJson();
    (json['config'] as Map<String, dynamic>)['serverMatchId'] = serverMatchId;
    return MatchDraft.fromJson(json);
  }

  Future<void> openMatch(WidgetTester tester, MatchDraft draft) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);

    await tester.pumpWidget(
      MaterialApp(
        locale: const Locale('fr'),
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        supportedLocales: AppLocalizations.supportedLocales,
        home: MatchPage(
          config: draft.config,
          serverName: draft.serverName,
          receiverName: draft.receiverName,
          leftSideTeam: draft.leftSideTeamId,
          draft: draft,
          scoreEventOutbox: outbox,
          scoreSync: scheduler,
        ),
      ),
    );
    await tester.pump();
  }

  /// Scores like the umpire, then lets the page save its match: the current
  /// match is written to a real file before its points are queued.
  Future<void> scorePoint(WidgetTester tester) async {
    tester.widget<MatchLayout>(find.byType(MatchLayout)).onLeftPoint();
    await tester.pump(const Duration(milliseconds: 500));
    for (var i = 0; i < 10; i++) {
      await tester.runAsync(
        () => Future<void>.delayed(const Duration(milliseconds: 20)),
      );
      await tester.pump();
    }
  }

  Future<void> closeMatch(WidgetTester tester) async {
    await tester.pumpWidget(const SizedBox());
    await tester.pump(const Duration(seconds: 1));
  }

  testWidgets('queues and sends the points of a match called to the court', (
    tester,
  ) async {
    await openMatch(tester, startedMatch(serverMatchId: _serverMatchId));

    await scorePoint(tester);
    await scorePoint(tester);

    expect(outbox.queued.keys, [_serverMatchId]);
    expect(outbox.queued[_serverMatchId], hasLength(2));
    expect(sync.count, greaterThan(0));
    expect(
      tester.widget<ScoreSyncIndicator>(find.byType(ScoreSyncIndicator)).status,
      ScoreSyncStatus.upToDate,
    );

    await closeMatch(tester);
  });

  testWidgets('sends nothing for a match created on the tablet', (
    tester,
  ) async {
    await openMatch(tester, startedMatch());

    await scorePoint(tester);

    expect(outbox.queued, isEmpty);
    expect(sync.count, 0);
    expect(find.byType(ScoreSyncIndicator), findsNothing);

    await closeMatch(tester);
  });
}
