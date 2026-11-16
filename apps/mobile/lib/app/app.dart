import 'package:flutter/material.dart';

import '../core/config/app_config.dart';
import '../features/pairing/screens/pairing_screen.dart';
import 'theme/app_theme.dart';

/// FenuaSmash umpire app: runs on the tablet of a court to enter the scores.
class FenuaSmashApp extends StatelessWidget {
  const FenuaSmashApp({super.key, required this.config});

  final AppConfig config;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'FenuaSmash',
      theme: AppTheme.light(),
      home: const PairingScreen(),
    );
  }
}
