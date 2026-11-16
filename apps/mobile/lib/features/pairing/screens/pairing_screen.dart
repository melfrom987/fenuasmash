import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/routing/app_routes.dart';
import '../../../core/widgets/page_body.dart';
import '../../../l10n/l10n.dart';

/// Links the tablet to a court with the code shown in the administration.
///
/// Placeholder until the umpire login (#85): the button goes straight to the
/// match list.
class PairingScreen extends StatelessWidget {
  const PairingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: PageBody(
        maxWidth: PageBody.formMaxWidth,
        scrollable: true,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              context.l10n.appTitle,
              style: Theme.of(context).textTheme.displaySmall,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: () => context.go(AppRoutes.matches),
              child: Text(context.l10n.pairingContinue),
            ),
          ],
        ),
      ),
    );
  }
}
