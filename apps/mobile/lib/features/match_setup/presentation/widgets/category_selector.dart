import 'package:badminton_umpire/features/match_setup/domain/models/match_category.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class CategorySelector extends StatelessWidget {
  final MatchCategory selectedCategory;
  final ValueChanged<MatchCategory> onChanged;

  const CategorySelector({
    super.key,
    required this.selectedCategory,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return DropdownButtonFormField<MatchCategory>(
      initialValue: selectedCategory,
      decoration: InputDecoration(
        labelText: '${l10n.category} *',
        prefixIcon: const Icon(Icons.sports),
      ),
      items: MatchCategory.values.map((category) {
        return DropdownMenuItem<MatchCategory>(
          value: category,
          child: Text(category.localizedLabel(l10n)),
        );
      }).toList(),
      onChanged: (category) {
        if (category != null) {
          onChanged(category);
        }
      },
    );
  }
}
