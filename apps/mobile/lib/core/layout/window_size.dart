import 'package:flutter/widgets.dart';

/// Width classes of the Material 3 guidelines: phones are compact, tablets in
/// portrait are medium, tablets in landscape are expanded.
enum WindowSize {
  compact,
  medium,
  expanded;

  static const double mediumMinWidth = 600;
  static const double expandedMinWidth = 840;

  static WindowSize fromWidth(double width) {
    if (width >= expandedMinWidth) {
      return WindowSize.expanded;
    }
    if (width >= mediumMinWidth) {
      return WindowSize.medium;
    }
    return WindowSize.compact;
  }

  /// Space between the content and the edges of the screen.
  double get horizontalPadding => switch (this) {
    WindowSize.compact => 16,
    WindowSize.medium => 24,
    WindowSize.expanded => 32,
  };
}

extension WindowSizeContext on BuildContext {
  /// Width class of the current screen, updated when the tablet is rotated.
  WindowSize get windowSize =>
      WindowSize.fromWidth(MediaQuery.sizeOf(this).width);
}
