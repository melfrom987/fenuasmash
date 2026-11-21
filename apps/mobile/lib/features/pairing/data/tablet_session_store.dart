import 'dart:convert';

import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Keeps the pairing of the tablet across app restarts.
abstract interface class TabletSessionStore {
  /// The saved session, or null when the tablet is not paired (or the saved
  /// data cannot be read).
  Future<TabletSession?> load();

  Future<void> save(TabletSession session);

  Future<void> clear();
}

/// Stores the session in the encrypted storage of the device (Android
/// Keystore, iOS Keychain): the tablet token is a credential.
class SecureTabletSessionStore implements TabletSessionStore {
  SecureTabletSessionStore({FlutterSecureStorage? storage})
    : _storage = storage ?? const FlutterSecureStorage();

  static const String _key = 'tablet_session';

  final FlutterSecureStorage _storage;

  @override
  Future<TabletSession?> load() async {
    try {
      final value = await _storage.read(key: _key);
      if (value == null) {
        return null;
      }
      return TabletSession.fromJson(jsonDecode(value) as Map<String, dynamic>);
    } on Object catch (error) {
      // Unreadable data: the umpire pairs the tablet again.
      debugPrint('Could not read the tablet session: $error');
      return null;
    }
  }

  @override
  Future<void> save(TabletSession session) {
    return _storage.write(key: _key, value: jsonEncode(session.toJson()));
  }

  @override
  Future<void> clear() => _storage.delete(key: _key);
}
