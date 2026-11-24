import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/match_setup/domain/models/match_category.dart';
import 'package:badminton_umpire/features/match_setup/presentation/widgets/category_selector.dart';
import 'package:badminton_umpire/features/match_setup/presentation/widgets/suggestion_text_field.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class MatchClassificationCard extends StatelessWidget {
  final MatchCategory selectedCategory;
  final ValueChanged<MatchCategory> onCategoryChanged;

  final TextEditingController ageCategoryController;
  final TextEditingController poolGroupController;
  final TextEditingController tournamentPhaseController;

  final List<String> ageCategorySuggestions;
  final List<String> poolGroupSuggestions;
  final List<String> tournamentPhaseSuggestions;

  final bool showPoolGroup;
  final ValueChanged<String> onTournamentPhaseChanged;

  const MatchClassificationCard({
    super.key,
    required this.selectedCategory,
    required this.onCategoryChanged,
    required this.ageCategoryController,
    required this.poolGroupController,
    required this.tournamentPhaseController,
    required this.ageCategorySuggestions,
    required this.poolGroupSuggestions,
    required this.tournamentPhaseSuggestions,
    required this.showPoolGroup,
    required this.onTournamentPhaseChanged,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Card(
      elevation: 0,
      margin: EdgeInsets.zero,
      color: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(responsive.radiusL),
        side: const BorderSide(color: Color(0xFFE4E4EA)),
      ),
      child: Padding(
        padding: EdgeInsets.all(responsive.paddingXL),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              l10n.matchClassification,
              style: TextStyle(
                fontSize: responsive.titleM,
                fontWeight: FontWeight.bold,
              ),
            ),

            SizedBox(height: responsive.spaceXS),

            Text(
              l10n.matchClassificationDescription,
              style: TextStyle(
                fontSize: responsive.textM,
                color: Colors.grey.shade700,
              ),
            ),

            SizedBox(height: responsive.spaceM),

            CategorySelector(
              selectedCategory: selectedCategory,
              onChanged: onCategoryChanged,
            ),

            SizedBox(height: responsive.spaceL),

            LayoutBuilder(
              builder: (context, constraints) {
                final canUseThreeColumns =
                    constraints.maxWidth >= responsive.teamColumnsMinWidth;

                if (canUseThreeColumns) {
                  return Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: SuggestionTextField(
                          controller: ageCategoryController,
                          label: l10n.ageCategory,
                          hint: l10n.ageCategoryHint,
                          suggestions: ageCategorySuggestions,
                        ),
                      ),

                      SizedBox(width: responsive.spaceM),

                      Expanded(
                        child: SuggestionTextField(
                          controller: tournamentPhaseController,
                          label: l10n.tournamentPhase,
                          hint: l10n.tournamentPhaseHint,
                          suggestions: tournamentPhaseSuggestions,
                          onChanged: onTournamentPhaseChanged,
                        ),
                      ),

                      SizedBox(width: responsive.spaceM),

                      if (showPoolGroup) ...[
                        Expanded(
                          child: SuggestionTextField(
                            controller: poolGroupController,
                            label: l10n.poolGroup,
                            hint: l10n.poolGroupHint,
                            suggestions: poolGroupSuggestions,
                          ),
                        ),

                        SizedBox(width: responsive.spaceM),
                      ],
                    ],
                  );
                }

                return Column(
                  children: [
                    SuggestionTextField(
                      controller: ageCategoryController,
                      label: l10n.ageCategory,
                      hint: l10n.ageCategoryHint,
                      suggestions: ageCategorySuggestions,
                    ),

                    SizedBox(height: responsive.spaceM),

                    SuggestionTextField(
                      controller: tournamentPhaseController,
                      label: l10n.tournamentPhase,
                      hint: l10n.tournamentPhaseHint,
                      suggestions: tournamentPhaseSuggestions,
                      onChanged: onTournamentPhaseChanged,
                    ),

                    SizedBox(height: responsive.spaceM),

                    if (showPoolGroup) ...[
                      SuggestionTextField(
                        controller: poolGroupController,
                        label: l10n.poolGroup,
                        hint: l10n.poolGroupHint,
                        suggestions: poolGroupSuggestions,
                      ),
                    ],
                  ],
                );
              },
            ),
          ],
        ),
      ),
    );
  }
}
