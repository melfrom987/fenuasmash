import 'dart:convert';
import 'dart:io';

import 'package:badminton_umpire/features/match/domain/models/match_draft.dart';
import 'package:path_provider/path_provider.dart';
import 'package:flutter/foundation.dart';

class MatchDraftService {
  static const String _fileName = 'current_match.json';

  Future<File> _getFile() async {
    final directory = await getApplicationDocumentsDirectory();

    return File('${directory.path}/$_fileName');
  }

  Future<void> save(MatchDraft draft) async {
    final file = await _getFile();

    await file.writeAsString(jsonEncode(draft.toJson()), flush: true);
  }

  Future<MatchDraft?> load() async {
    try {
      final file = await _getFile();

      if (!await file.exists()) {
        return null;
      }

      final content = await file.readAsString();

      if (content.trim().isEmpty) {
        return null;
      }

      final decoded = jsonDecode(content);

      if (decoded is! Map<String, dynamic>) {
        return null;
      }

      return MatchDraft.fromJson(decoded);
    } catch (error) {
      debugPrint('Unable to load current match: $error');
      return null;
    }
  }

  Future<bool> exists() async {
    final file = await _getFile();

    return file.exists();
  }

  Future<void> clear() async {
    final file = await _getFile();

    if (await file.exists()) {
      await file.delete();
    }
  }
}
