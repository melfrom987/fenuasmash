import 'package:flutter/material.dart';

import '../../../l10n/l10n.dart';

/// Score entry for one match, point by point.
///
/// Placeholder until the score screen (#104).
class ScoreScreen extends StatelessWidget {
  const ScoreScreen({super.key, required this.matchId});

  final String matchId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.scoreTitle)),
      body: Center(child: Text(matchId)),
    );
  }
}
