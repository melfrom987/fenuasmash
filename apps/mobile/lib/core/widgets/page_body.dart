import 'package:flutter/widgets.dart';

import '../layout/window_size.dart';

/// Body of a screen that adapts to phones and tablets, in portrait and
/// landscape: content centered, at most [maxWidth] wide, with padding that
/// grows with the screen and stays clear of notches and system bars.
///
/// [scrollable] lets the content scroll when the screen is too short, for
/// example a phone in landscape with the keyboard open.
class PageBody extends StatelessWidget {
  const PageBody({
    super.key,
    required this.child,
    this.maxWidth = PageBody.listMaxWidth,
    this.scrollable = false,
    this.verticalPadding = 16,
  });

  /// For forms and short messages.
  static const double formMaxWidth = 480;

  /// For lists.
  static const double listMaxWidth = 840;

  final Widget child;
  final double maxWidth;
  final bool scrollable;
  final double verticalPadding;

  @override
  Widget build(BuildContext context) {
    final padding = EdgeInsets.symmetric(
      horizontal: context.windowSize.horizontalPadding,
      vertical: verticalPadding,
    );
    final content = Center(
      child: ConstrainedBox(
        constraints: BoxConstraints(maxWidth: maxWidth),
        child: Padding(padding: padding, child: child),
      ),
    );
    return SafeArea(
      child: scrollable
          ? LayoutBuilder(
              builder: (context, constraints) => SingleChildScrollView(
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: constraints.maxHeight),
                  child: content,
                ),
              ),
            )
          : content,
    );
  }
}
