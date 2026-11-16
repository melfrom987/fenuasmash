import 'package:fenuasmash/app/app.dart';
import 'package:fenuasmash/core/config/app_config.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('app starts', (tester) async {
    await tester.pumpWidget(FenuaSmashApp(config: AppConfig.fromEnvironment()));

    expect(find.text('FenuaSmash'), findsOneWidget);
  });
}
