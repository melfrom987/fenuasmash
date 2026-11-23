import 'package:flutter/material.dart';

class Responsive {
  final double width;
  final double height;

  const Responsive({required this.width, required this.height});

  factory Responsive.of(BuildContext context) {
    final size = MediaQuery.sizeOf(context);

    return Responsive(width: size.width, height: size.height);
  }

  double get shortestSide => width < height ? width : height;

  bool get isSmallPhone => shortestSide < 380;
  bool get isPhone => shortestSide < 600;
  bool get isTablet => shortestSide >= 600;

  bool get isShortScreen => height < 650;
  bool get isVeryShortScreen => height < 550;

  double scale({
    required double min,
    required double max,
    double minWidth = 360,
    double maxWidth = 1000,
  }) {
    final factor = ((width - minWidth) / (maxWidth - minWidth)).clamp(0.0, 1.0);

    return min + ((max - min) * factor);
  }

  double scaleHeight({
    required double min,
    required double max,
    double minHeight = 550,
    double maxHeight = 1000,
  }) {
    final factor = ((height - minHeight) / (maxHeight - minHeight)).clamp(
      0.0,
      1.0,
    );

    return min + ((max - min) * factor);
  }

  // =========================================================
  // ESPACEMENTS
  // =========================================================

  double get spaceXXS => scale(min: 2, max: 4);

  double get spaceXS => scale(min: 3, max: 6);

  double get spaceS => scale(min: 5, max: 10);

  double get spaceM => scale(min: 8, max: 16);

  double get spaceL => scale(min: 12, max: 24);

  double get spaceXL => scale(min: 18, max: 32);

  // =========================================================
  // PADDING
  // =========================================================

  double get paddingXS => scale(min: 4, max: 8);

  double get paddingS => scale(min: 6, max: 12);

  double get paddingM => scale(min: 8, max: 16);

  double get paddingL => scale(min: 12, max: 20);

  double get paddingXL => scale(min: 20, max: 30);

  // =========================================================
  // TYPOGRAPHIE
  // =========================================================

  // Très petit texte :
  // labels secondaires, indication "obligatoire", etc.
  double get textXXS => scale(min: 5, max: 10);
  double get textXS => scale(min: 9, max: 11);

  // Texte secondaire
  double get textS => scale(min: 10, max: 13);

  // Texte normal
  double get textM => scale(min: 12, max: 15);

  // Texte important / nom joueur
  double get textL => scale(min: 14, max: 17);

  // Petit titre
  double get titleS => scale(min: 16, max: 19);

  // Titre standard
  double get titleM => scale(min: 18, max: 22);

  // Gros titre
  double get titleL => scale(min: 21, max: 28);

  double get titleXL => scale(min: 28, max: 35);

  // =========================================================
  // SCORE / TIMER
  // =========================================================

  double get scoreS => scale(min: 18, max: 24);

  double get scoreSM => scale(min: 26, max: 34);

  double get scoreM => scale(min: 36, max: 44);

  double get scoreL => scale(min: 46, max: 56);

  double get timerSize => scale(min: 34, max: 48);

  // =========================================================
  // ICONES
  // =========================================================

  double get iconXS => scale(min: 14, max: 17);

  double get iconS => scale(min: 16, max: 20);

  double get iconM => scale(min: 18, max: 24);

  double get iconL => scale(min: 22, max: 30);

  // =========================================================
  // BORDURES
  // =========================================================

  double get radiusS => scale(min: 8, max: 12);

  double get radiusM => scale(min: 12, max: 18);

  double get radiusL => scale(min: 16, max: 24);

  // =========================================================
  // BOUTONS
  // =========================================================

  double get buttonHeightS => scale(min: 34, max: 40);

  double get buttonHeightM => scale(min: 40, max: 48);

  double get buttonHeightL => scale(min: 48, max: 56);

  // =========================================================
  // ELEMENTS SPECIFIQUES AU MATCH
  // =========================================================

  double get sidePointButtonWidth => scale(min: 64, max: 150);

  double get controlPanelWidth => scale(min: 220, max: 300);

  double get bottomCardHeight => scale(min: 115, max: 165);

  double get dialogWidthS => scale(min: 330, max: 440);

  double get dialogWidthM => scale(min: 380, max: 620);

  double get dialogWidthL => scale(min: 480, max: 760);

  double get headerHeight => scale(min: 48, max: 68);

  double get teamColumnsMinWidth => 700;
}
