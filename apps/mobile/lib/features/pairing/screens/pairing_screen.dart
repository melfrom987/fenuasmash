import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/routing/app_routes.dart';

/// Links the tablet to a court with the code shown in the administration.
///
/// Placeholder until the umpire login (#85): the button goes straight to the
/// match list.
class PairingScreen extends StatelessWidget {
  const PairingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('FenuaSmash', style: Theme.of(context).textTheme.displaySmall),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: () => context.go(AppRoutes.matches),
              child: const Text('Continuer'),
            ),
          ],
        ),
      ),
    );
  }
}
