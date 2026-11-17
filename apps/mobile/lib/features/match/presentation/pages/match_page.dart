import 'dart:async';

import 'package:badminton_umpire/core/settings/settings.dart';
import 'package:badminton_umpire/core/utils/app_haptics.dart';
import 'package:badminton_umpire/features/history/domain/models/match_history_entry.dart';
import 'package:badminton_umpire/features/history/data/services/match_history_service.dart';
import 'package:badminton_umpire/features/match/application/controllers/match_controller.dart';
import 'package:badminton_umpire/features/match/domain/models/match_config.dart';
import 'package:badminton_umpire/features/match/domain/models/match_draft.dart';
import 'package:badminton_umpire/features/match/domain/models/match_event.dart';
import 'package:badminton_umpire/features/match/domain/models/match_phase.dart';
import 'package:badminton_umpire/features/match/domain/models/match_snapshot.dart';
import 'package:badminton_umpire/features/match/domain/models/score_event.dart';
import 'package:badminton_umpire/features/match/data/services/match_draft_service.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/menus/actions_menu.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/dialogs/correct_positions_dialog.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/dialogs/disciplinary_event_dialog.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/dialogs/retirement_dialog.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/dialogs/umpire_announcement_dialog.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/layouts/match_layout.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/overlays/match_finished_overlay.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/dialogs/player_colors_dialog.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/overlays/next_set_setup_overlay.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/dialogs/player_color_dialog.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/banners/match_interval_banner.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/banners/warmup_banner.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

part 'parts/match_page_actions.dart';
part 'parts/match_page_draft.dart';
part 'parts/match_page_history.dart';
part 'parts/match_page_navigation.dart';

class MatchPage extends StatefulWidget {
  final MatchConfig config;

  final String serverName;
  final String receiverName;
  final String leftSideTeam;

  final MatchDraft? draft;

  final bool autoStartWarmup;
  final Map<String, Color>? initialPlayerColors;

  const MatchPage({
    super.key,
    required this.config,
    required this.serverName,
    required this.receiverName,
    required this.leftSideTeam,
    this.autoStartWarmup = false,
    this.draft,
    this.initialPlayerColors,
  });

  @override
  State<MatchPage> createState() => _MatchPageState();
}

class _MatchPageState extends State<MatchPage> {
  late final MatchController matchController;
  late final Map<String, Color> playerColors = {};
  final MatchHistoryService _historyService = MatchHistoryService();

  bool _historySaved = false;

  Timer? _matchTimer;
  Timer? _warmupTimer;

  Duration matchDuration = Duration.zero;
  Duration warmupRemaining = const Duration(minutes: 2);

  bool _warmupStarted = false;
  bool _readyAnnouncementShown = false;
  bool _matchStarted = false;

  bool _intervalTwentySecondsHandled = false;
  bool _wasIntervalRunning = false;

  int shuttleCount = 1;

  DateTime? _matchStartedAt;
  DateTime? _matchFinishedAt;

  final MatchDraftService _draftService = MatchDraftService();

  DateTime? _warmupEndsAt;

  bool _savingDraft = false;
  bool _draftSavePending = false;
  bool _wasMatchFinished = false;
  late final String _matchId;

  Timer? _draftDebounceTimer;

  Future<void> _notifyWarmupTwentySeconds() async {
    await AppHaptics.countdownWarning();
  }

  void _handleMatchControllerChanged() {
    if (!mounted) {
      return;
    }

    final isFinished = matchController.matchIsFinished;

    if (isFinished) {
      if (_matchFinishedAt == null) {
        setState(() {
          _matchFinishedAt = DateTime.now();
        });
      }

      if (!_wasMatchFinished) {
        _wasMatchFinished = true;

        unawaited(_saveTemporaryMatch());
        unawaited(_saveMatchHistoryIfNeeded());
      }

      return;
    }

    if (_wasMatchFinished) {
      _wasMatchFinished = false;
      _historySaved = false;

      unawaited(_removeMatchFromHistory());
    }

    if (_matchFinishedAt != null) {
      setState(() {
        _matchFinishedAt = null;
      });
    }
  }

