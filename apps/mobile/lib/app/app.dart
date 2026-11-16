import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../core/config/app_config.dart';
import 'router.dart';
import 'theme/app_theme.dart';

/// FenuaSmash umpire app: runs on the tablet of a court to enter the scores.
class FenuaSmashApp extends StatefulWidget {
  const FenuaSmashApp({super.key, required this.config});

  final AppConfig config;

  @override
  State<FenuaSmashApp> createState() => _FenuaSmashAppState();
}

class _FenuaSmashAppState extends State<FenuaSmashApp> {
  late final GoRouter _router = createRouter();

  @override
  void dispose() {
    _router.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'FenuaSmash',
      theme: AppTheme.light(),
      routerConfig: _router,
    );
  }
}
