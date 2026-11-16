import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/routing/app_routes.dart';
import '../../../l10n/l10n.dart';

/// Matches assigned to the court of this tablet.
///
/// Placeholder until the match list (#101): shows one example match.
class MatchListScreen extends StatelessWidget {
  const MatchListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(context.l10n.matchesTitle)),
      body: ListView(
        children: [
          ListTile(
            title: Text(context.l10n.matchesExample),
            onTap: () => context.push(AppRoutes.score('example')),
          ),
        ],
      ),
    );
  }
}
