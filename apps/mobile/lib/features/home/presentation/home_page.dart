import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/history/presentation/pages/match_history_page.dart';
import 'package:badminton_umpire/features/match/domain/models/match_draft.dart';
import 'package:badminton_umpire/features/match/presentation/pages/match_page.dart';
import 'package:badminton_umpire/features/match/data/services/match_draft_service.dart';
import 'package:badminton_umpire/features/match/data/services/score_event_outbox.dart';
import 'package:badminton_umpire/features/match_setup/presentation/pages/match_setup_page.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:badminton_umpire/features/pairing/presentation/tablet_pairing_card.dart';
import 'package:badminton_umpire/features/settings/presentation/pages/settings_page.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:material_symbols_icons/symbols.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key, this.pairing});

  /// The pairing of the tablet; the app-wide one when null.
  final TabletSessionController? pairing;

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final MatchDraftService _draftService = MatchDraftService();
  final ScoreEventOutbox _scoreEventOutbox = ScoreEventOutbox();

  MatchDraft? _currentMatch;
  bool _isLoadingDraft = true;

  @override
  void initState() {
    super.initState();
    _loadCurrentMatch();
  }

  Future<void> _loadCurrentMatch() async {
    final draft = await _draftService.load();

    if (!mounted) {
      return;
    }

    setState(() {
      _currentMatch = draft;
      _isLoadingDraft = false;
    });
  }

  Future<void> _openMatchSetup() async {
    if (_currentMatch != null) {
      final shouldReplace = await _confirmReplaceCurrentMatch();

      if (!shouldReplace) {
        return;
      }

      // The points already played still have to reach the backend.
      final replaced = _currentMatch!;
      await _scoreEventOutbox.enqueue(replaced.matchId, replaced.scoreEvents);
      await _draftService.clear();

      if (!mounted) {
        return;
      }

      setState(() {
        _currentMatch = null;
      });
    }

    if (!mounted) {
      return;
    }

    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (context) {
          return MatchSetupPage();
        },
      ),
    );

    await _loadCurrentMatch();
  }

  Future<void> _resumeMatch() async {
    final draft = _currentMatch;

    if (draft == null) {
      return;
    }

    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (context) {
          return MatchPage(
            config: draft.config,
            serverName: draft.serverName,
            receiverName: draft.receiverName,
            leftSideTeam: draft.leftSideTeamId,
            draft: draft,
          );
        },
      ),
    );

    await _loadCurrentMatch();
  }

  Future<bool> _confirmReplaceCurrentMatch() async {
    final result = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('Un match est déjà en cours'),
          content: const Text(
            'Commencer un nouveau match supprimera la sauvegarde '
            'du match actuellement en cours.\n\n'
            'Cette action est irréversible. Voulez-vous continuer ?',
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(dialogContext).pop(false);
              },
              child: const Text('Annuler'),
            ),
            FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.error,
                foregroundColor: Theme.of(context).colorScheme.onError,
              ),
              onPressed: () {
                Navigator.of(dialogContext).pop(true);
              },
              child: const Text('Nouveau match'),
            ),
          ],
        );
      },
    );

    return result ?? false;
  }

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final MatchDraftService _draftService = MatchDraftService();
  final ScoreEventOutbox _scoreEventOutbox = ScoreEventOutbox();

  MatchDraft? _currentMatch;
  bool _isLoadingDraft = true;

  @override
  void initState() {
    super.initState();
    _loadCurrentMatch();
  }

  Future<void> _loadCurrentMatch() async {
    final draft = await _draftService.load();

    if (!mounted) {
      return;
    }

    setState(() {
      _currentMatch = draft;
      _isLoadingDraft = false;
    });
  }

  Future<void> _openMatchSetup() async {
    if (_currentMatch != null) {
      final shouldReplace = await _confirmReplaceCurrentMatch();

      if (!shouldReplace) {
        return;
      }

      // The points already played still have to reach the backend.
      final replaced = _currentMatch!;
      await _scoreEventOutbox.enqueue(replaced.matchId, replaced.scoreEvents);
      await _draftService.clear();

      if (!mounted) {
        return;
      }

      setState(() {
        _currentMatch = null;
      });
    }

    if (!mounted) {
      return;
    }

    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (context) {
          return MatchSetupPage();
        },
      ),
    );

    await _loadCurrentMatch();
  }

  Future<void> _resumeMatch() async {
    final draft = _currentMatch;

    if (draft == null) {
      return;
    }

    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (context) {
          return MatchPage(
            config: draft.config,
            serverName: draft.serverName,
            receiverName: draft.receiverName,
            leftSideTeam: draft.leftSideTeamId,
            draft: draft,
          );
        },
      ),
    );

    await _loadCurrentMatch();
  }

  Future<bool> _confirmReplaceCurrentMatch() async {
    final result = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('Un match est déjà en cours'),
          content: const Text(
            'Commencer un nouveau match supprimera la sauvegarde '
            'du match actuellement en cours.\n\n'
            'Cette action est irréversible. Voulez-vous continuer ?',
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(dialogContext).pop(false);
              },
              child: const Text('Annuler'),
            ),
            FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: Theme.of(context).colorScheme.error,
                foregroundColor: Theme.of(context).colorScheme.onError,
              ),
              onPressed: () {
                Navigator.of(dialogContext).pop(true);
              },
              child: const Text('Nouveau match'),
            ),
          ],
        );
      },
    );

    return result ?? false;
  }

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F8),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 700),
            // Scrolls on short screens (a phone in landscape), keeps the
            // content centered vertically on the others
            child: LayoutBuilder(
              builder: (context, constraints) => SingleChildScrollView(
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: constraints.maxHeight),
                  child: IntrinsicHeight(
                    child: Padding(
                      padding: EdgeInsets.all(responsive.paddingXL),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  l10n.appTitle,
                                  style: TextStyle(
                                    fontSize: responsive.titleL,
                                    fontWeight: FontWeight.w900,
                                  ),
                                ),
                              ),
                              IconButton(
                                tooltip: l10n.settings,
                                icon: Icon(
                                  Icons.settings_outlined,
                                  size: responsive.iconM,
                                ),
                                onPressed: () {
                                  Navigator.of(context).push(
                                    MaterialPageRoute<void>(
                                      builder: (context) {
                                        return const SettingsPage();
                                      },
                                    ),
                                  );
                                },
                              ),
                            ],
                          ),
                          const Spacer(),
                          Icon(
                            Symbols.badminton,
                            size: responsive.scale(min: 60, max: 90),
                            color: Theme.of(context).colorScheme.primary,
                          ),
                          SizedBox(height: responsive.spaceL),
                          Text(
                            l10n.appTitle,
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: responsive.titleXL,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                          SizedBox(height: responsive.spaceXL),
                          TabletPairingCard(
                            controller:
                                widget.pairing ?? tabletSessionController,
                          ),
                          SizedBox(height: responsive.spaceM),
                          if (!_isLoadingDraft && _currentMatch != null) ...[
                            _CurrentMatchCard(
                              draft: _currentMatch!,
                              onTap: _resumeMatch,
                            ),
                            SizedBox(height: responsive.spaceM),
                          ],
                          _HomeActionCard(
                            icon: Icons.add_circle_outline,
                            title: l10n.newMatch,
                            description: l10n.newMatchDescription,
                            onTap: _openMatchSetup,
                          ),
                          SizedBox(height: responsive.spaceM),
                          _HomeActionCard(
                            icon: Icons.history,
                            title: l10n.history,
                            description: l10n.historyDescription,
                            onTap: () {
                              Navigator.of(context).push(
                                MaterialPageRoute<void>(
                                  builder: (context) {
                                    return const MatchHistoryPage();
                                  },
                                ),
                              );
                            },
                          ),
                          const Spacer(),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _CurrentMatchCard extends StatelessWidget {
  final MatchDraft draft;
  final VoidCallback onTap;

  const _CurrentMatchCard({required this.draft, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final primaryColor = Theme.of(context).colorScheme.primary;

    final teamA = draft.teamAPlayers.join(' / ');
    final teamB = draft.teamBPlayers.join(' / ');

    final currentSet = draft.previousSets.length + 1;

    return Material(
      color: primaryColor.withValues(alpha: 0.08),
      borderRadius: BorderRadius.circular(responsive.radiusL),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(responsive.radiusL),
        child: Container(
          padding: EdgeInsets.all(responsive.paddingL),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(responsive.radiusL),
            border: Border.all(color: primaryColor.withValues(alpha: 0.35)),
          ),
          child: Row(
            children: [
              Container(
                width: responsive.scale(min: 48, max: 60),
                height: responsive.scale(min: 48, max: 60),
                decoration: BoxDecoration(
                  color: primaryColor.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(responsive.radiusM),
                ),
                child: Icon(
                  Icons.play_arrow_rounded,
                  color: primaryColor,
                  size: responsive.iconL,
                ),
              ),
              SizedBox(width: responsive.spaceM),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      draft.phase.name == 'finished'
                          ? 'Voir le match terminé'
                          : 'Reprendre le match',
                      style: TextStyle(
                        fontSize: responsive.titleS,
                        fontWeight: FontWeight.w900,
                        color: primaryColor,
                      ),
                    ),
                    SizedBox(height: responsive.spaceXS),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            teamA,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontSize: responsive.textS,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                        SizedBox(width: responsive.spaceS),
                        Text(
                          '${draft.teamAScore} - ${draft.teamBScore}',
                          style: TextStyle(
                            fontSize: responsive.titleS,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: responsive.spaceXXS),
                    Text(
                      teamB,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: responsive.textS,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    SizedBox(height: responsive.spaceXS),
                    Text(
                      'Set $currentSet • Terrain ${draft.courtNumber}',
                      style: TextStyle(
                        fontSize: responsive.textS,
                        color: Colors.grey.shade700,
                      ),
                    ),
                  ],
                ),
              ),
              SizedBox(width: responsive.spaceS),
              Icon(
                Icons.chevron_right,
                color: primaryColor,
                size: responsive.iconM,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _HomeActionCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;
  final VoidCallback onTap;

  const _HomeActionCard({
    required this.icon,
    required this.title,
    required this.description,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(responsive.radiusL),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(responsive.radiusL),
        child: Container(
          padding: EdgeInsets.all(responsive.paddingL),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(responsive.radiusL),
            border: Border.all(color: const Color(0xFFE3E3EA)),
          ),
          child: Row(
            children: [
              Container(
                width: responsive.scale(min: 48, max: 60),
                height: responsive.scale(min: 48, max: 60),
                decoration: BoxDecoration(
                  color: primaryColor.withValues(alpha: 0.10),
                  borderRadius: BorderRadius.circular(responsive.radiusM),
                ),
                child: Icon(icon, color: primaryColor, size: responsive.iconL),
              ),
              SizedBox(width: responsive.spaceM),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontSize: responsive.titleS,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    SizedBox(height: responsive.spaceXXS),
                    Text(
                      description,
                      style: TextStyle(
                        fontSize: responsive.textS,
                        color: Colors.grey.shade700,
                      ),
                    ),
                  ],
                ),
              ),
              SizedBox(width: responsive.spaceS),
              Icon(
                Icons.chevron_right,
                size: responsive.iconM,
                color: Colors.grey.shade500,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CurrentMatchCard extends StatelessWidget {
  final MatchDraft draft;
  final VoidCallback onTap;

  const _CurrentMatchCard({required this.draft, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final primaryColor = Theme.of(context).colorScheme.primary;

    final teamA = draft.teamAPlayers.join(' / ');
    final teamB = draft.teamBPlayers.join(' / ');

    final currentSet = draft.previousSets.length + 1;

    return Material(
      color: primaryColor.withValues(alpha: 0.08),
      borderRadius: BorderRadius.circular(responsive.radiusL),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(responsive.radiusL),
        child: Container(
          padding: EdgeInsets.all(responsive.paddingL),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(responsive.radiusL),
            border: Border.all(color: primaryColor.withValues(alpha: 0.35)),
          ),
          child: Row(
            children: [
              Container(
                width: responsive.scale(min: 48, max: 60),
                height: responsive.scale(min: 48, max: 60),
                decoration: BoxDecoration(
                  color: primaryColor.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(responsive.radiusM),
                ),
                child: Icon(
                  Icons.play_arrow_rounded,
                  color: primaryColor,
                  size: responsive.iconL,
                ),
              ),
              SizedBox(width: responsive.spaceM),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      draft.phase.name == 'finished'
                          ? 'Voir le match terminé'
                          : 'Reprendre le match',
                      style: TextStyle(
                        fontSize: responsive.titleS,
                        fontWeight: FontWeight.w900,
                        color: primaryColor,
                      ),
                    ),
                    SizedBox(height: responsive.spaceXS),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            teamA,
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              fontSize: responsive.textS,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                        SizedBox(width: responsive.spaceS),
                        Text(
                          '${draft.teamAScore} - ${draft.teamBScore}',
                          style: TextStyle(
                            fontSize: responsive.titleS,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: responsive.spaceXXS),
                    Text(
                      teamB,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: responsive.textS,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    SizedBox(height: responsive.spaceXS),
                    Text(
                      'Set $currentSet • Terrain ${draft.courtNumber}',
                      style: TextStyle(
                        fontSize: responsive.textS,
                        color: Colors.grey.shade700,
                      ),
                    ),
                  ],
                ),
              ),
              SizedBox(width: responsive.spaceS),
              Icon(
                Icons.chevron_right,
                color: primaryColor,
                size: responsive.iconM,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _HomeActionCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;
  final VoidCallback onTap;

  const _HomeActionCard({
    required this.icon,
    required this.title,
    required this.description,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final primaryColor = Theme.of(context).colorScheme.primary;

    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(responsive.radiusL),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(responsive.radiusL),
        child: Container(
          padding: EdgeInsets.all(responsive.paddingL),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(responsive.radiusL),
            border: Border.all(color: const Color(0xFFE3E3EA)),
          ),
          child: Row(
            children: [
              Container(
                width: responsive.scale(min: 48, max: 60),
                height: responsive.scale(min: 48, max: 60),
                decoration: BoxDecoration(
                  color: primaryColor.withValues(alpha: 0.10),
                  borderRadius: BorderRadius.circular(responsive.radiusM),
                ),
                child: Icon(icon, color: primaryColor, size: responsive.iconL),
              ),
              SizedBox(width: responsive.spaceM),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: TextStyle(
                        fontSize: responsive.titleS,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    SizedBox(height: responsive.spaceXXS),
                    Text(
                      description,
                      style: TextStyle(
                        fontSize: responsive.textS,
                        color: Colors.grey.shade700,
                      ),
                    ),
                  ],
                ),
              ),
              SizedBox(width: responsive.spaceS),
              Icon(
                Icons.chevron_right,
                size: responsive.iconM,
                color: Colors.grey.shade500,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
