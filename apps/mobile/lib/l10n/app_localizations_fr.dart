// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for French (`fr`).
class AppLocalizationsFr extends AppLocalizations {
  AppLocalizationsFr([String locale = 'fr']) : super(locale);

  @override
  String get appTitle => 'Arbitre Badminton';

  @override
  String get language => 'Langue';

  @override
  String get french => 'Français';

  @override
  String get english => 'Anglais';

  @override
  String get newMatch => 'Nouveau match';

  @override
  String get matchSetupDescription =>
      'Configure les joueurs et le service initial.';

  @override
  String get startMatch => 'Commencer le match';

  @override
  String get tournamentName => 'Nom du tournoi';

  @override
  String get defaultTournamentName => 'Tournoi';

  @override
  String get courtNumber => 'Numéro du terrain';

  @override
  String get court => 'Terrain';

  @override
  String get category => 'Catégorie';

  @override
  String get umpireName => 'Nom de l’arbitre';

  @override
  String get umpire => 'Arbitre';

  @override
  String get serviceJudge => 'Juge de Service';

  @override
  String get serviceJudgeName => 'Nom du juge de service';

  @override
  String get umpireInformation => 'Arbitrage';

  @override
  String get newMatchDescription => 'Configurer et démarrer un nouveau match.';

  @override
  String get history => 'Historique';

  @override
  String get historyDescription => 'Consulter les matchs enregistrés.';

  @override
  String get matchHistory => 'Historique des matchs';

  @override
  String get matchDetails => 'Détails du match';

  @override
  String get noMatchHistory => 'Aucun match enregistré.';

  @override
  String get menSingles => 'Simple Homme';

  @override
  String get womenSingles => 'Simple Dame';

  @override
  String get menDoubles => 'Double Homme';

  @override
  String get womenDoubles => 'Double Dame';

  @override
  String get mixedDoubles => 'Double Mixte';

  @override
  String get team => 'l’équipe';

  @override
  String get teamA => 'Équipe A';

  @override
  String get teamB => 'Équipe B';

  @override
  String get leftTeam => 'Équipe gauche';

  @override
  String get rightTeam => 'Équipe droite';

  @override
  String get left => 'Gauche';

  @override
  String get right => 'Droite';

  @override
  String get leftSide => 'Côté gauche';

  @override
  String get rightSide => 'Côté droit';

  @override
  String get startingSide => 'Position des équipes';

  @override
  String get chooseLeftTeam => 'Choisis l’équipe située à droite.';

  @override
  String get switchSides => 'Changer de demi-terrain';

  @override
  String get player => 'Joueur';

  @override
  String get playerOne => 'Joueur 1';

  @override
  String get playerTwo => 'Joueur 2';

  @override
  String get server => 'Serveur';

  @override
  String get receiver => 'Receveur';

  @override
  String get chooseServerReceiver => 'Choisis le serveur et le receveur';

  @override
  String get positionsOnlyAtZero =>
      'La correction des positions est possible uniquement à 0–0. Annule d’abord les points joués pour revenir à 0–0.';

  @override
  String get set => 'Set';

  @override
  String get sets => 'Sets';

  @override
  String get setPoint => 'POINT DE SET';

  @override
  String get matchPoint => 'POINT DE MATCH';

  @override
  String get undoLastPoint => 'Annuler le dernier point';

  @override
  String get noPointToUndo => 'Aucun point à annuler.';

  @override
  String get nextSet => 'Set suivant';

  @override
  String get startSet => 'Commencer le set';

  @override
  String get shuttlesUsed => 'Volants utilisés';

  @override
  String get shuttles => 'Volants';

  @override
  String get midSetInterval => 'Pause à 8 points';

  @override
  String get endOfSet => 'Fin du set';

  @override
  String get finishInterval => 'Terminer la pause';

  @override
  String get resumeMatch => 'Reprendre le match';

  @override
  String get umpireAnnouncement => 'Annonce de l’arbitre';

  @override
  String firstSetAnnouncement(String winner, String score) {
    return 'Set! Premier set remporté par $winner, $score. Changez de demi-terrain.';
  }

  @override
  String secondSetAnnouncement(String winner, String score) {
    return 'Set! Deuxième set remporté par $winner, $score. Changez de demi-terrain.';
  }

  @override
  String get oneGameAll => 'Un set égalité.';

  @override
  String twentySecondsAnnouncement(String courtNumber) {
    return 'Terrain $courtNumber, 20 secondes.';
  }

  @override
  String matchAnnouncement(String winner, String scores) {
    return 'Jeu! Match remporté par $winner, $scores.';
  }

  @override
  String get readyToPlayAnnouncement => 'Prêt à jouer.';

  @override
  String firstGameSinglesStartAnnouncement(
    String right,
    String left,
    String server,
  ) {
    return 'Mesdames et Messieurs à ma droite, $right et à ma gauche, $left. $server au service zéro égalité jouez.';
  }

  @override
  String firstGameDoublesStartAnnouncement(
    String right,
    String left,
    String server,
    String receiver,
  ) {
    return 'Mesdames et Messieurs à ma droite, $right et à ma gauche, $left. $server au service sur $receiver zéro égalité jouez.';
  }

  @override
  String get secondGameStartAnnouncement => 'Second set zéro égalité jouez.';

  @override
  String get finalGameStartAnnouncement => 'Dernier set zéro égalité jouez.';

  @override
  String get matchFinished => 'Match terminé';

  @override
  String get winner => 'Vainqueur';

  @override
  String get duration => 'Durée :';

  @override
  String get finishMatch => 'Terminer le match';

  @override
  String get matchActions => 'Actions du match';

  @override
  String get actions => 'Actions';

  @override
  String get matchInterruption => 'Interruption du match';

