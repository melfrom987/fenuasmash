/// Paths of the app screens. Features navigate with these paths, so that a
/// feature never has to import another one:
///
/// ```dart
/// context.go(AppRoutes.matches);
/// ```
abstract final class AppRoutes {
  /// Link the tablet to a court (first screen).
  static const pairing = '/pairing';

  /// Matches assigned to the court.
  static const matches = '/matches';

  /// Score entry of one match, opened on top of the match list.
  static String score(String matchId) =>
      '$matches/${Uri.encodeComponent(matchId)}/score';
}
