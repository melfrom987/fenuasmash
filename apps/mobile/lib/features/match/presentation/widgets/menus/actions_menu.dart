import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

Future<void> showMatchActionsMenu(
  BuildContext context, {
  required VoidCallback onCorrectPositions,
  required VoidCallback onWarningOrCard,
  required VoidCallback onRetirement,
  required VoidCallback onEditPlayerColors,
}) {
  final l10n = AppLocalizations.of(context)!;

  return showDialog<void>(
    context: context,
    builder: (dialogContext) {
      final responsive = Responsive.of(dialogContext);

      final sections = [
        _ActionSection(
          title: l10n.assistance,
          actions: [
            _MatchAction(icon: Icons.sports, title: l10n.callReferee),
            _MatchAction(
              icon: Icons.table_restaurant_outlined,
              title: l10n.callScoringTable,
            ),
            _MatchAction(
              icon: Icons.medical_services_outlined,
              title: l10n.callMedicalService,
            ),
          ],
        ),

        _ActionSection(
          title: l10n.incidents,
          actions: [
            _MatchAction(
              icon: Icons.warning_amber_rounded,
              title: l10n.warningOrCard,
              onTap: onWarningOrCard,
            ),
            _MatchAction(icon: Icons.healing_outlined, title: l10n.injury),
            _MatchAction(
              icon: Icons.flag_outlined,
              title: l10n.retirement,
              isDangerous: true,
              onTap: onRetirement,
            ),
            _MatchAction(
              icon: Icons.pause_circle_outline,
              title: l10n.matchInterruption,
            ),
          ],
        ),

        _ActionSection(
          title: l10n.corrections,
          actions: [
            _MatchAction(
              icon: Icons.swap_horiz,
              title: l10n.correctPlayerPositions,
              onTap: onCorrectPositions,
            ),
            _MatchAction(
              icon: Icons.palette_outlined,
              title: l10n.editPlayerColors,
              onTap: onEditPlayerColors,
            ),
          ],
        ),

        _ActionSection(
          title: l10n.match,
          actions: [
            _MatchAction(
              icon: Icons.stop_circle_outlined,
              title: l10n.finishMatch,
              isDangerous: true,
            ),
          ],
        ),
      ];

      return Dialog(
        insetPadding: EdgeInsets.symmetric(
          horizontal: responsive.spaceM,
          vertical: responsive.spaceS,
        ),
        child: ConstrainedBox(
          constraints: BoxConstraints(
            maxWidth: responsive.dialogWidthL,
            maxHeight:
                MediaQuery.sizeOf(dialogContext).height -
                (responsive.spaceM * 2),
          ),
          child: Padding(
            padding: EdgeInsets.all(responsive.paddingM),
            child: Column(
              children: [
                // HEADER
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        l10n.matchActions,
                        style: TextStyle(
                          fontSize: responsive.titleL,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                    ),
                    IconButton(
                      visualDensity: VisualDensity.compact,
                      onPressed: () {
                        Navigator.of(dialogContext).pop();
                      },
                      icon: Icon(Icons.close, size: responsive.iconM),
                    ),
                  ],
                ),

                SizedBox(height: responsive.spaceS),

                Expanded(
                  child: LayoutBuilder(
                    builder: (context, constraints) {
                      final availableWidth = constraints.maxWidth;

                      final useFourColumns = availableWidth >= 650;

                      if (useFourColumns) {
                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            for (
                              var index = 0;
                              index < sections.length;
                              index++
                            ) ...[
                              Expanded(
                                child: _ActionSectionWidget(
                                  section: sections[index],
                                  onActionPressed: (action) {
                                    Navigator.of(dialogContext).pop();

                                    if (action.onTap != null) {
                                      action.onTap!();
                                      return;
                                    }

                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(
                                        content: Text(
                                          l10n.actionNotImplemented(
                                            action.title,
                                          ),
                                        ),
                                      ),
                                    );
                                  },
                                ),
                              ),

                              if (index < sections.length - 1)
                                SizedBox(width: responsive.spaceS),
                            ],
                          ],
                        );
                      }

                      // Si l'écran devient vraiment trop étroit :
                      // 2 colonnes de catégories.
                      return GridView.builder(
                        padding: EdgeInsets.zero,
                        physics: const NeverScrollableScrollPhysics(),
                        itemCount: sections.length,
                        gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          crossAxisSpacing: responsive.spaceS,
                          mainAxisSpacing: responsive.spaceS,
                          childAspectRatio: 1.2,
                        ),
                        itemBuilder: (context, index) {
                          return _ActionSectionWidget(
                            section: sections[index],
                            onActionPressed: (action) {
                              Navigator.of(dialogContext).pop();

                              if (action.onTap != null) {
                                action.onTap!();
                                return;
                              }

                              ScaffoldMessenger.of(context).showSnackBar(
                                SnackBar(
                                  content: Text(
                                    l10n.actionNotImplemented(action.title),
                                  ),
                                ),
                              );
                            },
                          );
                        },
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    },
  );
}

class _ActionSection {
  final String title;
  final List<_MatchAction> actions;

  const _ActionSection({required this.title, required this.actions});
}

class _MatchAction {
  final IconData icon;
  final String title;
  final bool isDangerous;
  final VoidCallback? onTap;

  const _MatchAction({
    required this.icon,
    required this.title,
    this.isDangerous = false,
    this.onTap,
  });
}

class _ActionSectionWidget extends StatelessWidget {
  final _ActionSection section;
  final ValueChanged<_MatchAction> onActionPressed;

  const _ActionSectionWidget({
    required this.section,
    required this.onActionPressed,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          section.title.toUpperCase(),
          textAlign: TextAlign.center,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(
            fontSize: responsive.textXXS,
            fontWeight: FontWeight.w900,
            letterSpacing: 0.6,
            color: Colors.grey.shade600,
          ),
        ),

        SizedBox(height: responsive.spaceXS),

        for (var index = 0; index < section.actions.length; index++) ...[
          SizedBox(
            height: responsive.scale(min: 34, max: 44),
            child: _ActionButton(
              action: section.actions[index],
              onTap: () {
                onActionPressed(section.actions[index]);
              },
            ),
          ),

          if (index < section.actions.length - 1)
            SizedBox(height: responsive.spaceXS),
        ],
      ],
    );
  }
}

class _ActionButton extends StatelessWidget {
  final _MatchAction action;
  final VoidCallback onTap;

  const _ActionButton({required this.action, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final errorColor = Theme.of(context).colorScheme.error;

    final foregroundColor = action.isDangerous ? errorColor : Colors.black87;

    return Material(
      color: action.isDangerous
          ? errorColor.withValues(alpha: 0.06)
          : const Color(0xFFF7F7FA),
      borderRadius: BorderRadius.circular(responsive.radiusS),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(responsive.radiusS),
        child: Padding(
          padding: EdgeInsets.symmetric(
            horizontal: responsive.paddingXS,
            vertical: responsive.paddingXS,
          ),
          child: Row(
            children: [
              Icon(action.icon, size: responsive.iconS, color: foregroundColor),

              SizedBox(width: responsive.spaceS),

              Expanded(
                child: Text(
                  action.title,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: responsive.textXS,
                    height: 1.05,
                    color: foregroundColor,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
