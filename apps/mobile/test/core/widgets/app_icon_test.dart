import 'package:fenuasmash/core/widgets/app_icon.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hugeicons/hugeicons.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(
    home: Scaffold(
      body: DefaultTextStyle(
        style: const TextStyle(color: Colors.teal),
        child: child,
      ),
    ),
  );

  testWidgets('uses the design system defaults', (tester) async {
    await tester.pumpWidget(
      wrap(const AppIcon(HugeIcons.strokeRoundedBadminton)),
    );

    final icon = tester.widget<HugeIcon>(find.byType(HugeIcon));
    expect(icon.size, AppIconSize.regular);
    expect(icon.strokeWidth, 1.75);
    expect(icon.color, Colors.teal);
  });

  testWidgets('is hidden from screen readers without a label', (tester) async {
    final semantics = tester.ensureSemantics();
    await tester.pumpWidget(
      wrap(const AppIcon(HugeIcons.strokeRoundedBadminton)),
    );

    expect(find.bySemanticsLabel('Badminton'), findsNothing);
    semantics.dispose();
  });

  testWidgets('is announced with its label when it has one', (tester) async {
    final semantics = tester.ensureSemantics();
    await tester.pumpWidget(
      wrap(
        const AppIcon(
          HugeIcons.strokeRoundedBadminton,
          semanticLabel: 'Badminton',
        ),
      ),
    );

    expect(find.bySemanticsLabel('Badminton'), findsOneWidget);
    semantics.dispose();
  });
}
