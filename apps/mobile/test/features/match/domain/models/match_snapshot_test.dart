import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:badminton_umpire/features/match/domain/models/match_snapshot.dart';
import 'package:badminton_umpire/features/match/domain/models/team_court_state.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('MatchSnapshot JSON preserves interval end time', () {
    final intervalEndsAt = DateTime(2026, 9, 6, 16, 30);

    final snapshot = MatchSnapshot(
      teamAScore: 8,
      teamBScore: 5,
      teamASets: 1,
      teamBSets: 0,
      servingTeamId: 'teamA',
      serverName: 'Alice',
      receiverName: 'Bob',
      leftSideTeamId: 'teamA',
      midSetIntervalTaken: true,
      lastSetWinnerTeamId: 'teamA',
      previousSets: const [
        [15, 10],
      ],
      phase: MatchPhase.midSetIntervalRunning,
      intervalRemaining: const Duration(seconds: 42),
      intervalEndsAt: intervalEndsAt,
      teamA: TeamCourtState(
        teamId: 'teamA',
        players: const ['Alice'],
        topPlayer: 'Alice',
        bottomPlayer: '',
      ),
      teamB: TeamCourtState(
        teamId: 'teamB',
        players: const ['Bob'],
        topPlayer: 'Bob',
        bottomPlayer: '',
      ),
    );

    final restored = MatchSnapshot.fromJson(snapshot.toJson());

    expect(restored.teamAScore, 8);
    expect(restored.teamBScore, 5);

    expect(restored.phase, MatchPhase.midSetIntervalRunning);

    expect(restored.intervalRemaining, const Duration(seconds: 42));

    expect(restored.intervalEndsAt, intervalEndsAt);
  });
}
