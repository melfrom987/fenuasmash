import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  final occurredAt = DateTime.utc(2026, 11, 21, 18, 30, 5);

  group('ScoreEvent JSON', () {
    test('writes a point in the API format', () {
      final event = ScoreEvent(
        id: '0f8fad5b-d9cb-469f-a165-70867728950e',
        sequence: 3,
        type: ScoreEventType.point,
        side: MatchSide.b,
        occurredAt: occurredAt,
      );

      expect(event.toJson(), {
        'id': '0f8fad5b-d9cb-469f-a165-70867728950e',
        'sequence': 3,
        'type': 'POINT',
        'side': 'B',
        'occurredAt': '2026-11-21T18:30:05.000Z',
      });
    });

    test('writes an undo without side', () {
      final event = ScoreEvent(
        id: 'id',
        sequence: 4,
        type: ScoreEventType.undo,
        side: null,
        occurredAt: occurredAt,
      );

      expect(event.toJson(), isNot(contains('side')));
      expect(event.toJson()['type'], 'UNDO');
    });

    test('stores the time in UTC', () {
      final event = ScoreEvent(
        id: 'id',
        sequence: 1,
        type: ScoreEventType.point,
        side: MatchSide.a,
        occurredAt: DateTime.parse('2026-11-21T08:30:05-10:00'),
      );

      expect(event.occurredAt.isUtc, isTrue);
      expect(event.toJson()['occurredAt'], '2026-11-21T18:30:05.000Z');
    });

    test('reads back what it writes', () {
      final event = ScoreEvent(
        id: 'id',
        sequence: 7,
        type: ScoreEventType.point,
        side: MatchSide.a,
        occurredAt: occurredAt,
      );

      final copy = ScoreEvent.fromJson(event.toJson());

      expect(copy.toJson(), event.toJson());
    });

    test('rejects unknown values', () {
      expect(
        () => ScoreEvent.fromJson({
          'id': 'id',
          'sequence': 1,
          'type': 'SMASH',
          'occurredAt': '2026-11-21T18:30:05Z',
        }),
        throwsFormatException,
      );
    });
  });

  group('ScoreEvent rules', () {
    test('a point needs a side and an undo has none', () {
      expect(
        () => ScoreEvent(
          id: 'id',
          sequence: 1,
          type: ScoreEventType.point,
          side: null,
          occurredAt: occurredAt,
        ),
        throwsArgumentError,
      );
      expect(
        () => ScoreEvent(
          id: 'id',
          sequence: 1,
          type: ScoreEventType.undo,
          side: MatchSide.a,
          occurredAt: occurredAt,
        ),
        throwsArgumentError,
      );
    });

    test('sequences start at 1', () {
      expect(
        () => ScoreEvent(
          id: 'id',
          sequence: 0,
          type: ScoreEventType.undo,
          side: null,
          occurredAt: occurredAt,
        ),
        throwsArgumentError,
      );
    });

    test('maps controller team ids to sides', () {
      expect(MatchSide.fromTeamId('teamA'), MatchSide.a);
      expect(MatchSide.fromTeamId('teamB'), MatchSide.b);
      expect(() => MatchSide.fromTeamId('teamC'), throwsArgumentError);
    });
  });
}
