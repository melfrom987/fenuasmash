import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:badminton_umpire/features/pairing/data/tablet_pairing_api.dart';
import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// API address given at build time (`--dart-define=API_URL=...`, see
/// `.github/workflows/cd.yml`); the umpire can change it on this page.
const String defaultApiUrl = String.fromEnvironment('API_URL');

/// Pairs the tablet with a court: the umpire types the code shown by the
/// organizer on the page of the court. Pops `true` once paired.
class PairingPage extends StatefulWidget {
  const PairingPage({super.key, required this.controller});

  final TabletSessionController controller;

  @override
  State<PairingPage> createState() => _PairingPageState();
}

class _PairingPageState extends State<PairingPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _server;
  final TextEditingController _code = TextEditingController();
  final TextEditingController _deviceName = TextEditingController();

  bool _sending = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    final previous = widget.controller.session?.apiBaseUri;
    _server = TextEditingController(
      text: previous == null ? defaultApiUrl : _withoutApiSegment(previous),
    );
  }

  @override
  void dispose() {
    _server.dispose();
    _code.dispose();
    _deviceName.dispose();
    super.dispose();
  }

  /// Shows `http://10.0.0.5:8081` rather than `http://10.0.0.5:8081/api`.
  static String _withoutApiSegment(Uri uri) {
    final segments = [...uri.pathSegments];
    if (segments.isNotEmpty && segments.last == 'api') {
      segments.removeLast();
    }
    return uri.replace(pathSegments: segments).toString();
  }

  Future<void> _submit() async {
    if (_sending || !_formKey.currentState!.validate()) {
      return;
    }
    setState(() {
      _sending = true;
      _error = null;
    });
    final result = await widget.controller.pair(
      parseApiBaseUri(_server.text)!,
      _code.text,
      deviceName: _deviceName.text,
    );
    if (!mounted) {
      return;
    }
    if (result is Paired) {
      Navigator.of(context).pop(true);
      return;
    }
    setState(() {
      _sending = false;
      _error = _messageOf(result, AppLocalizations.of(context)!);
    });
  }

  static String _messageOf(PairingResult result, AppLocalizations l10n) {
    return switch (result) {
      Paired() => '',
      InvalidPairingCode() => l10n.pairingInvalidCode,
      TooManyPairingAttempts(:final retryAfter) => l10n.pairingTooManyAttempts(
        (retryAfter.inSeconds / 60).ceil().clamp(1, 60),
      ),
      PairingCourtInactive() => l10n.pairingCourtInactive,
      PairingServerUnreachable() => l10n.pairingUnreachable,
      PairingFailed(:final code) => l10n.pairingFailed(code),
    };
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.pairingTitle)),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 520),
            child: Form(
              key: _formKey,
              child: ListView(
                padding: EdgeInsets.all(responsive.paddingXL),
                children: [
                  Text(
                    l10n.pairingDescription,
                    style: TextStyle(fontSize: responsive.textM),
                  ),
                  SizedBox(height: responsive.spaceL),
                  TextFormField(
                    key: const Key('pairingCode'),
                    controller: _code,
                    autofocus: true,
                    enabled: !_sending,
                    textCapitalization: TextCapitalization.characters,
                    autocorrect: false,
                    enableSuggestions: false,
                    textInputAction: TextInputAction.done,
                    inputFormatters: [LengthLimitingTextInputFormatter(20)],
                    style: TextStyle(
                      fontSize: responsive.titleM,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 6,
                    ),
                    decoration: InputDecoration(
                      labelText: l10n.pairingCode,
                      hintText: 'K7MP4X',
                      border: const OutlineInputBorder(),
                    ),
                    validator: (value) =>
                        normalizePairingCode(value ?? '').length ==
                            pairingCodeLength
                        ? null
                        : l10n.pairingCodeLength,
                    onFieldSubmitted: (_) => _submit(),
                  ),
                  SizedBox(height: responsive.spaceM),
                  TextFormField(
                    key: const Key('pairingServer'),
                    controller: _server,
                    enabled: !_sending,
                    keyboardType: TextInputType.url,
                    autocorrect: false,
                    decoration: InputDecoration(
                      labelText: l10n.pairingServer,
                      hintText: 'http://192.168.1.10:8081',
                      border: const OutlineInputBorder(),
                    ),
                    validator: (value) => parseApiBaseUri(value ?? '') == null
                        ? l10n.pairingServerInvalid
                        : null,
                  ),
                  SizedBox(height: responsive.spaceM),
                  TextFormField(
                    key: const Key('pairingDeviceName'),
                    controller: _deviceName,
                    enabled: !_sending,
                    inputFormatters: [LengthLimitingTextInputFormatter(100)],
                    decoration: InputDecoration(
                      labelText: l10n.pairingDeviceName,
                      border: const OutlineInputBorder(),
                    ),
                  ),
                  if (_error != null) ...[
                    SizedBox(height: responsive.spaceM),
                    Text(
                      _error!,
                      key: const Key('pairingError'),
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.error,
                        fontSize: responsive.textM,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                  SizedBox(height: responsive.spaceL),
                  FilledButton.icon(
                    onPressed: _sending ? null : _submit,
                    icon: _sending
                        ? const SizedBox.square(
                            dimension: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.link),
                    label: Text(l10n.pairingSubmit),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
