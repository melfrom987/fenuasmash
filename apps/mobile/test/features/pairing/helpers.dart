import 'package:badminton_umpire/features/pairing/data/tablet_pairing_api.dart';
import 'package:badminton_umpire/features/pairing/data/tablet_session_store.dart';
import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';

TabletSession sessionOnCourt(int number, {String token = 'fst_secret'}) {
  return TabletSession(
    apiBaseUri: Uri.parse('http://10.0.0.5:8081/api'),
    accessToken: token,
    tabletId: 'tablet-1',
    courtId: 'court-$number',
    courtNumber: number,
    tournamentId: 'tournament-1',
    pairedAt: DateTime.utc(2026, 11, 21, 8),
  );
}

extension SessionBase on TabletSession {
  TabletSession copyWithBase(Uri base) => TabletSession(
    apiBaseUri: base,
    accessToken: accessToken,
    tabletId: tabletId,
    courtId: courtId,
    courtNumber: courtNumber,
    tournamentId: tournamentId,
    pairedAt: pairedAt,
  );
}

/// Keeps the session in memory, as the device storage would across restarts.
class MemorySessionStore implements TabletSessionStore {
  TabletSession? saved;

  @override
  Future<TabletSession?> load() async => saved;

  @override
  Future<void> save(TabletSession session) async => saved = session;

  @override
  Future<void> clear() async => saved = null;
}

class FakePairingApi implements TabletPairingApi {
  PairingResult pairResult = const PairingServerUnreachable();
  TabletCheckResult checkResult = (
    status: TabletCheck.unknown,
    courtNumber: null,
  );
  final List<({Uri uri, String code, String? deviceName})> pairCalls = [];
  int checkCount = 0;

  @override
  Future<PairingResult> pair(
    Uri apiBaseUri,
    String code, {
    String? deviceName,
  }) async {
    pairCalls.add((uri: apiBaseUri, code: code, deviceName: deviceName));
    return pairResult;
  }

  @override
  Future<TabletCheckResult> check(TabletSession session) async {
    checkCount++;
    return checkResult;
  }
}
