import 'package:flutter/services.dart';

class AppHaptics {
  AppHaptics._();

  static Future<void> light() async {
    await HapticFeedback.lightImpact();
  }

  static Future<void> medium() async {
    await HapticFeedback.mediumImpact();
  }

  static Future<void> heavy() async {
    await HapticFeedback.heavyImpact();
  }

  static Future<void> alert() async {
    await HapticFeedback.heavyImpact();
    await Future.delayed(const Duration(milliseconds: 120));
    await HapticFeedback.heavyImpact();
    await Future.delayed(const Duration(milliseconds: 120));
    await HapticFeedback.heavyImpact();
  }

  static Future<void> urgent() async {
    for (var i = 0; i < 5; i++) {
      await HapticFeedback.heavyImpact();
      await Future.delayed(const Duration(milliseconds: 150));
    }
  }

  static Future<void> countdownWarning() async {
    for (var i = 0; i < 5; i++) {
      await HapticFeedback.heavyImpact();
      await Future.delayed(const Duration(milliseconds: 220));
    }
  }

  static Future<void> countdownFinished() async {
    for (var i = 0; i < 8; i++) {
      await HapticFeedback.heavyImpact();
      await Future.delayed(const Duration(milliseconds: 180));
    }
  }
}
