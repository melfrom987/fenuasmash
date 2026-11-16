import 'package:flutter/material.dart';

void main() {
  runApp(const FenuaSmashApp());
}

/// FenuaSmash umpire app: runs on the tablet of a court to enter the scores.
class FenuaSmashApp extends StatelessWidget {
  const FenuaSmashApp({super.key});

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      title: 'FenuaSmash',
      home: Scaffold(body: Center(child: Text('FenuaSmash'))),
    );
  }
}
