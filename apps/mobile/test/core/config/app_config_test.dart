import 'package:fenuasmash/core/config/app_config.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('parseServerUrl', () {
    test('accepts http and https URLs', () {
      expect(
        AppConfig.parseServerUrl('https://fenuasmash.example'),
        Uri.parse('https://fenuasmash.example'),
      );
      expect(
        AppConfig.parseServerUrl('http://10.0.2.2:8081'),
        Uri.parse('http://10.0.2.2:8081'),
      );
    });

    test('removes the trailing slash and surrounding spaces', () {
      expect(
        AppConfig.parseServerUrl(' https://fenuasmash.example/ '),
        Uri.parse('https://fenuasmash.example'),
      );
    });

    test('keeps a path prefix', () {
      expect(
        AppConfig.parseServerUrl('https://example.com/fenuasmash/'),
        Uri.parse('https://example.com/fenuasmash'),
      );
    });

    test('rejects malformed URLs', () {
      for (final value in [
        '',
        'fenuasmash.example',
        'ftp://fenuasmash.example',
        'https://',
        'https://fenuasmash.example?x=1',
      ]) {
        expect(
          () => AppConfig.parseServerUrl(value),
          throwsFormatException,
          reason: value,
        );
      }
    });

    test('rejects a URL that already ends with /api', () {
      expect(
        () => AppConfig.parseServerUrl('https://fenuasmash.example/api/'),
        throwsFormatException,
      );
    });
  });

  group('apiUrl', () {
    test('adds /api before the endpoint path', () {
      final config = AppConfig(
        serverUrl: AppConfig.parseServerUrl('https://fenuasmash.example'),
      );

      expect(
        config.apiUrl('/auth/me'),
        Uri.parse('https://fenuasmash.example/api/auth/me'),
      );
      expect(
        config.apiUrl('courts/3'),
        Uri.parse('https://fenuasmash.example/api/courts/3'),
      );
    });

    test('keeps the server path prefix', () {
      final config = AppConfig(
        serverUrl: AppConfig.parseServerUrl('http://example.com/fenuasmash'),
      );

      expect(
        config.apiUrl('/auth/me'),
        Uri.parse('http://example.com/fenuasmash/api/auth/me'),
      );
    });
  });

  test('defaults to the local API seen from the Android emulator', () {
    expect(
      AppConfig.fromEnvironment().serverUrl,
      Uri.parse(AppConfig.defaultServerUrl),
    );
  });
}
