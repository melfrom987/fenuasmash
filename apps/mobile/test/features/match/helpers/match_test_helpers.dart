import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';
import 'package:badminton_umpire/features/match/domain/models/match_config.dart';
import 'package:badminton_umpire/features/match/domain/models/match_draft.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';

MatchController createSinglesController({
  String leftSideTeamId = 'teamA',
  String servingTeamId = 'teamA',
  String serverName = 'Alice',
  String receiverName = 'Bob',
}) {
  return MatchController(
    isDoubles: false,
    teamAPlayers: const ['Alice'],
    teamBPlayers: const ['Bob'],
    leftSideTeamId: leftSideTeamId,
    servingTeamId: servingTeamId,
    serverName: serverName,
    receiverName: receiverName,
    courtNumber: '1',
  );
}

MatchController createDoublesController({
  String leftSideTeamId = 'teamA',
  String servingTeamId = 'teamA',
  String serverName = 'Alice',
  String receiverName = 'Charlie',
}) {
  return MatchController(
    isDoubles: true,
    teamAPlayers: const ['Alice', 'Anna'],
    teamBPlayers: const ['Charlie', 'Chloe'],
    leftSideTeamId: leftSideTeamId,
    servingTeamId: servingTeamId,
    serverName: serverName,
    receiverName: receiverName,
    courtNumber: '1',
  );
}

/// Ajoute un point et saute automatiquement uniquement
/// l'intervalle de milieu de set.
void addTestPoint(MatchController controller, String teamId) {
  controller.addPoint(teamId);

  if (controller.phase == MatchPhase.midSetIntervalReady) {
    controller.finishIntervalEarly();
  }
}

void addTestPoints(MatchController controller, String teamId, int count) {
  for (var i = 0; i < count; i++) {
    addTestPoint(controller, teamId);
  }
}

void finishBetweenSetsInterval(MatchController controller) {
  expectPhase(controller, MatchPhase.betweenSetsIntervalReady);

  controller.finishIntervalEarly();

  expectPhase(controller, MatchPhase.preparingNextSet);
}

void startPreparedNextSet(MatchController controller) {
  final server = controller.nextSetServerCandidates.first;

  final receiver = controller.nextSetReceiverCandidates.first;

  controller.startNextSet(
    selectedServerName: server,
    selectedReceiverName: receiver,
  );
}

void prepareNextSet(MatchController controller) {
  finishBetweenSetsInterval(controller);
  startPreparedNextSet(controller);
}

void winSet(MatchController controller, String teamId) {
  while (controller.phase == MatchPhase.playing) {
    addTestPoint(controller, teamId);
  }
}

void expectPhase(MatchController controller, MatchPhase phase) {
  assert(
    controller.phase == phase,
    'Expected $phase but got ${controller.phase}',
  );
}

/// Saves a singles match between Alice and Bob the way the match page does.
MatchDraft draftOf(
  MatchController controller, {
  String matchId = 'match-1',
  required DateTime savedAt,
}) {
  return MatchDraft(
    matchId: matchId,
    config: const MatchConfig(
      isDoubles: false,
      teamAPlayers: ['Alice'],
      teamBPlayers: ['Bob'],
      tournamentName: 'Open',
      courtNumber: '1',
      category: 'Singles',
      umpireName: 'Umpire',
    ),
    teamAScore: controller.teamAScore,
    teamBScore: controller.teamBScore,
    teamASets: controller.teamASets,
    teamBSets: controller.teamBSets,
    servingTeamId: controller.servingTeamId,
    serverName: controller.serverName,
    receiverName: controller.receiverName,
    leftSideTeamId: controller.leftSideTeamId,
    teamA: controller.teamA.copy(),
    teamB: controller.teamB.copy(),
    midSetIntervalTaken: controller.midSetIntervalTaken,
    lastSetWinnerTeamId: controller.lastSetWinnerTeamId,
    previousSets: controller.previousSets
        .map((set) => List<int>.from(set))
        .toList(),
    phase: controller.phase,
    intervalRemaining: controller.intervalRemaining,
    history: controller.history,
    events: controller.events,
    scoreEvents: controller.scoreEvents,
    matchStarted: true,
    matchStartedAt: savedAt,
    matchFinishedAt: null,
    warmupStarted: false,
    warmupEndsAt: null,
    shuttleCount: 1,
    playerColors: const {},
    savedAt: savedAt,
  );
}
