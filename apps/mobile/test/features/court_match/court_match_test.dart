import 'package:badminton_umpire/features/court_match/domain/court_match.dart';
import 'package:badminton_umpire/features/match_setup/domain/models/match_category.dart';
import 'package:flutter_test/flutter_test.dart';

import 'helpers.dart';

void main() {
  test('reads a match of the API', () {
    final match = CourtMatch.fromJson(
      matchJson(
        status: 'IN_PROGRESS',
        stateVersion: 8,
        games: [
          {'a': 21, 'b': 17},
          {'a': 4, 'b': 6},
        ],
      ),
    );

    expect(match.id, matchId);
    expect(match.tournamentId, tournamentId);
    expect(match.eventId, eventId);
    expect(match.status, CourtMatchStatus.inProgress);
    expect(match.stateVersion, 8);
    expect(match.round, 2);
    expect(match.games, [(a: 21, b: 17), (a: 4, b: 6)]);
    expect(match.eventName, isNull);
  });

  test('a match outside a draw has no round', () {
    expect(CourtMatch.fromJson(matchJson(round: null)).round, isNull);
  });

  test('only a match on a court is accepted', () {
    for (final status in ['READY', 'FINISHED', 'SCHEDULED', 'UNKNOWN']) {
      expect(
        () => CourtMatch.fromJson(matchJson(status: status)),
        throwsFormatException,
        reason: status,
      );
    }
  });

  test('an answer that is not a match is refused', () {
    expect(
      () => CourtMatch.fromJson({'title': 'Wi-Fi login'}),
      throwsFormatException,
    );
    expect(
      () => CourtMatch.fromJson({...matchJson(), 'stateVersion': '3'}),
      throwsFormatException,
    );
  });

  test('the discipline gives the category of the setup screen', () {
    const expected = {
      'MS': MatchCategory.menSingles,
      'WS': MatchCategory.womenSingles,
      'MD': MatchCategory.menDoubles,
      'WD': MatchCategory.womenDoubles,
      'XD': MatchCategory.mixedDoubles,
    };
    for (final MapEntry(key: discipline, value: category) in expected.entries) {
      expect(calledMatch(discipline: discipline).category, category);
    }
    // Open doubles: any pair, no category to choose for the umpire
    expect(calledMatch(discipline: 'OD').category, isNull);
    expect(calledMatch(discipline: 'OD').isDoubles, isTrue);
    expect(calledMatch(discipline: 'MS').isDoubles, isFalse);
    expect(calledMatch(discipline: null).isDoubles, isNull);
  });

  test('two reads of the same state are equal', () {
    expect(calledMatch(), calledMatch());
    expect(calledMatch(), isNot(calledMatch(stateVersion: 4)));
    expect(calledMatch(), isNot(calledMatch(eventName: null)));
  });
}
