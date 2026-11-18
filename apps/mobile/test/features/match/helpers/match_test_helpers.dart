import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';
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
