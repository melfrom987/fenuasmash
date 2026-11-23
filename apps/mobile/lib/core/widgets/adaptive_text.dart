import 'package:flutter/material.dart';

class AdaptiveText extends StatelessWidget {
  final String text;
  final TextStyle style;
  final TextAlign textAlign;
  final double minFontSize;
  final int maxLines;
  final TextOverflow overflow;

  const AdaptiveText({
    super.key,
    required this.text,
    required this.style,
    this.textAlign = TextAlign.start,
    this.minFontSize = 8,
    this.maxLines = 1,
    this.overflow = TextOverflow.ellipsis,
  });

  @override
  Widget build(BuildContext context) {
    final textScaler = MediaQuery.textScalerOf(context);

    return LayoutBuilder(
      builder: (context, constraints) {
        double fontSize = style.fontSize ?? 16;

        while (fontSize > minFontSize) {
          final painter = TextPainter(
            text: TextSpan(
              text: text,
              style: style.copyWith(fontSize: fontSize),
            ),
            textAlign: textAlign,
            textDirection: Directionality.of(context),
            textScaler: textScaler,
            maxLines: maxLines,
          )..layout(maxWidth: constraints.maxWidth);

          final fitsLines = !painter.didExceedMaxLines;

          final fitsHeight =
              !constraints.hasBoundedHeight ||
              painter.height <= constraints.maxHeight;

          if (fitsLines && fitsHeight) {
            break;
          }

          fontSize -= 0.5;
        }

        return Text(
          text,
          textAlign: textAlign,
          maxLines: maxLines,
          softWrap: maxLines > 1,
          overflow: overflow,
          style: style.copyWith(fontSize: fontSize),
        );
      },
    );
  }
}
