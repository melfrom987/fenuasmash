import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Adds the licenses of the bundled fonts to the app license page, as the SIL
/// Open Font License requires.
void registerFontLicenses() {
  LicenseRegistry.addLicense(() async* {
    for (final font in ['Inter', 'Outfit']) {
      yield LicenseEntryWithLineBreaks([
        font,
      ], await rootBundle.loadString('assets/fonts/OFL-$font.txt'));
    }
  });
}
