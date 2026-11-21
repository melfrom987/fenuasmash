import 'dart:async';

import 'package:badminton_umpire/features/pairing/data/tablet_pairing_api.dart';
import 'package:badminton_umpire/features/pairing/data/tablet_session_store.dart';
import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';
import 'package:flutter/foundation.dart';

enum TabletPairingState {
  /// The saved session has not been read yet.
  loading,

  /// No pairing: the umpire can still score matches locally.
  unpaired,

  /// Paired with a court: points can be sent to the server.
  paired,

  /// The organizer revoked the tablet: a new code is needed.
  revoked,
}

/// The pairing of the tablet with a court, kept across restarts.
///
/// At startup, the saved session is used at once, even without network: the
/// umpire must be able to score offline. It is then checked with the server
/// in the background; only a server that says the tablet is revoked ends it.
class TabletSessionController extends ChangeNotifier {
  TabletSessionController({TabletSessionStore? store, TabletPairingApi? api})
    : _store = store ?? SecureTabletSessionStore(),
      _api = api ?? HttpTabletPairingApi();

  final TabletSessionStore _store;
  final TabletPairingApi _api;

  TabletPairingState _state = TabletPairingState.loading;
  TabletSession? _session;

  TabletPairingState get state => _state;

  /// The current pairing, or null when the tablet is not paired.
  TabletSession? get session => _session;

  /// The tablet token, for the requests sent to the server.
  Future<String?> accessToken() async => _session?.accessToken;

  /// Reads the saved session. Does not use the network.
  Future<void> load() async {
    _session = await _store.load();
    _state = _session == null
        ? TabletPairingState.unpaired
        : TabletPairingState.paired;
    notifyListeners();
  }

  /// Asks the server whether the saved session still works.
  Future<void> verify() async {
    final session = _session;
    if (session == null) {
      return;
    }
    final result = await _api.check(session);
    if (_session != session) {
      // Paired again or unpaired meanwhile
      return;
    }
    switch (result.status) {
      case TabletCheck.revoked:
        await _store.clear();
        _session = null;
        _state = TabletPairingState.revoked;
        notifyListeners();
      case TabletCheck.paired:
        final number = result.courtNumber;
        if (number != null && number != session.courtNumber) {
          _session = session.withCourtNumber(number);
          await _store.save(_session!);
          notifyListeners();
        }
      case TabletCheck.unknown:
        break;
    }
  }

  /// Pairs the tablet with the court of the code. The previous pairing, if
  /// any, is replaced only when the new one succeeds.
  Future<PairingResult> pair(
    Uri apiBaseUri,
    String code, {
    String? deviceName,
  }) async {
    final name = deviceName?.trim();
    final result = await _api.pair(
      apiBaseUri,
      normalizePairingCode(code),
      deviceName: name == null || name.isEmpty ? null : name,
    );
    if (result is Paired) {
      await _store.save(result.session);
      _session = result.session;
      _state = TabletPairingState.paired;
      notifyListeners();
    }
    return result;
  }

  /// Forgets the pairing on this tablet. The organizer still sees it until
  /// they revoke it.
  Future<void> unpair() async {
    await _store.clear();
    _session = null;
    _state = TabletPairingState.unpaired;
    notifyListeners();
  }
}

/// The pairing of this tablet, for the whole app.
final tabletSessionController = TabletSessionController();