  @override
  String get callMedicalService => 'Appeler le service médical';

  @override
  String get callScoringTable => 'Appeler la table de marque';

  @override
  String get callReferee => 'Appeler le juge-arbitre';

  @override
  String get correctPlayerPositions => 'Corriger les positions';

  @override
  String get assistance => 'Assistance';

  @override
  String get incidents => 'Incidents';

  @override
  String get corrections => 'Corrections';

  @override
  String get match => 'Match';

  @override
  String get injury => 'Blessure';

  @override
  String get warningOrCard => 'Avertissement ou carton';

  @override
  String get sanction => 'Sanction';

  @override
  String get warning => 'Avertissement';

  @override
  String get yellowCard => 'Carton jaune';

  @override
  String get redCard => 'Carton rouge';

  @override
  String get blackCard => 'Carton noir';

  @override
  String get confirmSanction => 'Confirmer la sanction';

  @override
  String get irreversibleAction =>
      'Cette action est irréversible. Vérifiez les informations avant de confirmer.';

  @override
  String get retirement => 'Abandon';

  @override
  String get selectRetiringPlayer => 'Sélectionnez le joueur qui abandonne.';

  @override
  String get confirmRetirement => 'Confirmer l’abandon';

  @override
  String retirementConfirmationMessage(String winner) {
    return 'Cet abandon terminera immédiatement le match. $winner sera déclaré vainqueur. Cette action est irréversible.';
  }

  @override
  String get playerColors => 'Couleurs des joueurs';

  @override
  String get colorsOf => 'Couleur de';

  @override
  String get editPlayerColors => 'Modifier les couleurs';

  @override
  String get sameColor => 'Même couleur';

  @override
  String get apply => 'Appliquer';

  @override
  String get back => 'Retour';

  @override
  String get home => 'Accueil';

  @override
  String get cancel => 'Annuler';

  @override
  String get confirm => 'Confirmer';

  @override
  String get continueLabel => 'Continuer';

  @override
  String get start => 'Démarrer';

  @override
  String get finish => 'Terminer';

  @override
  String get optional => 'Facultatif';

  @override
  String get requiredField => 'Champ obligatoire';

  @override
  String get notProvided => 'Non renseigné';

  @override
  String get startTime => 'Début';

  @override
  String get endTime => 'Fin';

  @override
  String actionNotImplemented(String title) {
    return 'Action « $title » à implémenter.';
  }

  @override
  String get checkServerReceiver =>
      'Vérifiez le serveur et le receveur avant de commencer le set.';

  @override
  String get confirmAndStartSet => 'Valider et commencer le set';

  @override
  String get selected => 'sélectionné(s)';

  @override
  String get selectMatches => 'Sélectionner des matchs';

  @override
  String get clearHistory => 'Supprimer l’historique';

  @override
  String get clearHistoryConfirmation =>
      'Êtes-vous sûr de vouloir supprimer tous les matchs enregistrés ?';

  @override
  String get delete => 'Supprimer';

  @override
  String get deleteSelectedMatches => 'Supprimer les matchs';

  @override
  String deleteSelectedMatchesConfirmation(int count) {
    return 'Supprimer les $count matchs sélectionnés ?';
  }

  @override
  String get warmup => 'Échauffement';

  @override
  String get startWarmup => 'Démarrer l’échauffement';

  @override
  String get skipWarmup => 'Passer l’échauffement';

  @override
  String get matchClassification => 'Classification du match';

  @override
  String get matchClassificationDescription =>
      'Définissez la catégorie et les informations de compétition.';

  @override
  String get ageCategory => 'Catégorie d’âge';

  @override
  String get ageCategoryHint => 'Ex. Minime Fille';

  @override
  String get poolGroup => 'Poule / Groupe';

  @override
  String get poolGroupHint => 'Ex. Poule B';

  @override
  String get tournamentPhase => 'Phase';

  @override
  String get tournamentPhaseHint => 'Ex. Quart de finale';

  @override
  String get agePoussin => 'Poussin';

  @override
  String get agePoussine => 'Poussine';

  @override
  String get ageBenjamin => 'Benjamin';

  @override
  String get ageBenjamine => 'Benjamine';

  @override
  String get ageMinimeBoy => 'Minime Garçon';

  @override
  String get ageMinimeGirl => 'Minime Fille';

  @override
  String get ageCadet => 'Cadet';

  @override
  String get ageCadette => 'Cadette';

  @override
  String get ageJuniorBoy => 'Junior Garçon';

  @override
  String get ageJuniorGirl => 'Junior Fille';

  @override
  String get ageSeniorSeries1 => 'Senior Série 1';

  @override
  String get ageSeniorSeries2 => 'Senior Série 2';

  @override
  String get ageSeniorSeries3 => 'Senior Série 3';

  @override
  String get poolUnique => 'Poule unique';

  @override
  String get poolA => 'Poule A';

  @override
  String get poolB => 'Poule B';

  @override
  String get poolC => 'Poule C';

  @override
  String get poolD => 'Poule D';

  @override
  String get phasePoolMatch => 'Match de poule';

  @override
  String get phaseRoundOf16 => 'Huitième de finale';

  @override
  String get phaseQuarterFinal => 'Quart de finale';

  @override
  String get phaseSemiFinal => 'Demi-finale';

  @override
  String get phaseFinal => 'Finale';

  @override
  String get phaseClassification => 'Match de classement';

  @override
  String get settings => 'Paramètres';

  @override
  String get matchSettings => 'Match';

  @override
  String get umpireAnnouncements => 'Annonces de l\'arbitre';

  @override
  String get umpireAnnouncementsDescription =>
      'Afficher les annonces à prononcer par l\'arbitre pendant le match';
}