  @override
  void initState() {
    super.initState();

    final draft = widget.draft;

    if (draft != null) {
      _matchId = draft.matchId;

      matchController = MatchController.fromDraft(draft);

      playerColors.addAll(draft.playerColors);

      shuttleCount = draft.shuttleCount;

      _matchStarted = draft.matchStarted;
      _matchStartedAt = draft.matchStartedAt;
      _matchFinishedAt = draft.matchFinishedAt;

      _warmupStarted = draft.warmupStarted;
      _warmupEndsAt = draft.warmupEndsAt;

      _wasMatchFinished = draft.phase == MatchPhase.finished;
    } else {
      _matchId = DateTime.now().microsecondsSinceEpoch.toString();

      final servingTeamId =
          widget.config.teamAPlayers.contains(widget.serverName)
          ? 'teamA'
          : 'teamB';

      matchController = MatchController(
        isDoubles: widget.config.isDoubles,
        teamAPlayers: widget.config.teamAPlayers,
        teamBPlayers: widget.config.teamBPlayers,
        leftSideTeamId: widget.leftSideTeam,
        servingTeamId: servingTeamId,
        serverName: widget.serverName,
        receiverName: widget.receiverName,
        courtNumber: widget.config.courtNumber,
      );

      if (widget.initialPlayerColors != null) {
        playerColors.addAll(widget.initialPlayerColors!);
      } else {
        playerColors.addAll({
          for (final player in widget.config.teamAPlayers)
            player: const Color(0xFF7E57C2),
          for (final player in widget.config.teamBPlayers)
            player: const Color(0xFF42A5F5),
        });
      }
    }

    matchController.addListener(_refreshMatchPage);
    matchController.addListener(_handleMatchControllerChanged);

    SystemChrome.setPreferredOrientations([
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);

    if (draft != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          _restoreDraftTimers();
        }
      });
    } else {
      unawaited(_saveTemporaryMatch());
    }

    if (widget.autoStartWarmup) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || _warmupStarted || _matchStarted) {
          return;
        }

        _startWarmup();
      });
    }
  }

  @override
  void dispose() {
    _warmupTimer?.cancel();
    _matchTimer?.cancel();
    _draftDebounceTimer?.cancel();

    matchController.removeListener(_refreshMatchPage);
    matchController.removeListener(_handleMatchControllerChanged);
    matchController.dispose();

    SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
      DeviceOrientation.portraitDown,
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);

    super.dispose();
  }

  void _refreshMatchPage() {
    final intervalIsRunning = matchController.intervalIsRunning;
    if (!mounted) {
      return;
    }

    if (matchController.matchIsFinished) {
      _matchTimer?.cancel();
      _matchTimer = null;

      _warmupTimer?.cancel();
      _warmupTimer = null;
    }

    if (matchController.intervalTwentySecondsReached &&
        !_intervalTwentySecondsHandled) {
      _intervalTwentySecondsHandled = true;

      unawaited(AppHaptics.countdownWarning());
    }

    if (_wasIntervalRunning && !intervalIsRunning) {
      unawaited(AppHaptics.countdownFinished());
    }

    if (!intervalIsRunning) {
      _intervalTwentySecondsHandled = false;
    }

    _wasIntervalRunning = intervalIsRunning;
    setState(() {});

    unawaited(_saveTemporaryMatch());
    _scheduleTemporarySave();
  }

  @override
  Widget build(BuildContext context) {
    final timedLock =
        !_matchStarted ||
        matchController.intervalIsReady ||
        matchController.intervalIsRunning;
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) {
          return;
        }

        await _confirmLeaveMatch();
      },

      child: Scaffold(
        backgroundColor: const Color(0xFFF5F5F8),
        body: SafeArea(
          child: Stack(
            children: [
              Positioned.fill(
                child: MatchLayout(
                  teamAIsOnLeft: matchController.teamAIsOnLeft,
                  courtNumber: widget.config.courtNumber,
                  tournamentName: widget.config.tournamentName,
                  category: widget.config.category,
                  matchDuration: matchDuration,
                  leftPlayers: matchController.leftPlayers,
                  rightPlayers: matchController.rightPlayers,
                  leftTeamName: matchController.leftTeamName,
                  rightTeamName: matchController.rightTeamName,
                  serverName: matchController.serverName,
                  receiverName: matchController.receiverName,
                  ageCategory: widget.config.ageCategory,
                  tournamentPhase: widget.config.tournamentPhase,
                  playerColors: playerColors,
                  leftScore: matchController.leftScore,
                  rightScore: matchController.rightScore,
                  leftSets: matchController.leftSets,
                  rightSets: matchController.rightSets,
                  shuttleCount: shuttleCount,
                  previousSets: matchController.previousSets,
                  controlsLocked: matchController.controlsLocked || timedLock,
                  canUndo: !timedLock && matchController.canUndo,
                  onLeftPoint: matchController.addPointToLeftTeam,
                  onRightPoint: matchController.addPointToRightTeam,
                  onUndo: _undoLastPoint,
                  onIncrementShuttle: timedLock ? null : _incrementShuttleCount,
                  onDecrementShuttle: timedLock ? null : _decrementShuttleCount,
                  onOpenActions: _openActionsMenu,
                  onPlayerTap: _changeSinglePlayerColor,
                  leftIsServing: matchController.leftTeamIsServing,
                  rightIsServing: matchController.rightTeamIsServing,
                  leftHasMatchPoint: matchController.leftHasMatchPoint,
                  rightHasMatchPoint: matchController.rightHasMatchPoint,
                  leftHasSetPoint: matchController.leftHasSetPoint,
                  rightHasSetPoint: matchController.rightHasSetPoint,
                  leftHasImportantPoint: matchController.leftHasImportantPoint,
                  rightHasImportantPoint:
                      matchController.rightHasImportantPoint,
                ),
              ),

              if (!_matchStarted)
                Positioned(
                  top: 8,
                  left: 0,
                  right: 0,
                  child: Center(
                    child: WarmupBanner(
                      remaining: warmupRemaining,
                      started: _warmupStarted,
                      onStart: _startWarmup,
                      onSkip: _skipWarmup,
                    ),
                  ),
                ),

              if (matchController.intervalIsReady ||
                  matchController.intervalIsRunning)
                Positioned(
                  top: 8,
                  left: 0,
                  right: 0,
                  child: Center(
                    child: MatchIntervalBanner(
                      phase: matchController.phase,
                      remaining: matchController.intervalRemaining,
                      courtNumber: matchController.courtNumber,
                      onStart: matchController.startCurrentInterval,
                      onSkip: matchController.finishIntervalEarly,
                    ),
                  ),
                ),

              if (matchController.phase == MatchPhase.preparingNextSet)
                NextSetSetupOverlay(
                  isDoubles: widget.config.isDoubles,
                  leftPlayers: matchController.leftTeam.players,
                  rightPlayers: matchController.rightTeam.players,
                  servingTeamId: matchController.lastSetWinnerTeamId!,
                  leftTeamId: matchController.leftSideTeamId,
                  nextSetNumber: matchController.previousSets.length + 1,
                  onStartNextSet:
                      ({
                        required String serverName,
                        required String receiverName,
                      }) {
                        matchController.startNextSet(
                          selectedServerName: serverName,
                          selectedReceiverName: receiverName,
                        );
                      },
                ),

              if (matchController.phase == MatchPhase.finished)
                MatchFinishedOverlay(
                  teamAPlayers: widget.config.teamAPlayers,
                  teamBPlayers: widget.config.teamBPlayers,
                  winnerName:
                      matchController.teamASets > matchController.teamBSets
                      ? widget.config.teamAPlayers.join(' / ')
                      : widget.config.teamBPlayers.join(' / '),

                  teamASets: matchController.teamASets,
                  teamBSets: matchController.teamBSets,

                  previousSets: matchController.previousSets,

                  matchDuration: matchDuration,

                  shuttleCount: shuttleCount,

                  umpireName: widget.config.umpireName,
                  serviceJudgeName: widget.config.serviceJudgeName,

                  matchStartedAt: _matchStartedAt,
                  matchFinishedAt: _matchFinishedAt,

                  onBack: _goBack,
                  onNewMatch: _startNewMatch,

                  canUndo: matchController.canUndo,
                  onUndo: _undoLastPoint,
                ),
            ],
          ),
        ),
      ),
    );
  }

  void _incrementShuttleCount() {
    setState(() {
      shuttleCount++;
    });

    _scheduleTemporarySave();

    unawaited(_saveTemporaryMatch());
  }

  void _decrementShuttleCount() {
    if (shuttleCount <= 1) {
      return;
    }

    setState(() {
      shuttleCount--;
    });

    unawaited(_saveTemporaryMatch());
  }

  Future<void> _editPlayerColors() async {
    final updatedColors = await showPlayerColorsDialog(
      context: context,
      leftPlayers: matchController.leftTeam.players,
      rightPlayers: matchController.rightTeam.players,
      currentColors: playerColors,
    );

    if (!mounted || updatedColors == null) {
      return;
    }

    setState(() {
      playerColors
        ..clear()
        ..addAll(updatedColors);
    });

    unawaited(_saveTemporaryMatch());
  }

  Future<void> _changeSinglePlayerColor(String playerName) async {
    final currentColor = playerColors[playerName] ?? Colors.grey;

    final selectedColor = await showPlayerColorDialog(
      context: context,
      playerName: playerName,
      currentColor: currentColor,
    );

    if (selectedColor == null) {
      return;
    }

    setState(() {
      playerColors[playerName] = selectedColor;
    });

    unawaited(_saveTemporaryMatch());
  }

  void _restoreDraftTimers() {
    if (_matchStarted && _matchStartedAt != null) {
      matchDuration = DateTime.now().difference(_matchStartedAt!);

      if (!matchController.matchIsFinished) {
        _startMatchTimer();
      }

      setState(() {});
      return;
    }

    if (!_warmupStarted) {
      return;
    }

    final end = _warmupEndsAt;

    if (end == null) {
      warmupRemaining = Duration.zero;
      return;
    }

    final remaining = end.difference(DateTime.now());

    if (remaining <= Duration.zero) {
      warmupRemaining = Duration.zero;

      setState(() {});

      _finishWarmup();
      return;
    }

    warmupRemaining = remaining;

    setState(() {});

    _startWarmupTimer();
  }

  void _startMatchTimer() {
    _matchTimer?.cancel();

    final startedAt = _matchStartedAt;

    if (startedAt == null) {
      return;
    }

    _matchTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted || matchController.matchIsFinished) {
        return;
      }

      setState(() {
        matchDuration = DateTime.now().difference(startedAt);
      });
    });
  }

  void _startWarmup() {
    if (_warmupStarted || _matchStarted) {
      return;
    }

    _warmupEndsAt = DateTime.now().add(const Duration(minutes: 2));

    setState(() {
      _warmupStarted = true;
      warmupRemaining = const Duration(minutes: 2);
    });

    unawaited(_saveTemporaryMatch());

    _startWarmupTimer();
  }

  void _startWarmupTimer() {
    _warmupTimer?.cancel();

    _warmupTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted || _matchStarted) {
        timer.cancel();
        return;
      }

      final end = _warmupEndsAt;

      if (end == null) {
        timer.cancel();
        return;
      }

      final remaining = end.difference(DateTime.now());

      if (remaining <= Duration.zero) {
        timer.cancel();
        _warmupTimer = null;

        setState(() {
          warmupRemaining = Duration.zero;
        });

        _finishWarmup();
        return;
      }

      setState(() {
        warmupRemaining = remaining;
      });

      if (remaining.inSeconds <= 20 && !_readyAnnouncementShown) {
        _readyAnnouncementShown = true;

        unawaited(_notifyWarmupTwentySeconds());
      }
    });
  }

  Future<void> _finishWarmup() async {
    if (!mounted || _matchStarted) {
      return;
    }

    setState(() {
      warmupRemaining = Duration.zero;
    });

    if (!mounted) {
      return;
    }

    await AppHaptics.countdownFinished();

    if (!mounted) {
      return;
    }

    await _showFirstSetAnnouncement();
  }

  Future<void> _skipWarmup() async {
    _warmupTimer?.cancel();
    _warmupTimer = null;

    _warmupEndsAt = null;

    setState(() {
      _warmupStarted = true;
      warmupRemaining = Duration.zero;
    });

    await _saveTemporaryMatch();

    await _showFirstSetAnnouncement();
  }

  String _getFirstSetAnnouncement() {
    final l10n = AppLocalizations.of(context)!;

    final rightTeamName = matchController.rightTeam.players.join(' / ');
    final leftTeamName = matchController.leftTeam.players.join(' / ');

    if (widget.config.isDoubles) {
      return l10n.firstGameDoublesStartAnnouncement(
        rightTeamName,
        leftTeamName,
        matchController.serverName,
        matchController.receiverName,
      );
    }

    return l10n.firstGameSinglesStartAnnouncement(
      rightTeamName,
      leftTeamName,
      matchController.serverName,
    );
  }

  Future<void> _startMatch() async {
    if (!mounted || _matchStarted) {
      return;
    }

    _warmupTimer?.cancel();
    _warmupTimer = null;
    _warmupEndsAt = null;

    setState(() {
      _matchStarted = true;
      _matchStartedAt ??= DateTime.now();
    });

    await _saveTemporaryMatch();

    _startMatchTimer();
  }

  Future<void> _showFirstSetAnnouncement() async {
    if (!appSettingsController.showUmpireAnnouncements) {
      await _startMatch();
      return;
    }

    final l10n = AppLocalizations.of(context)!;

    final confirmed = await showUmpireAnnouncementDialog(
      context: context,
      announcement: _getFirstSetAnnouncement(),
      buttonLabel: l10n.startMatch,
    );

    if (!confirmed || !mounted) {
      return;
    }

    await _startMatch();
  }
}
