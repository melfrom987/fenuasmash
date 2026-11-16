import 'package:flutter/material.dart';

import '../core/config/app_config.dart';
import '../features/pairing/screens/pairing_screen.dart';

/// FenuaSmash umpire app: runs on the tablet of a court to enter the scores.
class FenuaSmashApp extends StatelessWidget {
  const FenuaSmashApp({super.key, required this.config});

  final AppConfig config;

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(title: 'FenuaSmash', home: PairingScreen());
  }
}
