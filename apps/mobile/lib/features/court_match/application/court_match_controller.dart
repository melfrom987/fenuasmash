import 'dart:async';

import 'package:badminton_umpire/features/court_match/data/court_match_api.dart';
import 'package:badminton_umpire/features/court_match/domain/court_match.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';
import 'package:flutter/foundation.dart';

enum CourtMatchState {
  /// The tablet is not paired: there is no court to watch.
  idle,

  /// First read of the court, nothing known yet.
  loading,

  /// No match on the court.
  free,

  /// A match is called to the court, or played on it.
  match,
}

/// The match on the court of the tablet, kept up to date.
///
/// The court is read every [interval] while [start]ed and paired, until the
/// server pushes the changes in real time (#108). Without network, the last
/// known state is kept and [offline] is set.
class CourtMatchController extends ChangeNotifier {
  CourtMatchController({
    required this.pairing,
    CourtMatchApi? api,
    this.interval = const Duration(seconds: 10),
  }) : _api = api ?? HttpCourtMatchApi();

  /// The pairing that gives the court to watch.
  final TabletSessionController pairing;
  final CourtMatchApi _api;
  final Duration interval;

  CourtMatchState _state = CourtMatchState.idle;
  CourtMatch? _match;
  bool _offline = false;
  DateTime? _updatedAt;

  TabletSession? _watched;
  Timer? _timer;
  Future<void>? _refreshing;
  bool _started = false;

  CourtMatchState get state => _state;

  /// The match on the court, when [state] is [CourtMatchState.match].
  CourtMatch? get match => _match;

  /// Whether the last read failed: the state shown may be out of date.
  bool get offline => _offline;

  /// When the server last answered.
  DateTime? get updatedAt => _updatedAt;

  /// Watches the court while the tablet is paired. After a [stop], the last
  /// known match stays shown until the court is read again.
  void start() {
    if (_started) {
      return;
    }
    _started = true;
    pairing.addListener(_onPairingChanged);
    _watch(pairing.session, restart: true);
  }

  /// Stops reading the court, e.g. while the app is in the background.
  void stop() {
    if (!_started) {
      return;
    }
    _started = false;
    pairing.removeListener(_onPairingChanged);
    _timer?.cancel();
    _timer = null;
  }

  void _onPairingChanged() => _watch(pairing.session);

  void _watch(TabletSession? session, {bool restart = false}) {
    final changed = session != _watched;
    if (!changed && !restart) {
      return;
    }
    _watched = session;
    _timer?.cancel();
    _timer = null;
    if (changed) {
      _match = null;
      _offline = false;
      _updatedAt = null;
      _state = session == null ? CourtMatchState.idle : CourtMatchState.loading;
      notifyListeners();
    }
    if (session == null) {
      return;
    }
    unawaited(refresh());
    _timer = Timer.periodic(interval, (_) => unawaited(refresh()));
  }

  /// Reads the court now. Calls made while a read is running wait for it.
  Future<void> refresh() {
    return _refreshing ??= _read().whenComplete(() => _refreshing = null);
  }

  Future<void> _read() async {
    final session = pairing.session;
    if (session == null) {
      return;
    }
    final result = await _api.fetch(session, known: _match);
    if (pairing.session != session) {
      // Paired again or unpaired meanwhile
      return;
    }
    switch (result) {
      case CourtMatchFound(:final match):
        _show(CourtMatchState.match, match);
      case CourtFree():
        _show(CourtMatchState.free, null);
      case CourtMatchRevoked():
        // The pairing decides: it asks the server, then clears the session
        await pairing.verify();
      case CourtMatchUnreachable():
        if (!_offline) {
          _offline = true;
          notifyListeners();
        }
    }
  }

  void _show(CourtMatchState state, CourtMatch? match) {
    final changed = state != _state || match != _match || _offline;
    _state = state;
    _match = match;
    _offline = false;
    _updatedAt = DateTime.now();
    if (changed) {
      notifyListeners();
    }
  }

  @override
  void dispose() {
    stop();
    super.dispose();
  }
}

/// The match on the court of this tablet, for the whole app.
final courtMatchController = CourtMatchController(
  pairing: tabletSessionController,
);
