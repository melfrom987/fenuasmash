import 'package:badminton_umpire/features/match/domain/models/match_config.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('MatchConfig JSON', () {
    test('serializes and deserializes complete config', () {
      const config = MatchConfig(
        isDoubles: true,
        teamAPlayers: ['Alice', 'Anna'],
        teamBPlayers: ['Bob', 'Ben'],
        tournamentName: 'Tahiti Open',
        courtNumber: '3',
        category: 'Mixed Doubles',
        ageCategory: 'Senior',
        poolGroup: 'Pool A',
        tournamentPhase: 'Pool match',
        umpireName: 'Melissa',
        serviceJudgeName: 'John',
      );

      final json = config.toJson();

      final restored = MatchConfig.fromJson(json);

      expect(restored.isDoubles, isTrue);

      expect(restored.teamAPlayers, ['Alice', 'Anna']);

      expect(restored.teamBPlayers, ['Bob', 'Ben']);

      expect(restored.tournamentName, 'Tahiti Open');
      expect(restored.courtNumber, '3');
      expect(restored.category, 'Mixed Doubles');
      expect(restored.ageCategory, 'Senior');
      expect(restored.poolGroup, 'Pool A');
      expect(restored.tournamentPhase, 'Pool match');
      expect(restored.umpireName, 'Melissa');
      expect(restored.serviceJudgeName, 'John');
    });

    test('optional classification fields may be null', () {
      const config = MatchConfig(
        isDoubles: false,
        teamAPlayers: ['Alice'],
        teamBPlayers: ['Bob'],
        tournamentName: 'Tournament',
        courtNumber: '1',
        category: 'Women Singles',
        umpireName: 'Melissa',
      );

      final restored = MatchConfig.fromJson(config.toJson());

      expect(restored.ageCategory, isNull);
      expect(restored.poolGroup, isNull);
      expect(restored.tournamentPhase, isNull);
      expect(restored.serviceJudgeName, isNull);
    });
  });
}
