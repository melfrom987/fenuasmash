import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/routing/app_routes.dart';

/// Matches assigned to the court of this tablet.
///
/// Placeholder until the match list (#101): shows one example match.
class MatchListScreen extends StatelessWidget {
  const MatchListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Matchs')),
      body: ListView(
        children: [
          ListTile(
            title: const Text('Match exemple'),
            onTap: () => context.push(AppRoutes.score('example')),
          ),
        ],
      ),
    );
  }
}
