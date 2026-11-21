import 'dart:async';

import 'package:badminton_umpire/core/settings/settings.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:flutter/material.dart';

import 'app/app.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await appSettingsController.load();
  // The saved pairing is used at once, then checked with the server
  await tabletSessionController.load();
  unawaited(tabletSessionController.verify());

  runApp(const BadmintonUmpireApp());
}
