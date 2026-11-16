import 'package:flutter/material.dart';

/// Score entry for one match, point by point.
///
/// Placeholder until the score screen (#104).
class ScoreScreen extends StatelessWidget {
  const ScoreScreen({super.key, required this.matchId});

  final String matchId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Score')),
      body: Center(child: Text(matchId)),
    );
  }
}
