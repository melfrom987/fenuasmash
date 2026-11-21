import 'package:badminton_umpire/features/pairing/data/tablet_session_store.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';

import 'helpers.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('keeps the session until it is cleared', () async {
    FlutterSecureStorage.setMockInitialValues({});
    final store = SecureTabletSessionStore();

    expect(await store.load(), isNull);
    await store.save(sessionOnCourt(4));
    // A new store, as after a restart
    expect(await SecureTabletSessionStore().load(), sessionOnCourt(4));

    await store.clear();
    expect(await store.load(), isNull);
  });

  test('unreadable data counts as not paired', () async {
    FlutterSecureStorage.setMockInitialValues({'tablet_session': '{broken'});

    expect(await SecureTabletSessionStore().load(), isNull);
  });
}
