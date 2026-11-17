import 'package:badminton_umpire/core/settings/settings.dart';
import 'package:flutter/material.dart';

import 'app/app.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await appSettingsController.load();

  runApp(const BadmintonUmpireApp());
}
