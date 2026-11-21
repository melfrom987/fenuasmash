import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

Future<Color?> showPlayerColorDialog({
  required BuildContext context,
  required String playerName,
  required Color currentColor,
}) {
  return showDialog<Color>(
    context: context,
    builder: (context) {
      return _PlayerColorDialog(
        playerName: playerName,
        currentColor: currentColor,
      );
    },
  );
}

class _PlayerColorDialog extends StatefulWidget {
  final String playerName;
  final Color currentColor;

  const _PlayerColorDialog({
    required this.playerName,
    required this.currentColor,
  });

  @override
  State<_PlayerColorDialog> createState() => _PlayerColorDialogState();
}

class _PlayerColorDialogState extends State<_PlayerColorDialog> {
  late Color selectedColor;

  static const availableColors = [
    Color(0xFFEF5350), // rouge
    Color(0xFFEC407A), // rose
    Color(0xFF7E57C2), // violet
    Color(0xFF5C6BC0), // indigo
    Color(0xFF42A5F5), // bleu
    Color(0xFF26A69A), // turquoise
    Color(0xFF66BB6A), // vert
    Color(0xFFFFCA28), // jaune
    Color(0xFFFFA726), // orange
    Color(0xFF8D6E63), // marron
    Color(0xFF616161), // gris
    Color(0xFF212121), // noir
    Color(0xFFF5F5F5), // blanc
  ];

  @override
  void initState() {
    super.initState();
    selectedColor = widget.currentColor;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final responsive = Responsive.of(context);
    return AlertDialog(
      title: Text('${l10n.colorsOf} ${widget.playerName}'),
      content: SizedBox(
        width: 360,
        child: Wrap(
          spacing: responsive.spaceM,
          runSpacing: responsive.spaceM,
          children: [
            for (final color in availableColors)
              _ColorChoice(
                color: color,
                isSelected: selectedColor == color,
                onTap: () {
                  setState(() {
                    selectedColor = color;
                  });
                },
              ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () {
            Navigator.of(context).pop();
          },
          child: Text(l10n.cancel),
        ),
        FilledButton(
          onPressed: () {
            Navigator.of(context).pop(selectedColor);
          },
          child: Text(l10n.apply),
        ),
      ],
    );
  }
}

class _ColorChoice extends StatelessWidget {
  final Color color;
  final bool isSelected;
  final VoidCallback onTap;

  const _ColorChoice({
    required this.color,
    required this.isSelected,
    required this.onTap,
  });

  bool get _needsDarkBorder {
    return color == const Color(0xFFF5F5F5);
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(50),
      child: Container(
        width: 48,
        height: 48,
        decoration: BoxDecoration(
          color: color,
          shape: BoxShape.circle,
          border: Border.all(
            color: isSelected
                ? Theme.of(context).colorScheme.primary
                : _needsDarkBorder
                ? Colors.grey.shade400
                : Colors.transparent,
            width: isSelected ? 4 : 1,
          ),
        ),
        child: isSelected
            ? Icon(
                Icons.check,
                color: color.computeLuminance() > 0.5
                    ? Colors.black
                    : Colors.white,
              )
            : null,
      ),
    );
  }
}
