import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/court_match/domain/court_match.dart';
import 'package:badminton_umpire/features/match/domain/models/match_config.dart';
import 'package:badminton_umpire/features/match_setup/domain/models/match_category.dart';
import 'package:badminton_umpire/features/match_setup/presentation/pages/court_setup_page.dart';
import 'package:badminton_umpire/features/match_setup/presentation/widgets/match_classification_card.dart';
import 'package:badminton_umpire/features/match_setup/presentation/widgets/match_information_card.dart';
import 'package:badminton_umpire/features/match_setup/presentation/widgets/match_setup_header.dart';
import 'package:badminton_umpire/features/match_setup/presentation/widgets/start_match_button.dart';
import 'package:badminton_umpire/features/match_setup/presentation/widgets/team_information_card.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class MatchSetupPage extends StatefulWidget {
  const MatchSetupPage({super.key, this.courtMatch, this.courtNumber});

  /// The match called to the court of the tablet, to fill in what the server
  /// knows; null for a match created on the tablet.
  final CourtMatch? courtMatch;

  /// The court of the tablet, with [courtMatch].
  final int? courtNumber;

  @override
  State<MatchSetupPage> createState() => _MatchSetupPageState();
}

class _MatchSetupPageState extends State<MatchSetupPage> {
  MatchCategory selectedCategory = MatchCategory.mixedDoubles;

  String? ageCategory;
  String? poolGroup;
  String? tournamentPhase;

  String rightSideTeam = 'teamA';
  final tournamentController = TextEditingController();
  final courtNumberController = TextEditingController();

  final teamAPlayer1Controller = TextEditingController();
  final teamAPlayer2Controller = TextEditingController();
  final teamBPlayer1Controller = TextEditingController();
  final teamBPlayer2Controller = TextEditingController();
  final umpireNameController = TextEditingController();
  final serviceJudgeNameController = TextEditingController();
  final ageCategoryController = TextEditingController();
  final tournamentPhaseController = TextEditingController();
  final poolGroupController = TextEditingController();

  List<String> _ageCategorySuggestions(AppLocalizations l10n) {
    return [
      l10n.agePoussin,
      l10n.agePoussine,
      l10n.ageBenjamin,
      l10n.ageBenjamine,
      l10n.ageMinimeBoy,
      l10n.ageMinimeGirl,
      l10n.ageCadet,
      l10n.ageCadette,
      l10n.ageJuniorBoy,
      l10n.ageJuniorGirl,
      l10n.ageSeniorSeries1,
      l10n.ageSeniorSeries2,
      l10n.ageSeniorSeries3,
    ];
  }

  List<String> _poolGroupSuggestions(AppLocalizations l10n) {
    return [l10n.poolA, l10n.poolB, l10n.poolC, l10n.poolD];
  }

  List<String> _tournamentPhaseSuggestions(AppLocalizations l10n) {
    return [
      l10n.poolUnique,
      l10n.phasePoolMatch,
      l10n.phaseRoundOf16,
      l10n.phaseQuarterFinal,
      l10n.phaseSemiFinal,
      l10n.phaseFinal,
      l10n.phaseClassification,
    ];
  }

  bool get _hasValidPlayerNames {
    final playerA1 = teamAPlayer1Controller.text.trim();
    final playerB1 = teamBPlayer1Controller.text.trim();

    if (playerA1.isEmpty || playerB1.isEmpty) {
      return false;
    }

    if (selectedCategory.isDoubles) {
      final playerA2 = teamAPlayer2Controller.text.trim();
      final playerB2 = teamBPlayer2Controller.text.trim();

      if (playerA2.isEmpty || playerB2.isEmpty) {
        return false;
      }

      if (playerA1.toLowerCase() == playerA2.toLowerCase() ||
          playerB1.toLowerCase() == playerB2.toLowerCase()) {
        return false;
      }
    }

    return true;
  }

  bool get _hasValidMatchInformation {
    return courtNumberController.text.trim().isNotEmpty &&
        umpireNameController.text.trim().isNotEmpty;
  }

  bool get _canContinue {
    return _hasValidPlayerNames && _hasValidMatchInformation;
  }

  @override
  void initState() {
    super.initState();

    teamAPlayer1Controller.addListener(_refreshPage);
    teamAPlayer2Controller.addListener(_refreshPage);
    teamBPlayer1Controller.addListener(_refreshPage);
    teamBPlayer2Controller.addListener(_refreshPage);
    tournamentController.addListener(_refreshPage);
    courtNumberController.addListener(_refreshPage);
    umpireNameController.addListener(_refreshPage);
    serviceJudgeNameController.addListener(_refreshPage);
  }

