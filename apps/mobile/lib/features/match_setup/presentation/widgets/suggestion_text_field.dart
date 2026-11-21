import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:flutter/material.dart';

class SuggestionTextField extends StatelessWidget {
  final TextEditingController controller;

  final String label;
  final String hint;

  final List<String> suggestions;
  final ValueChanged<String>? onChanged;

  const SuggestionTextField({
    super.key,
    required this.controller,
    required this.label,
    required this.hint,
    required this.suggestions,
    this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);

    return Autocomplete<String>(
      optionsBuilder: (textEditingValue) {
        final query = textEditingValue.text.trim().toLowerCase();

        if (query.isEmpty) {
          return suggestions;
        }

        return suggestions.where(
          (suggestion) => suggestion.toLowerCase().contains(query),
        );
      },
      onSelected: (value) {
        controller.text = value;

        controller.selection = TextSelection.collapsed(
          offset: controller.text.length,
        );

        onChanged?.call(value);
      },
      fieldViewBuilder: (context, textController, focusNode, onFieldSubmitted) {
        if (textController.text != controller.text) {
          textController.text = controller.text;
          textController.selection = TextSelection.collapsed(
            offset: textController.text.length,
          );
        }

        return TextFormField(
          controller: textController,
          focusNode: focusNode,
          decoration: InputDecoration(
            labelText: label,
            hintText: hint,
            border: const OutlineInputBorder(),
          ),
          style: TextStyle(fontSize: responsive.textM),
          onChanged: (value) {
            controller.text = value;
            onChanged?.call(value);
          },
        );
      },
      optionsViewBuilder: (context, onSelected, options) {
        return Align(
          alignment: Alignment.topLeft,
          child: Material(
            elevation: 4,
            borderRadius: BorderRadius.circular(responsive.radiusS),
            child: ConstrainedBox(
              constraints: BoxConstraints(
                maxHeight: responsive.scale(min: 180, max: 260),
                maxWidth: responsive.scale(min: 240, max: 420),
              ),
              child: ListView.builder(
                padding: EdgeInsets.zero,
                shrinkWrap: true,
                itemCount: options.length,
                itemBuilder: (context, index) {
                  final option = options.elementAt(index);

                  return ListTile(
                    dense: true,
                    title: Text(
                      option,
                      style: TextStyle(fontSize: responsive.textS),
                    ),
                    onTap: () {
                      onSelected(option);
                    },
                  );
                },
              ),
            ),
          ),
        );
      },
    );
  }
}
