import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';
import 'package:flutter_test/flutter_test.dart';

import 'helpers.dart';

void main() {
  group('parseApiBaseUri', () {
    test('adds /api to the server address', () {
      expect(
        parseApiBaseUri('http://192.168.1.10:8081'),
        Uri.parse('http://192.168.1.10:8081/api'),
      );
      expect(
        parseApiBaseUri(' https://fenuasmash.pf/ '),
        Uri.parse('https://fenuasmash.pf/api'),
      );
    });

    test('keeps an address that already ends with /api', () {
      expect(
        parseApiBaseUri('https://fenuasmash.pf/api/'),
        Uri.parse('https://fenuasmash.pf/api'),
      );
    });

    test('takes an address without scheme as http', () {
      expect(
        parseApiBaseUri('10.0.0.5:8081'),
        Uri.parse('http://10.0.0.5:8081/api'),
      );
    });

    test('refuses what is not an http address', () {
      for (final input in ['', '   ', 'ftp://server', 'http://', '::']) {
        expect(parseApiBaseUri(input), isNull, reason: input);
      }
    });
  });

  test('normalizePairingCode removes spaces and dashes, in capitals', () {
    expect(normalizePairingCode(' k7m-p4x '), 'K7MP4X');
    expect(normalizePairingCode('K7M P4X'), 'K7MP4X');
  });

  test('a session is read back as saved', () {
    final session = sessionOnCourt(3);

    expect(TabletSession.fromJson(session.toJson()), session);
  });
}
