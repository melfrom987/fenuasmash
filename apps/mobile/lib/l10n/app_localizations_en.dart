// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'Badminton Umpire';

  @override
  String get language => 'Language';

  @override
  String get french => 'French';

  @override
  String get english => 'English';

  @override
  String get newMatch => 'New match';

  @override
  String get matchSetupDescription =>
      'Configure the players and the initial service.';

  @override
  String get startMatch => 'Start match';

  @override
  String get tournamentName => 'Tournament name';

  @override
  String get defaultTournamentName => 'Tournament';

  @override
  String get courtNumber => 'Court number';

  @override
  String get court => 'Court';

  @override
  String get category => 'Category';

  @override
  String get umpireName => 'Umpire name';

  @override
  String get umpire => 'Umpire';

  @override
  String get serviceJudge => 'Service judge';

  @override
  String get serviceJudgeName => 'Service judge name';

  @override
  String get umpireInformation => 'Officials';

  @override
  String get newMatchDescription => 'Configure and start a new match.';

  @override
  String get history => 'History';

  @override
  String get historyDescription => 'View saved matches.';

  @override
  String get matchHistory => 'Match history';

  @override
  String get matchDetails => 'Match details';

  @override
  String get noMatchHistory => 'No saved matches.';

  @override
  String get menSingles => 'Men\'s Singles';

  @override
  String get womenSingles => 'Women\'s Singles';

  @override
  String get menDoubles => 'Men\'s Doubles';

  @override
  String get womenDoubles => 'Women\'s Doubles';

  @override
  String get mixedDoubles => 'Mixed Doubles';

  @override
  String get team => 'team';

  @override
  String get teamA => 'Team A';

  @override
  String get teamB => 'Team B';

  @override
  String get leftTeam => 'Left team';

  @override
  String get rightTeam => 'Right team';

  @override
  String get left => 'Left';

  @override
  String get right => 'Right';

  @override
  String get leftSide => 'Left side';

  @override
  String get rightSide => 'Right side';

  @override
  String get startingSide => 'Team position';

  @override
  String get chooseLeftTeam => 'Select the team on the right.';

  @override
  String get switchSides => 'Change ends';

  @override
  String get player => 'Player';

  @override
  String get playerOne => 'Player 1';

  @override
  String get playerTwo => 'Player 2';

  @override
  String get server => 'Server';

  @override
  String get receiver => 'Receiver';

  @override
  String get chooseServerReceiver => 'Choose the server and receiver';

  @override
  String get positionsOnlyAtZero =>
      'Positions can only be corrected at 0–0. Undo the played points first to return to 0–0.';

  @override
  String get set => 'Game';

  @override
  String get sets => 'Games';

  @override
  String get setPoint => 'GAME POINT';

  @override
  String get matchPoint => 'MATCH POINT';

  @override
  String get undoLastPoint => 'Undo last point';

  @override
  String get noPointToUndo => 'No point to undo.';

  @override
  String get nextSet => 'Next game';

  @override
  String get startSet => 'Start game';

  @override
  String get shuttlesUsed => 'Shuttles used';

  @override
  String get shuttles => 'Shuttles';

  @override
  String get midSetInterval => 'Mid-game interval';

  @override
  String get endOfSet => 'End of game';

  @override
  String get finishInterval => 'End interval';

  @override
  String get resumeMatch => 'Resume match';

  @override
  String get umpireAnnouncement => 'Umpire announcement';

  @override
  String firstSetAnnouncement(String winner, String score) {
    return 'Game! First game won by $winner, $score. Change ends.';
  }

  @override
  String secondSetAnnouncement(String winner, String score) {
    return 'Game! Second game won by $winner, $score. Change ends.';
  }

  @override
  String get oneGameAll => 'One game all.';

  @override
  String twentySecondsAnnouncement(String courtNumber) {
    return 'Court $courtNumber, 20 seconds.';
  }

  @override
  String matchAnnouncement(String winner, String scores) {
    return 'Game! Match won by $winner, $scores.';
  }

  @override
  String get readyToPlayAnnouncement => 'Ready to play.';

  @override
  String firstGameSinglesStartAnnouncement(
    String right,
    String left,
    String server,
  ) {
    return 'Ladies and Gentlemen on my right, $right and on my left, $left. $server to serve love all play.';
  }

  @override
  String firstGameDoublesStartAnnouncement(
    String right,
    String left,
    String server,
    String receiver,
  ) {
    return 'Ladies and Gentlemen on my right, $right and on my left, $left. $server to serve to $receiver love all play.';
  }

  @override
  String get secondGameStartAnnouncement => 'Second game love all play.';

  @override
  String get finalGameStartAnnouncement => 'Final game love all play.';

  @override
  String get matchFinished => 'Match finished';

  @override
  String get winner => 'Winner';

  @override
  String get duration => 'Duration:';

  @override
  String get finishMatch => 'Finish match';

  @override
  String get matchActions => 'Match actions';

  @override
  String get actions => 'Actions';

  @override
  String get matchInterruption => 'Match interruption';

  @override
  String get callMedicalService => 'Call medical staff';

  @override
  String get callScoringTable => 'Call scorer\'s table';

  @override
  String get callReferee => 'Call referee';

  @override
  String get correctPlayerPositions => 'Correct player positions';

  @override
  String get assistance => 'Assistance';

  @override
  String get incidents => 'Incidents';

  @override
  String get corrections => 'Corrections';

  @override
  String get match => 'Match';

  @override
  String get injury => 'Injury';

  @override
  String get warningOrCard => 'Warning or card';

  @override
  String get sanction => 'Sanction';

  @override
  String get warning => 'Warning';

  @override
  String get yellowCard => 'Yellow card';

  @override
  String get redCard => 'Red card';

  @override
  String get blackCard => 'Black card';

  @override
  String get confirmSanction => 'Confirm sanction';

  @override
  String get irreversibleAction =>
      'This action cannot be undone. Check the information before confirming.';

  @override
  String get retirement => 'Retirement';

  @override
  String get selectRetiringPlayer => 'Select the player who is retiring.';

  @override
  String get confirmRetirement => 'Confirm retirement';

  @override
  String retirementConfirmationMessage(String winner) {
    return 'This retirement will immediately end the match. $winner will be declared the winner. This action cannot be undone.';
  }

  @override
  String get playerColors => 'Player colors';

  @override
  String get colorsOf => 'Color of';

  @override
  String get editPlayerColors => 'Edit player colors';

  @override
  String get sameColor => 'Same color';

  @override
  String get apply => 'Apply';

  @override
  String get back => 'Back';

  @override
  String get home => 'Home';

  @override
  String get cancel => 'Cancel';

  @override
  String get confirm => 'Confirm';

  @override
  String get continueLabel => 'Continue';

  @override
  String get start => 'Start';

  @override
  String get finish => 'Finish';

  @override
  String get optional => 'Optional';

  @override
  String get requiredField => 'Required field';

  @override
  String get notProvided => 'Not provided';

  @override
  String get startTime => 'Start';

  @override
  String get endTime => 'End';

  @override
  String actionNotImplemented(String title) {
    return 'Action \"$title\" is not implemented yet.';
  }

  @override
  String get checkServerReceiver =>
      'Check the server and receiver before starting the game.';

  @override
  String get confirmAndStartSet => 'Confirm and start game';

  @override
  String get selected => 'selected';

  @override
  String get selectMatches => 'Select matches';

  @override
  String get clearHistory => 'Clear history';

  @override
  String get clearHistoryConfirmation =>
      'Are you sure you want to delete all saved matches?';

  @override
  String get delete => 'Delete';

  @override
  String get deleteSelectedMatches => 'Delete matches';

  @override
  String deleteSelectedMatchesConfirmation(int count) {
    return 'Delete the $count selected matches?';
  }

  @override
  String get warmup => 'Warm-up';

  @override
  String get startWarmup => 'Start warm-up';

  @override
  String get skipWarmup => 'Skip warm-up';

  @override
  String get matchClassification => 'Match classification';

  @override
  String get matchClassificationDescription =>
      'Define the category and competition information.';

  @override
  String get ageCategory => 'Age category';

  @override
  String get ageCategoryHint => 'e.g. U15 Girls';

  @override
  String get poolGroup => 'Pool / Group';

  @override
  String get poolGroupHint => 'e.g. Pool B';

  @override
  String get tournamentPhase => 'Stage';

  @override
  String get tournamentPhaseHint => 'e.g. Quarter-final';

  @override
  String get agePoussin => 'Poussin';

  @override
  String get agePoussine => 'Poussine';

  @override
  String get ageBenjamin => 'Benjamin';

  @override
  String get ageBenjamine => 'Benjamine';

  @override
  String get ageMinimeBoy => 'Minime Boys';

  @override
  String get ageMinimeGirl => 'Minime Girls';

  @override
  String get ageCadet => 'Cadet';

  @override
  String get ageCadette => 'Cadette';

  @override
  String get ageJuniorBoy => 'Junior Boys';

  @override
  String get ageJuniorGirl => 'Junior Girls';

  @override
  String get ageSeniorSeries1 => 'Senior Series 1';

  @override
  String get ageSeniorSeries2 => 'Senior Series 2';

  @override
  String get ageSeniorSeries3 => 'Senior Series 3';

  @override
  String get poolUnique => 'Single pool';

  @override
  String get poolA => 'Pool A';

  @override
  String get poolB => 'Pool B';

  @override
  String get poolC => 'Pool C';

  @override
  String get poolD => 'Pool D';

  @override
  String get phasePoolMatch => 'Pool match';

  @override
  String get phaseRoundOf16 => 'Round of 16';

  @override
  String get phaseQuarterFinal => 'Quarter-final';

  @override
  String get phaseSemiFinal => 'Semi-final';

  @override
  String get phaseFinal => 'Final';

  @override
  String get phaseClassification => 'Classification match';

  @override
  String get settings => 'Settings';

  @override
  String get matchSettings => 'Match';

  @override
  String get umpireAnnouncements => 'Umpire announcements';

  @override
  String get umpireAnnouncementsDescription =>
      'Display announcements to be spoken by the umpire during the match';

  @override
  String get scoreSyncUpToDate => 'Up to date';

  @override
  String scoreSyncPending(int count) {
    return '$count pending';
  }

  @override
  String scoreSyncOffline(int count) {
    return 'Offline · $count pending';
  }

  @override
  String get scoreSyncBlocked => 'Sending blocked';

  @override
  String get scoreSyncUpToDateTooltip => 'Every point is saved on the server.';

  @override
  String get scoreSyncPendingTooltip =>
      'Points are kept on the tablet and sent to the server.';

  @override
  String get scoreSyncOfflineTooltip =>
      'The server cannot be reached. Points are kept on the tablet and will be sent when the network is back.';

  @override
  String get scoreSyncBlockedTooltip =>
      'The server refuses the points of a match. They are kept on the tablet: tell the organizer.';

  @override
  String get pairingTitle => 'Pair the tablet';

  @override
  String get pairingDescription =>
      'Type the 6-character code shown by the organizer on the court page. It is valid for 10 minutes.';

  @override
  String get pairingCode => 'Pairing code';

  @override
  String get pairingServer => 'Server address';

  @override
  String get pairingDeviceName => 'Tablet name (optional)';

  @override
  String get pairingSubmit => 'Pair';

  @override
  String get pairingCodeLength => 'The code has 6 characters.';

  @override
  String get pairingServerInvalid =>
      'Invalid address, for example http://192.168.1.10:8081.';

  @override
  String get pairingInvalidCode =>
      'Wrong, expired or already used code. Ask the organizer for a new one.';

  @override
  String pairingTooManyAttempts(int minutes) {
    return 'Too many wrong codes. Try again in $minutes min.';
  }

  @override
  String get pairingCourtInactive =>
      'This court is disabled. Ask the organizer to enable it again.';

  @override
  String get pairingUnreachable =>
      'Server unreachable. Check the Wi-Fi and the server address.';

  @override
  String pairingFailed(String code) {
    return 'Pairing failed ($code).';
  }

  @override
  String pairingSuccess(int number) {
    return 'Tablet paired with court $number.';
  }

  @override
  String get tabletPairingLoading => 'Tablet…';

  @override
  String get tabletNotPairedTitle => 'Pair with a court';

  @override
  String get tabletNotPairedDescription =>
      'Link the tablet to a court of the tournament to send the scores.';

  @override
  String tabletPairedTitle(int number) {
    return 'Court $number';
  }

  @override
  String get tabletPairedDescription =>
      'Tablet paired: scores can be sent to the tournament.';

  @override
  String get tabletRevokedTitle => 'Pairing removed';

  @override
  String get tabletRevokedDescription =>
      'The organizer removed this tablet. Type a new code.';

  @override
  String get tabletPairAgain => 'Pair with another court';

  @override
  String get tabletUnpair => 'Unpair';

  @override
  String get tabletUnpairTitle => 'Unpair the tablet?';

  @override
  String get tabletUnpairMessage =>
      'The tablet will not send scores until it gets a new code.';

  @override
  String get courtMatchLoading => 'Looking for the match of the court…';

  @override
  String get courtMatchFreeTitle => 'No match on the court';

  @override
  String get courtMatchFreeDescription =>
      'The match called by the organizer will show up here by itself.';

  @override
  String get courtMatchCalled => 'Match called';

  @override
  String get courtMatchWarmup => 'Warm-up';

  @override
  String get courtMatchInProgress => 'Match in progress';

  @override
  String get courtMatchUnknownEvent => 'Tournament match';

  @override
  String courtMatchRound(int round) {
    return 'Round $round';
  }

  @override
  String get courtMatchOpenHint => 'Tap to type the players and start.';

  @override
  String get courtMatchResumeHint => 'Tap to resume the match.';

  @override
  String courtMatchOffline(String time) {
    return 'Offline: shown as of $time.';
  }

  @override
  String get courtMatchOfflineNever => 'Offline: the server does not answer.';

  @override
  String get courtMatchReplaceTitle => 'Another match is in progress';

  @override
  String get courtMatchReplaceMessage =>
      'Opening the match of the court replaces the match in progress on the tablet. Its points already played will still be sent.';

  @override
  String get courtMatchReplace => 'Open';
}
