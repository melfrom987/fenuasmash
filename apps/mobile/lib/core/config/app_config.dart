/// Settings chosen when the app is built, with `--dart-define`.
class AppConfig {
  const AppConfig({required this.serverUrl});

  /// Reads `API_URL`: the root URL of the server, without `/api`.
  ///
  /// Defaults to the API started by `make api`, as seen from the Android
  /// emulator (`10.0.2.2` is the computer running the emulator).
  factory AppConfig.fromEnvironment() {
    return AppConfig(serverUrl: parseServerUrl(_apiUrl));
  }

  static const _apiUrl = String.fromEnvironment(
    'API_URL',
    defaultValue: defaultServerUrl,
  );

  static const defaultServerUrl = 'http://10.0.2.2:8081';

  /// Root URL of the server, without a trailing slash.
  final Uri serverUrl;

  /// Full URL of an API endpoint, e.g. `apiUrl('/auth/me')`.
  Uri apiUrl(String path) {
    final relative = path.startsWith('/') ? path : '/$path';
    return serverUrl.replace(path: '${serverUrl.path}/api$relative');
  }

  /// Checks the URL when the app starts, so that a wrong build setting fails
  /// right away instead of on the first request.
  static Uri parseServerUrl(String value) {
    final uri = Uri.tryParse(value.trim());
    if (uri == null ||
        !(uri.isScheme('http') || uri.isScheme('https')) ||
        uri.host.isEmpty ||
        uri.hasQuery ||
        uri.hasFragment) {
      throw FormatException(
        'API_URL must be an http or https URL, such as https://fenuasmash.example',
        value,
      );
    }
    final path = uri.path.endsWith('/')
        ? uri.path.substring(0, uri.path.length - 1)
        : uri.path;
    if (path.endsWith('/api')) {
      throw FormatException(
        'API_URL is the server root: remove /api, the app adds it',
        value,
      );
    }
    return uri.replace(path: path);
  }
}
