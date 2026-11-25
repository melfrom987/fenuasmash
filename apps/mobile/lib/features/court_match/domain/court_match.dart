import 'package:badminton_umpire/features/match_setup/domain/models/match_category.dart';

/// Status of a match on the court, as sent by the server. Only these three
/// statuses put a match on a court.
enum CourtMatchStatus {
  /// `CALLED`: the players are called to the court.
  called,

  /// `WARMUP`: the players warm up.
  warmup,

  /// `IN_PROGRESS`: being played.
  inProgress;

  static CourtMatchStatus? fromApi(Object? value) => switch (value) {
    'CALLED' => called,
    'WARMUP' => warmup,
    'IN_PROGRESS' => inProgress,
    _ => null,
  };
}

/// The match the organizer called to the court of the tablet.
///
/// The sides are registrations of the tournament: the tablet does not get the
/// names of the players, which the umpire types in when opening the match.
class CourtMatch {
  const CourtMatch({
    required this.id,
    required this.tournamentId,
    required this.eventId,
    required this.status,
    required this.stateVersion,
    this.round,
    this.games = const [],
    this.tournamentName,
    this.eventName,
    this.discipline,
    this.categoryLabel,
  });

  final String id;
  final String tournamentId;
  final String eventId;
  final CourtMatchStatus status;
  final int stateVersion;

  /// Round in the draw, from 1; null when the match is not part of a draw.
  final int? round;

  /// Score of each game played or in progress, as known by the server.
  final List<({int a, int b})> games;

  /// Labels read from the tournament and the event; null when they could not
  /// be read.
  final String? tournamentName;
  final String? eventName;

  /// `MS`, `WS`, `MD`, `WD`, `XD` or `OD`.
  final String? discipline;

  /// E.g. "Cadet U17"; null for an open event.
  final String? categoryLabel;

  /// The category of the setup screen; null for open doubles (`OD`), which
  /// has no equivalent there.
  MatchCategory? get category => switch (discipline) {
    'MS' => MatchCategory.menSingles,
    'WS' => MatchCategory.womenSingles,
    'MD' => MatchCategory.menDoubles,
    'WD' => MatchCategory.womenDoubles,
    'XD' => MatchCategory.mixedDoubles,
    _ => null,
  };

  /// Whether the event is played in doubles, when it is known.
  bool? get isDoubles =>
      discipline == null ? null : discipline != 'MS' && discipline != 'WS';

  /// Reads a `Match` of the API. Throws a [FormatException] when it is not
  /// one, or when the match is not on a court.
  factory CourtMatch.fromJson(Map<String, dynamic> json) {
    final status = CourtMatchStatus.fromApi(json['status']);
    final id = json['id'];
    final tournamentId = json['tournamentId'];
    final eventId = json['eventId'];
    final stateVersion = json['stateVersion'];
    final round = json['round'];
    if (status == null ||
        id is! String ||
        tournamentId is! String ||
        eventId is! String ||
        stateVersion is! int ||
        (round != null && round is! int)) {
      throw const FormatException('Not a match on a court');
    }
    final score = json['score'];
    final games = <({int a, int b})>[];
    if (score is Map<String, dynamic> && score['games'] is List) {
      for (final game in score['games'] as List) {
        if (game is Map<String, dynamic> &&
            game['a'] is int &&
            game['b'] is int) {
          games.add((a: game['a'] as int, b: game['b'] as int));
        }
      }
    }
    return CourtMatch(
      id: id,
      tournamentId: tournamentId,
      eventId: eventId,
      status: status,
      stateVersion: stateVersion,
      round: round as int?,
      games: games,
    );
  }

  CourtMatch withLabels({
    String? tournamentName,
    String? eventName,
    String? discipline,
    String? categoryLabel,
  }) {
    return CourtMatch(
      id: id,
      tournamentId: tournamentId,
      eventId: eventId,
      status: status,
      stateVersion: stateVersion,
      round: round,
      games: games,
      tournamentName: tournamentName,
      eventName: eventName,
      discipline: discipline,
      categoryLabel: categoryLabel,
    );
  }

  /// Same match, same state and same labels.
  @override
  bool operator ==(Object other) {
    return other is CourtMatch &&
        other.id == id &&
        other.stateVersion == stateVersion &&
        other.status == status &&
        other.tournamentName == tournamentName &&
        other.eventName == eventName &&
        other.discipline == discipline &&
        other.categoryLabel == categoryLabel;
  }

  @override
  int get hashCode => Object.hash(
    id,
    stateVersion,
    status,
    tournamentName,
    eventName,
    discipline,
    categoryLabel,
  );
}
