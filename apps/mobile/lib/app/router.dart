import 'package:go_router/go_router.dart';

import '../core/routing/app_routes.dart';
import '../features/matches/screens/match_list_screen.dart';
import '../features/pairing/screens/pairing_screen.dart';
import '../features/scoring/screens/score_screen.dart';

/// Screens of the app and how they connect:
///
/// pairing → matches → score (back returns to matches)
GoRouter createRouter() {
  return GoRouter(
    initialLocation: AppRoutes.pairing,
    routes: [
      GoRoute(
        path: AppRoutes.pairing,
        builder: (context, state) => const PairingScreen(),
      ),
      GoRoute(
        path: AppRoutes.matches,
        builder: (context, state) => const MatchListScreen(),
        routes: [
          GoRoute(
            path: ':matchId/score',
            builder: (context, state) =>
                ScoreScreen(matchId: state.pathParameters['matchId']!),
          ),
        ],
      ),
    ],
  );
}
