import 'package:fenuasmash/main.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('app starts', (tester) async {
    await tester.pumpWidget(const FenuaSmashApp());

    expect(find.text('FenuaSmash'), findsOneWidget);
  });
}
