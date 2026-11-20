import 'dart:convert';
import 'dart:io';

import 'package:path_provider/path_provider.dart';

import '../../domain/models/match_history_entry.dart';

class MatchHistoryService {
  static const String _fileName = 'match_history.json';
  static const int _maxMatches = 100;

  Future<File> _getHistoryFile() async {
    final directory = await getApplicationDocumentsDirectory();

    return File('${directory.path}/$_fileName');
  }

  Future<List<MatchHistoryEntry>> loadHistory() async {
    try {
      final file = await _getHistoryFile();

      if (!await file.exists()) {
        return [];
      }

      final content = await file.readAsString();

      if (content.trim().isEmpty) {
        return [];
      }

      final decoded = jsonDecode(content);

      if (decoded is! List) {
        return [];
      }

      return decoded
          .map(
            (item) => MatchHistoryEntry.fromJson(
              Map<String, dynamic>.from(item as Map),
            ),
          )
          .toList();
    } catch (_) {
      return [];
    }
  }

  Future<void> saveHistory(List<MatchHistoryEntry> matches) async {
    final file = await _getHistoryFile();

    final data = matches.map((match) => match.toJson()).toList();

    const encoder = JsonEncoder.withIndent('  ');

    await file.writeAsString(encoder.convert(data), flush: true);
  }

  Future<void> addMatch(MatchHistoryEntry match) async {
    final history = await loadHistory();

    history.insert(0, match);

    if (history.length > _maxMatches) {
      history.removeRange(_maxMatches, history.length);
    }

    await saveHistory(history);
  }

  Future<void> clearHistory() async {
    final file = await _getHistoryFile();

    if (await file.exists()) {
      await file.delete();
    }
  }

  Future<void> deleteMatch(String matchId) async {
    final history = await loadHistory();

    history.removeWhere((match) => match.id == matchId);

    await saveHistory(history);
  }

  Future<void> deleteMatches(Set<String> matchIds) async {
    final history = await loadHistory();

    history.removeWhere((match) => matchIds.contains(match.id));

    await saveHistory(history);
  }
}
