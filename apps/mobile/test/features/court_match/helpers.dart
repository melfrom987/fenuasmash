import 'package:badminton_umpire/features/court_match/data/court_match_api.dart';
import 'package:badminton_umpire/features/court_match/domain/court_match.dart';
import 'package:badminton_umpire/features/pairing/domain/tablet_session.dart';

const tournamentId = '3c0b4f1e-2a6d-4e8b-9f1a-7d2c5e8b0a14';
const eventId = '6e1d2c3b-4a5f-4b7e-8d9c-0a1b2c3d4e5f';
const matchId = '9b2f6a52-7c1e-4d5e-8a3b-1f0c2d4e6a8b';

/// A `Match` of the API, as sent by `GET /tablets/me/match`.
Map<String, Object?> matchJson({
  String id = matchId,
  String status = 'CALLED',
  int stateVersion = 3,
  int? round = 2,
  List<Map<String, int>> games = const [],
}) {
  return {
    'id': id,
    'tournamentId': tournamentId,
    'eventId': eventId,
    'round': round,
    'sideA': '0f8e2c1a-1b2c-4d3e-8f9a-0b1c2d3e4f5a',
    'sideB': '1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d',
    'courtId': 'court-3',
    'status': status,
    'winner': null,
    'outcome': null,
    'score': {'games': games, 'winner': null},
    'stateVersion': stateVersion,
    'startedAt': null,
    'endedAt': null,
    'createdAt': '2026-11-21T07:00:00Z',
    'updatedAt': '2026-11-21T08:00:00Z',
  };
}

CourtMatch calledMatch({
  String id = matchId,
  CourtMatchStatus status = CourtMatchStatus.called,
  int stateVersion = 3,
  int? round = 2,
  String? discipline = 'XD',
  String? eventName = 'Double mixte Senior',
  String? tournamentName = 'Open de Papeete',
  String? categoryLabel = 'Senior',
}) {
  return CourtMatch(
    id: id,
    tournamentId: tournamentId,
    eventId: eventId,
    status: status,
    stateVersion: stateVersion,
    round: round,
    tournamentName: tournamentName,
    eventName: eventName,
    discipline: discipline,
    categoryLabel: categoryLabel,
  );
}

/// Answers [next] to every read of the court, and counts them.
class FakeCourtMatchApi implements CourtMatchApi {
  CourtMatchResult next = const CourtFree();
  final List<({TabletSession session, CourtMatch? known})> calls = [];

  @override
  Future<CourtMatchResult> fetch(
    TabletSession session, {
    CourtMatch? known,
  }) async {
    calls.add((session: session, known: known));
    return next;
  }
}
