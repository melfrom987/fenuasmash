import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';
import 'app_localizations_fr.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('en'),
    Locale('fr'),
  ];

  /// No description provided for @appTitle.
  ///
  /// In fr, this message translates to:
  /// **'Arbitre Badminton'**
  String get appTitle;

  /// No description provided for @language.
  ///
  /// In fr, this message translates to:
  /// **'Langue'**
  String get language;

  /// No description provided for @french.
  ///
  /// In fr, this message translates to:
  /// **'Français'**
  String get french;

  /// No description provided for @english.
  ///
  /// In fr, this message translates to:
  /// **'Anglais'**
  String get english;

  /// No description provided for @newMatch.
  ///
  /// In fr, this message translates to:
  /// **'Nouveau match'**
  String get newMatch;

  /// No description provided for @matchSetupDescription.
  ///
  /// In fr, this message translates to:
  /// **'Configure les joueurs et le service initial.'**
  String get matchSetupDescription;

  /// No description provided for @startMatch.
  ///
  /// In fr, this message translates to:
  /// **'Commencer le match'**
  String get startMatch;

  /// No description provided for @tournamentName.
  ///
  /// In fr, this message translates to:
  /// **'Nom du tournoi'**
  String get tournamentName;

  /// No description provided for @defaultTournamentName.
  ///
  /// In fr, this message translates to:
  /// **'Tournoi'**
  String get defaultTournamentName;

  /// No description provided for @courtNumber.
  ///
  /// In fr, this message translates to:
  /// **'Numéro du terrain'**
  String get courtNumber;

  /// No description provided for @court.
  ///
  /// In fr, this message translates to:
  /// **'Terrain'**
  String get court;

  /// No description provided for @category.
  ///
  /// In fr, this message translates to:
  /// **'Catégorie'**
  String get category;

  /// No description provided for @umpireName.
  ///
  /// In fr, this message translates to:
  /// **'Nom de l’arbitre'**
  String get umpireName;

  /// No description provided for @umpire.
  ///
  /// In fr, this message translates to:
  /// **'Arbitre'**
  String get umpire;

  /// No description provided for @serviceJudge.
  ///
  /// In fr, this message translates to:
  /// **'Juge de Service'**
  String get serviceJudge;

  /// No description provided for @serviceJudgeName.
  ///
  /// In fr, this message translates to:
  /// **'Nom du juge de service'**
  String get serviceJudgeName;

  /// No description provided for @umpireInformation.
  ///
  /// In fr, this message translates to:
  /// **'Arbitrage'**
  String get umpireInformation;

  /// No description provided for @newMatchDescription.
  ///
  /// In fr, this message translates to:
  /// **'Configurer et démarrer un nouveau match.'**
  String get newMatchDescription;

  /// No description provided for @history.
  ///
  /// In fr, this message translates to:
  /// **'Historique'**
  String get history;

  /// No description provided for @historyDescription.
  ///
  /// In fr, this message translates to:
  /// **'Consulter les matchs enregistrés.'**
  String get historyDescription;

  /// No description provided for @matchHistory.
  ///
  /// In fr, this message translates to:
  /// **'Historique des matchs'**
  String get matchHistory;

  /// No description provided for @matchDetails.
  ///
  /// In fr, this message translates to:
  /// **'Détails du match'**
  String get matchDetails;

  /// No description provided for @noMatchHistory.
  ///
  /// In fr, this message translates to:
  /// **'Aucun match enregistré.'**
  String get noMatchHistory;

  /// No description provided for @menSingles.
  ///
  /// In fr, this message translates to:
  /// **'Simple Homme'**
  String get menSingles;

  /// No description provided for @womenSingles.
  ///
  /// In fr, this message translates to:
  /// **'Simple Dame'**
  String get womenSingles;

  /// No description provided for @menDoubles.
  ///
  /// In fr, this message translates to:
  /// **'Double Homme'**
  String get menDoubles;

  /// No description provided for @womenDoubles.
  ///
  /// In fr, this message translates to:
  /// **'Double Dame'**
  String get womenDoubles;

  /// No description provided for @mixedDoubles.
  ///
  /// In fr, this message translates to:
  /// **'Double Mixte'**
  String get mixedDoubles;

  /// No description provided for @team.
  ///
  /// In fr, this message translates to:
  /// **'l’équipe'**
  String get team;

  /// No description provided for @teamA.
  ///
  /// In fr, this message translates to:
  /// **'Équipe A'**
  String get teamA;

  /// No description provided for @teamB.
  ///
  /// In fr, this message translates to:
  /// **'Équipe B'**
  String get teamB;

  /// No description provided for @leftTeam.
  ///
  /// In fr, this message translates to:
  /// **'Équipe gauche'**
  String get leftTeam;

  /// No description provided for @rightTeam.
  ///
  /// In fr, this message translates to:
  /// **'Équipe droite'**
  String get rightTeam;

  /// No description provided for @left.
  ///
  /// In fr, this message translates to:
  /// **'Gauche'**
  String get left;

  /// No description provided for @right.
  ///
  /// In fr, this message translates to:
  /// **'Droite'**
  String get right;

  /// No description provided for @leftSide.
  ///
  /// In fr, this message translates to:
  /// **'Côté gauche'**
  String get leftSide;

  /// No description provided for @rightSide.
  ///
  /// In fr, this message translates to:
  /// **'Côté droit'**
  String get rightSide;

  /// No description provided for @startingSide.
  ///
  /// In fr, this message translates to:
  /// **'Position des équipes'**
  String get startingSide;

  /// No description provided for @chooseLeftTeam.
  ///
  /// In fr, this message translates to:
  /// **'Choisis l’équipe située à droite.'**
  String get chooseLeftTeam;

  /// No description provided for @switchSides.
  ///
  /// In fr, this message translates to:
  /// **'Changer de demi-terrain'**
  String get switchSides;

  /// No description provided for @player.
  ///
  /// In fr, this message translates to:
  /// **'Joueur'**
  String get player;

  /// No description provided for @playerOne.
  ///
  /// In fr, this message translates to:
  /// **'Joueur 1'**
  String get playerOne;

  /// No description provided for @playerTwo.
  ///
  /// In fr, this message translates to:
  /// **'Joueur 2'**
  String get playerTwo;

  /// No description provided for @server.
  ///
  /// In fr, this message translates to:
  /// **'Serveur'**
  String get server;

  /// No description provided for @receiver.
  ///
  /// In fr, this message translates to:
  /// **'Receveur'**
  String get receiver;

  /// No description provided for @chooseServerReceiver.
  ///
  /// In fr, this message translates to:
  /// **'Choisis le serveur et le receveur'**
  String get chooseServerReceiver;

  /// No description provided for @positionsOnlyAtZero.
  ///
  /// In fr, this message translates to:
  /// **'La correction des positions est possible uniquement à 0–0. Annule d’abord les points joués pour revenir à 0–0.'**
  String get positionsOnlyAtZero;

  /// No description provided for @set.
  ///
  /// In fr, this message translates to:
  /// **'Set'**
  String get set;

  /// No description provided for @sets.
  ///
  /// In fr, this message translates to:
  /// **'Sets'**
  String get sets;

  /// No description provided for @setPoint.
  ///
  /// In fr, this message translates to:
  /// **'POINT DE SET'**
  String get setPoint;

  /// No description provided for @matchPoint.
  ///
  /// In fr, this message translates to:
  /// **'POINT DE MATCH'**
  String get matchPoint;

  /// No description provided for @undoLastPoint.
  ///
  /// In fr, this message translates to:
  /// **'Annuler le dernier point'**
  String get undoLastPoint;

  /// No description provided for @noPointToUndo.
  ///
  /// In fr, this message translates to:
  /// **'Aucun point à annuler.'**
  String get noPointToUndo;

  /// No description provided for @nextSet.
  ///
  /// In fr, this message translates to:
  /// **'Set suivant'**
  String get nextSet;

  /// No description provided for @startSet.
  ///
  /// In fr, this message translates to:
  /// **'Commencer le set'**
  String get startSet;

  /// No description provided for @shuttlesUsed.
  ///
  /// In fr, this message translates to:
  /// **'Volants utilisés'**
  String get shuttlesUsed;

  /// No description provided for @shuttles.
  ///
  /// In fr, this message translates to:
  /// **'Volants'**
  String get shuttles;

  /// No description provided for @midSetInterval.
  ///
  /// In fr, this message translates to:
  /// **'Pause à 8 points'**
  String get midSetInterval;

  /// No description provided for @endOfSet.
  ///
  /// In fr, this message translates to:
  /// **'Fin du set'**
  String get endOfSet;

  /// No description provided for @finishInterval.
  ///
  /// In fr, this message translates to:
  /// **'Terminer la pause'**
  String get finishInterval;

  /// No description provided for @resumeMatch.
  ///
  /// In fr, this message translates to:
  /// **'Reprendre le match'**
  String get resumeMatch;

  /// No description provided for @umpireAnnouncement.
  ///
  /// In fr, this message translates to:
  /// **'Annonce de l’arbitre'**
  String get umpireAnnouncement;

  /// No description provided for @firstSetAnnouncement.
  ///
  /// In fr, this message translates to:
  /// **'Set! Premier set remporté par {winner}, {score}. Changez de demi-terrain.'**
  String firstSetAnnouncement(String winner, String score);

  /// No description provided for @secondSetAnnouncement.
  ///
  /// In fr, this message translates to:
  /// **'Set! Deuxième set remporté par {winner}, {score}. Changez de demi-terrain.'**
  String secondSetAnnouncement(String winner, String score);

  /// No description provided for @oneGameAll.
  ///
  /// In fr, this message translates to:
  /// **'Un set égalité.'**
  String get oneGameAll;

  /// No description provided for @twentySecondsAnnouncement.
  ///
  /// In fr, this message translates to:
  /// **'Terrain {courtNumber}, 20 secondes.'**
  String twentySecondsAnnouncement(String courtNumber);

  /// No description provided for @matchAnnouncement.
  ///
  /// In fr, this message translates to:
  /// **'Jeu! Match remporté par {winner}, {scores}.'**
  String matchAnnouncement(String winner, String scores);

  /// No description provided for @readyToPlayAnnouncement.
  ///
  /// In fr, this message translates to:
  /// **'Prêt à jouer.'**
  String get readyToPlayAnnouncement;

  /// No description provided for @firstGameSinglesStartAnnouncement.
  ///
  /// In fr, this message translates to:
  /// **'Mesdames et Messieurs à ma droite, {right} et à ma gauche, {left}. {server} au service zéro égalité jouez.'**
  String firstGameSinglesStartAnnouncement(
    String right,
    String left,
    String server,
  );

  /// No description provided for @firstGameDoublesStartAnnouncement.
  ///
  /// In fr, this message translates to:
  /// **'Mesdames et Messieurs à ma droite, {right} et à ma gauche, {left}. {server} au service sur {receiver} zéro égalité jouez.'**
  String firstGameDoublesStartAnnouncement(
    String right,
    String left,
    String server,
    String receiver,
  );

  /// No description provided for @secondGameStartAnnouncement.
  ///
  /// In fr, this message translates to:
  /// **'Second set zéro égalité jouez.'**
  String get secondGameStartAnnouncement;

  /// No description provided for @finalGameStartAnnouncement.
  ///
  /// In fr, this message translates to:
  /// **'Dernier set zéro égalité jouez.'**
  String get finalGameStartAnnouncement;

  /// No description provided for @matchFinished.
  ///
  /// In fr, this message translates to:
  /// **'Match terminé'**
  String get matchFinished;

  /// No description provided for @winner.
  ///
  /// In fr, this message translates to:
  /// **'Vainqueur'**
  String get winner;

  /// No description provided for @duration.
  ///
  /// In fr, this message translates to:
  /// **'Durée :'**
  String get duration;

  /// No description provided for @finishMatch.
  ///
  /// In fr, this message translates to:
  /// **'Terminer le match'**
  String get finishMatch;

  /// No description provided for @matchActions.
  ///
  /// In fr, this message translates to:
  /// **'Actions du match'**
  String get matchActions;

  /// No description provided for @actions.
  ///
  /// In fr, this message translates to:
  /// **'Actions'**
  String get actions;

  /// No description provided for @matchInterruption.
  ///
  /// In fr, this message translates to:
  /// **'Interruption du match'**
  String get matchInterruption;

  /// No description provided for @callMedicalService.
  ///
  /// In fr, this message translates to:
  /// **'Appeler le service médical'**
  String get callMedicalService;

  /// No description provided for @callScoringTable.
  ///
  /// In fr, this message translates to:
  /// **'Appeler la table de marque'**
  String get callScoringTable;

  /// No description provided for @callReferee.
  ///
  /// In fr, this message translates to:
  /// **'Appeler le juge-arbitre'**
  String get callReferee;

  /// No description provided for @correctPlayerPositions.
  ///
  /// In fr, this message translates to:
  /// **'Corriger les positions'**
  String get correctPlayerPositions;

  /// No description provided for @assistance.
  ///
  /// In fr, this message translates to:
  /// **'Assistance'**
  String get assistance;

  /// No description provided for @incidents.
  ///
  /// In fr, this message translates to:
  /// **'Incidents'**
  String get incidents;

  /// No description provided for @corrections.
  ///
  /// In fr, this message translates to:
  /// **'Corrections'**
  String get corrections;

  /// No description provided for @match.
  ///
  /// In fr, this message translates to:
  /// **'Match'**
  String get match;

  /// No description provided for @injury.
  ///
  /// In fr, this message translates to:
  /// **'Blessure'**
  String get injury;

  /// No description provided for @warningOrCard.
  ///
  /// In fr, this message translates to:
  /// **'Avertissement ou carton'**
  String get warningOrCard;

  /// No description provided for @sanction.
  ///
  /// In fr, this message translates to:
  /// **'Sanction'**
  String get sanction;

  /// No description provided for @warning.
  ///
  /// In fr, this message translates to:
  /// **'Avertissement'**
  String get warning;

  /// No description provided for @yellowCard.
  ///
  /// In fr, this message translates to:
  /// **'Carton jaune'**
  String get yellowCard;

  /// No description provided for @redCard.
  ///
  /// In fr, this message translates to:
  /// **'Carton rouge'**
  String get redCard;

  /// No description provided for @blackCard.
  ///
  /// In fr, this message translates to:
  /// **'Carton noir'**
  String get blackCard;

  /// No description provided for @confirmSanction.
  ///
  /// In fr, this message translates to:
  /// **'Confirmer la sanction'**
  String get confirmSanction;

  /// No description provided for @irreversibleAction.
  ///
  /// In fr, this message translates to:
  /// **'Cette action est irréversible. Vérifiez les informations avant de confirmer.'**
  String get irreversibleAction;

  /// No description provided for @retirement.
  ///
  /// In fr, this message translates to:
  /// **'Abandon'**
  String get retirement;

  /// No description provided for @selectRetiringPlayer.
  ///
  /// In fr, this message translates to:
  /// **'Sélectionnez le joueur qui abandonne.'**
  String get selectRetiringPlayer;

  /// No description provided for @confirmRetirement.
  ///
  /// In fr, this message translates to:
  /// **'Confirmer l’abandon'**
  String get confirmRetirement;

  /// No description provided for @retirementConfirmationMessage.
  ///
  /// In fr, this message translates to:
  /// **'Cet abandon terminera immédiatement le match. {winner} sera déclaré vainqueur. Cette action est irréversible.'**
  String retirementConfirmationMessage(String winner);

  /// No description provided for @playerColors.
  ///
  /// In fr, this message translates to:
  /// **'Couleurs des joueurs'**
  String get playerColors;

  /// No description provided for @colorsOf.
  ///
  /// In fr, this message translates to:
  /// **'Couleur de'**
  String get colorsOf;

  /// No description provided for @editPlayerColors.
  ///
  /// In fr, this message translates to:
  /// **'Modifier les couleurs'**
  String get editPlayerColors;

  /// No description provided for @sameColor.
  ///
  /// In fr, this message translates to:
  /// **'Même couleur'**
  String get sameColor;

  /// No description provided for @apply.
  ///
  /// In fr, this message translates to:
  /// **'Appliquer'**
  String get apply;

  /// No description provided for @back.
  ///
  /// In fr, this message translates to:
  /// **'Retour'**
  String get back;

  /// No description provided for @home.
  ///
  /// In fr, this message translates to:
  /// **'Accueil'**
  String get home;

  /// No description provided for @cancel.
  ///
  /// In fr, this message translates to:
  /// **'Annuler'**
  String get cancel;

  /// No description provided for @confirm.
  ///
  /// In fr, this message translates to:
  /// **'Confirmer'**
  String get confirm;

  /// No description provided for @continueLabel.
  ///
  /// In fr, this message translates to:
  /// **'Continuer'**
  String get continueLabel;

  /// No description provided for @start.
  ///
  /// In fr, this message translates to:
  /// **'Démarrer'**
  String get start;

  /// No description provided for @finish.
  ///
  /// In fr, this message translates to:
  /// **'Terminer'**
  String get finish;

  /// No description provided for @optional.
  ///
  /// In fr, this message translates to:
  /// **'Facultatif'**
  String get optional;

  /// No description provided for @requiredField.
  ///
  /// In fr, this message translates to:
  /// **'Champ obligatoire'**
  String get requiredField;

  /// No description provided for @notProvided.
  ///
  /// In fr, this message translates to:
  /// **'Non renseigné'**
  String get notProvided;

  /// No description provided for @startTime.
  ///
  /// In fr, this message translates to:
  /// **'Début'**
  String get startTime;

  /// No description provided for @endTime.
  ///
  /// In fr, this message translates to:
  /// **'Fin'**
  String get endTime;

  /// No description provided for @actionNotImplemented.
  ///
  /// In fr, this message translates to:
  /// **'Action « {title} » à implémenter.'**
  String actionNotImplemented(String title);

  /// No description provided for @checkServerReceiver.
  ///
  /// In fr, this message translates to:
  /// **'Vérifiez le serveur et le receveur avant de commencer le set.'**
  String get checkServerReceiver;

  /// No description provided for @confirmAndStartSet.
  ///
  /// In fr, this message translates to:
  /// **'Valider et commencer le set'**
  String get confirmAndStartSet;

  /// No description provided for @selected.
  ///
  /// In fr, this message translates to:
  /// **'sélectionné(s)'**
  String get selected;

  /// No description provided for @selectMatches.
  ///
  /// In fr, this message translates to:
  /// **'Sélectionner des matchs'**
  String get selectMatches;

  /// No description provided for @clearHistory.
  ///
  /// In fr, this message translates to:
  /// **'Supprimer l’historique'**
  String get clearHistory;

  /// No description provided for @clearHistoryConfirmation.
  ///
  /// In fr, this message translates to:
  /// **'Êtes-vous sûr de vouloir supprimer tous les matchs enregistrés ?'**
  String get clearHistoryConfirmation;

  /// No description provided for @delete.
  ///
  /// In fr, this message translates to:
  /// **'Supprimer'**
  String get delete;

  /// No description provided for @deleteSelectedMatches.
  ///
  /// In fr, this message translates to:
  /// **'Supprimer les matchs'**
  String get deleteSelectedMatches;

  /// No description provided for @deleteSelectedMatchesConfirmation.
  ///
  /// In fr, this message translates to:
  /// **'Supprimer les {count} matchs sélectionnés ?'**
  String deleteSelectedMatchesConfirmation(int count);

  /// No description provided for @warmup.
  ///
  /// In fr, this message translates to:
  /// **'Échauffement'**
  String get warmup;

  /// No description provided for @startWarmup.
  ///
  /// In fr, this message translates to:
  /// **'Démarrer l’échauffement'**
  String get startWarmup;

  /// No description provided for @skipWarmup.
  ///
  /// In fr, this message translates to:
  /// **'Passer l’échauffement'**
  String get skipWarmup;

  /// No description provided for @matchClassification.
  ///
  /// In fr, this message translates to:
  /// **'Classification du match'**
  String get matchClassification;

  /// No description provided for @matchClassificationDescription.
  ///
  /// In fr, this message translates to:
  /// **'Définissez la catégorie et les informations de compétition.'**
  String get matchClassificationDescription;

  /// No description provided for @ageCategory.
  ///
  /// In fr, this message translates to:
  /// **'Catégorie d’âge'**
  String get ageCategory;

  /// No description provided for @ageCategoryHint.
  ///
  /// In fr, this message translates to:
  /// **'Ex. Minime Fille'**
  String get ageCategoryHint;

  /// No description provided for @poolGroup.
  ///
  /// In fr, this message translates to:
  /// **'Poule / Groupe'**
  String get poolGroup;

  /// No description provided for @poolGroupHint.
  ///
  /// In fr, this message translates to:
  /// **'Ex. Poule B'**
  String get poolGroupHint;

  /// No description provided for @tournamentPhase.
  ///
  /// In fr, this message translates to:
  /// **'Phase'**
  String get tournamentPhase;

  /// No description provided for @tournamentPhaseHint.
  ///
  /// In fr, this message translates to:
  /// **'Ex. Quart de finale'**
  String get tournamentPhaseHint;

  /// No description provided for @agePoussin.
  ///
  /// In fr, this message translates to:
  /// **'Poussin'**
  String get agePoussin;

  /// No description provided for @agePoussine.
  ///
  /// In fr, this message translates to:
  /// **'Poussine'**
  String get agePoussine;

  /// No description provided for @ageBenjamin.
  ///
  /// In fr, this message translates to:
  /// **'Benjamin'**
  String get ageBenjamin;

  /// No description provided for @ageBenjamine.
  ///
  /// In fr, this message translates to:
  /// **'Benjamine'**
  String get ageBenjamine;

  /// No description provided for @ageMinimeBoy.
  ///
  /// In fr, this message translates to:
  /// **'Minime Garçon'**
  String get ageMinimeBoy;

  /// No description provided for @ageMinimeGirl.
  ///
  /// In fr, this message translates to:
  /// **'Minime Fille'**
  String get ageMinimeGirl;

  /// No description provided for @ageCadet.
  ///
  /// In fr, this message translates to:
  /// **'Cadet'**
  String get ageCadet;

  /// No description provided for @ageCadette.
  ///
  /// In fr, this message translates to:
  /// **'Cadette'**
  String get ageCadette;

  /// No description provided for @ageJuniorBoy.
  ///
  /// In fr, this message translates to:
  /// **'Junior Garçon'**
  String get ageJuniorBoy;

  /// No description provided for @ageJuniorGirl.
  ///
  /// In fr, this message translates to:
  /// **'Junior Fille'**
  String get ageJuniorGirl;

  /// No description provided for @ageSeniorSeries1.
  ///
  /// In fr, this message translates to:
  /// **'Senior Série 1'**
  String get ageSeniorSeries1;

  /// No description provided for @ageSeniorSeries2.
  ///
  /// In fr, this message translates to:
  /// **'Senior Série 2'**
  String get ageSeniorSeries2;

  /// No description provided for @ageSeniorSeries3.
  ///
  /// In fr, this message translates to:
  /// **'Senior Série 3'**
  String get ageSeniorSeries3;

  /// No description provided for @poolUnique.
  ///
  /// In fr, this message translates to:
  /// **'Poule unique'**
  String get poolUnique;

  /// No description provided for @poolA.
  ///
  /// In fr, this message translates to:
  /// **'Poule A'**
  String get poolA;

  /// No description provided for @poolB.
  ///
  /// In fr, this message translates to:
  /// **'Poule B'**
  String get poolB;

  /// No description provided for @poolC.
  ///
  /// In fr, this message translates to:
  /// **'Poule C'**
  String get poolC;

  /// No description provided for @poolD.
  ///
  /// In fr, this message translates to:
  /// **'Poule D'**
  String get poolD;

  /// No description provided for @phasePoolMatch.
  ///
  /// In fr, this message translates to:
  /// **'Match de poule'**
  String get phasePoolMatch;

  /// No description provided for @phaseRoundOf16.
  ///
  /// In fr, this message translates to:
  /// **'Huitième de finale'**
  String get phaseRoundOf16;

  /// No description provided for @phaseQuarterFinal.
  ///
  /// In fr, this message translates to:
  /// **'Quart de finale'**
  String get phaseQuarterFinal;

  /// No description provided for @phaseSemiFinal.
  ///
  /// In fr, this message translates to:
  /// **'Demi-finale'**
  String get phaseSemiFinal;

  /// No description provided for @phaseFinal.
  ///
  /// In fr, this message translates to:
  /// **'Finale'**
  String get phaseFinal;

  /// No description provided for @phaseClassification.
  ///
  /// In fr, this message translates to:
  /// **'Match de classement'**
  String get phaseClassification;

  /// No description provided for @settings.
  ///
  /// In fr, this message translates to:
  /// **'Paramètres'**
  String get settings;

  /// No description provided for @matchSettings.
  ///
  /// In fr, this message translates to:
  /// **'Match'**
  String get matchSettings;

  /// No description provided for @umpireAnnouncements.
  ///
  /// In fr, this message translates to:
  /// **'Annonces de l\'arbitre'**
  String get umpireAnnouncements;

  /// No description provided for @umpireAnnouncementsDescription.
  ///
  /// In fr, this message translates to:
  /// **'Afficher les annonces à prononcer par l\'arbitre pendant le match'**
  String get umpireAnnouncementsDescription;

  /// Sync indicator: every point is confirmed by the server
  ///
  /// In fr, this message translates to:
  /// **'À jour'**
  String get scoreSyncUpToDate;

  /// Sync indicator: points waiting to be sent, server reachable
  ///
  /// In fr, this message translates to:
  /// **'{count} en attente'**
  String scoreSyncPending(int count);

  /// Sync indicator: server unreachable, points kept on the tablet
  ///
  /// In fr, this message translates to:
  /// **'Hors ligne · {count} en attente'**
  String scoreSyncOffline(int count);

  /// Sync indicator: the server refused the points of a match
  ///
  /// In fr, this message translates to:
  /// **'Envoi bloqué'**
  String get scoreSyncBlocked;

  /// No description provided for @scoreSyncUpToDateTooltip.
  ///
  /// In fr, this message translates to:
  /// **'Tous les points sont enregistrés sur le serveur.'**
  String get scoreSyncUpToDateTooltip;

  /// No description provided for @scoreSyncPendingTooltip.
  ///
  /// In fr, this message translates to:
  /// **'Les points sont gardés sur la tablette et envoyés au serveur.'**
  String get scoreSyncPendingTooltip;

  /// No description provided for @scoreSyncOfflineTooltip.
  ///
  /// In fr, this message translates to:
  /// **'Le serveur est injoignable. Les points sont gardés sur la tablette et seront envoyés au retour du réseau.'**
  String get scoreSyncOfflineTooltip;

  /// No description provided for @scoreSyncBlockedTooltip.
  ///
  /// In fr, this message translates to:
  /// **'Le serveur refuse les points d\'un match. Ils sont gardés sur la tablette : prévenez l\'organisateur.'**
  String get scoreSyncBlockedTooltip;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en', 'fr'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
    case 'fr':
      return AppLocalizationsFr();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