  bool _prefilled = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final match = widget.courtMatch;
    if (_prefilled || match == null) {
      return;
    }
    _prefilled = true;
    final l10n = AppLocalizations.of(context)!;
    tournamentController.text = match.tournamentName ?? '';
    if (widget.courtNumber != null) {
      courtNumberController.text = '${widget.courtNumber}';
    }
    // Open doubles has no category here: the doubles default stays
    selectedCategory =
        match.category ??
        (match.isDoubles == false
            ? MatchCategory.menSingles
            : MatchCategory.mixedDoubles);
    ageCategoryController.text = match.categoryLabel ?? '';
    if (match.round != null) {
      tournamentPhaseController.text = l10n.courtMatchRound(match.round!);
    }
  }

  void _refreshPage() {
    setState(() {});
  }

  void resetMatchPlayers() {
    teamAPlayer1Controller.clear();
    teamAPlayer2Controller.clear();
    teamBPlayer1Controller.clear();
    teamBPlayer2Controller.clear();

    setState(() {});
  }

  @override
  void dispose() {
    teamAPlayer1Controller.dispose();
    teamAPlayer2Controller.dispose();
    teamBPlayer1Controller.dispose();
    teamBPlayer2Controller.dispose();

    tournamentController.dispose();
    courtNumberController.dispose();

    umpireNameController.dispose();
    serviceJudgeNameController.dispose();

    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    final showPoolGroup =
        tournamentPhaseController.text.trim() == l10n.phasePoolMatch;

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 1100),
            child: Padding(
              padding: EdgeInsets.all(responsive.paddingXL),
              child: Column(
                children: [
                  Expanded(
                    child: ListView(
                      children: [
                        MatchSetupHeader(
                          onHome: () {
                            Navigator.of(
                              context,
                            ).popUntil((route) => route.isFirst);
                          },
                        ),
                        SizedBox(height: responsive.spaceXL),

                        MatchInformationCard(
                          tournamentController: tournamentController,
                          courtNumberController: courtNumberController,
                          umpireNameController: umpireNameController,
                          serviceJudgeNameController:
                              serviceJudgeNameController,
                        ),

                        SizedBox(height: responsive.spaceXL),

                        MatchClassificationCard(
                          selectedCategory: selectedCategory,
                          onCategoryChanged: (category) {
                            setState(() {
                              selectedCategory = category;
                            });
                          },
                          ageCategoryController: ageCategoryController,
                          poolGroupController: poolGroupController,
                          tournamentPhaseController: tournamentPhaseController,

                          ageCategorySuggestions: _ageCategorySuggestions(l10n),
                          poolGroupSuggestions: _poolGroupSuggestions(l10n),
                          tournamentPhaseSuggestions:
                              _tournamentPhaseSuggestions(l10n),

                          showPoolGroup: showPoolGroup,
                          onTournamentPhaseChanged: (value) {
                            setState(() {
                              if (value.trim() != l10n.phasePoolMatch) {
                                poolGroupController.clear();
                              }
                            });
                          },
                        ),
                        SizedBox(height: responsive.spaceXL),

                        LayoutBuilder(
                          builder: (context, constraints) {
                            final responsive = Responsive.of(context);

                            final canDisplaySideBySide =
                                constraints.maxWidth >=
                                responsive.teamColumnsMinWidth;

                            if (canDisplaySideBySide) {
                              return Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Expanded(child: _buildTeamA()),
                                  SizedBox(width: responsive.spaceL),
                                  Expanded(child: _buildTeamB()),
                                ],
                              );
                            }

                            return Column(
                              children: [
                                _buildTeamA(),
                                SizedBox(height: responsive.spaceL),
                                _buildTeamB(),
                              ],
                            );
                          },
                        ),
                      ],
                    ),
                  ),
                  StartMatchButton(
                    enabled: _canContinue,
                    onPressed: _continueToCourtSetup,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildTeamA() {
    final l10n = AppLocalizations.of(context)!;

    return TeamInformationCard(
      title: l10n.teamA,
      isDoubles: selectedCategory.isDoubles,
      player1Controller: teamAPlayer1Controller,
      player2Controller: teamAPlayer2Controller,
    );
  }

  Widget _buildTeamB() {
    final l10n = AppLocalizations.of(context)!;

    return TeamInformationCard(
      title: l10n.teamB,
      isDoubles: selectedCategory.isDoubles,
      player1Controller: teamBPlayer1Controller,
      player2Controller: teamBPlayer2Controller,
    );
  }

  Future<void> _continueToCourtSetup() async {
    final l10n = AppLocalizations.of(context)!;
    if (!_canContinue) {
      return;
    }

    final teamAPlayers = [
      teamAPlayer1Controller.text.trim(),
      if (selectedCategory.isDoubles) teamAPlayer2Controller.text.trim(),
    ];

    final teamBPlayers = [
      teamBPlayer1Controller.text.trim(),
      if (selectedCategory.isDoubles) teamBPlayer2Controller.text.trim(),
    ];

    final matchConfig = MatchConfig(
      isDoubles: selectedCategory.isDoubles,

      teamAPlayers: teamAPlayers,
      teamBPlayers: teamBPlayers,

      tournamentName: tournamentController.text.trim().isEmpty
          ? l10n.defaultTournamentName
          : tournamentController.text.trim(),

      courtNumber: courtNumberController.text.trim(),

      category: selectedCategory.localizedLabel(l10n),

      ageCategory: ageCategoryController.text.trim().isEmpty
          ? null
          : ageCategoryController.text.trim(),

      poolGroup: poolGroupController.text.trim().isEmpty
          ? null
          : poolGroupController.text.trim(),

      tournamentPhase: tournamentPhaseController.text.trim().isEmpty
          ? null
          : tournamentPhaseController.text.trim(),

      umpireName: umpireNameController.text.trim(),

      serviceJudgeName: serviceJudgeNameController.text.trim().isEmpty
          ? null
          : serviceJudgeNameController.text.trim(),

      serverMatchId: widget.courtMatch?.id,
    );

    await Navigator.of(context).push<void>(
      MaterialPageRoute<void>(
        builder: (context) {
          return CourtSetupPage(config: matchConfig);
        },
      ),
    );
  }
}
