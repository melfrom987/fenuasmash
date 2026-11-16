import 'package:flutter/material.dart';

import 'app/app.dart';
import 'core/config/app_config.dart';

void main() {
  final config = AppConfig.fromEnvironment();
  runApp(FenuaSmashApp(config: config));
}
