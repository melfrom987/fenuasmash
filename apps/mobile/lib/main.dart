import 'package:flutter/material.dart';

import 'app/app.dart';
import 'app/licenses.dart';
import 'core/config/app_config.dart';

void main() {
  final config = AppConfig.fromEnvironment();
  registerFontLicenses();
  runApp(FenuaSmashApp(config: config));
}
