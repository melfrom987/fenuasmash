import 'package:fenuasmash/core/layout/window_size.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('classifies widths like Material 3', () {
    expect(WindowSize.fromWidth(360), WindowSize.compact);
    expect(WindowSize.fromWidth(599.9), WindowSize.compact);
    expect(WindowSize.fromWidth(600), WindowSize.medium);
    expect(WindowSize.fromWidth(839.9), WindowSize.medium);
    expect(WindowSize.fromWidth(840), WindowSize.expanded);
    expect(WindowSize.fromWidth(1280), WindowSize.expanded);
  });

  test('padding grows with the screen', () {
    expect(WindowSize.compact.horizontalPadding, 16);
    expect(WindowSize.medium.horizontalPadding, 24);
    expect(WindowSize.expanded.horizontalPadding, 32);
  });
}
