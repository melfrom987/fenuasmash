import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/core/utils/formatters.dart';
import 'package:badminton_umpire/features/history/domain/models/match_history_entry.dart';
import 'package:badminton_umpire/features/history/presentation/pages/match_history_details_page.dart';
import 'package:badminton_umpire/features/history/data/services/match_history_service.dart';
import 'package:badminton_umpire/features/history/presentation/widgets/match_history_card.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

class MatchHistoryPage extends StatefulWidget {
  const MatchHistoryPage({super.key});

  @override
  State<MatchHistoryPage> createState() => _MatchHistoryPageState();
}

class _MatchHistoryPageState extends State<MatchHistoryPage> {
  final MatchHistoryService _historyService = MatchHistoryService();

  List<MatchHistoryEntry> _matches = [];
  bool _isLoading = true;
  bool _selectionMode = false;

  final Set<String> _selectedMatchIds = {};

  void _toggleSelection(MatchHistoryEntry match) {
    setState(() {
      if (_selectedMatchIds.contains(match.id)) {
        _selectedMatchIds.remove(match.id);
      } else {
        _selectedMatchIds.add(match.id);
      }

      _selectionMode = _selectedMatchIds.isNotEmpty;
    });
  }

  void _cancelSelection() {
    setState(() {
      _selectionMode = false;
      _selectedMatchIds.clear();
    });
  }

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  Future<void> _loadHistory() async {
    final matches = await _historyService.loadHistory();

    matches.sort((a, b) => b.playedAt.compareTo(a.playedAt));

    if (!mounted) {
      return;
    }

    setState(() {
      _matches = matches;
      _isLoading = false;
    });
  }

  Map<DateTime, List<MatchHistoryEntry>> _groupMatchesByDate() {
    final groups = <DateTime, List<MatchHistoryEntry>>{};

    for (final match in _matches) {
      final date = DateTime(
        match.playedAt.year,
        match.playedAt.month,
        match.playedAt.day,
      );

      groups.putIfAbsent(date, () => []);
      groups[date]!.add(match);
    }

    return groups;
  }

  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F8),
      appBar: AppBar(
        leading: _selectionMode
            ? IconButton(
                onPressed: _cancelSelection,
                icon: const Icon(Icons.close),
              )
            : null,
        title: Text(
          _selectionMode
              ? '${_selectedMatchIds.length} ${l10n.selected}'
              : l10n.matchHistory,
          style: const TextStyle(fontWeight: FontWeight.w900),
        ),
        actions: _selectionMode
            ? [
                IconButton(
                  tooltip: l10n.delete,
                  onPressed: _selectedMatchIds.isEmpty
                      ? null
                      : _confirmDeleteSelectedMatches,
                  icon: const Icon(Icons.delete_outline),
                ),
              ]
            : [
                PopupMenuButton<String>(
                  onSelected: (value) {
                    switch (value) {
                      case 'select':
                        setState(() {
                          _selectionMode = true;
                        });
                        break;

                      case 'clear':
                        _confirmClearHistory();
                        break;
                    }
                  },
                  itemBuilder: (context) => [
                    PopupMenuItem(
                      value: 'select',
                      child: Row(
                        children: [
                          const Icon(Icons.checklist),
                          const SizedBox(width: 8),
                          Text(l10n.selectMatches),
                        ],
                      ),
                    ),
                    PopupMenuItem(
                      value: 'clear',
                      child: Row(
                        children: [
                          const Icon(Icons.delete_sweep_outlined),
                          const SizedBox(width: 8),
                          Text(l10n.clearHistory),
                        ],
                      ),
                    ),
                  ],
                ),
              ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _matches.isEmpty
          ? _EmptyHistory()
          : _buildHistory(responsive),
    );
  }

  Future<void> _confirmDeleteSelectedMatches() async {
    if (_selectedMatchIds.isEmpty) {
      return;
    }

    final l10n = AppLocalizations.of(context)!;
    final errorColor = Theme.of(context).colorScheme.error;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          icon: Icon(Icons.delete_outline, color: errorColor),
          title: Text(l10n.deleteSelectedMatches),
          content: Text(
            l10n.deleteSelectedMatchesConfirmation(_selectedMatchIds.length),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(dialogContext).pop(false);
              },
              child: Text(l10n.cancel),
            ),
            FilledButton(
              style: FilledButton.styleFrom(backgroundColor: errorColor),
              onPressed: () {
                Navigator.of(dialogContext).pop(true);
              },
              child: Text(l10n.delete),
            ),
          ],
        );
      },
    );

    if (confirmed != true) {
      return;
    }

    await _historyService.deleteMatches(_selectedMatchIds);

    if (!mounted) {
      return;
    }

    setState(() {
      _matches.removeWhere((match) => _selectedMatchIds.contains(match.id));

      _selectedMatchIds.clear();
      _selectionMode = false;
    });
  }

  Future<void> _confirmClearHistory() async {
    if (_matches.isEmpty) {
      return;
    }

    final l10n = AppLocalizations.of(context)!;
    final errorColor = Theme.of(context).colorScheme.error;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          icon: Icon(Icons.delete_sweep_outlined, color: errorColor),
          title: Text(l10n.clearHistory),
          content: Text(l10n.clearHistoryConfirmation),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(dialogContext).pop(false);
              },
              child: Text(l10n.cancel),
            ),
            FilledButton(
              style: FilledButton.styleFrom(backgroundColor: errorColor),
              onPressed: () {
                Navigator.of(dialogContext).pop(true);
              },
              child: Text(l10n.delete),
            ),
          ],
        );
      },
    );

    if (confirmed != true) {
      return;
    }

    await _historyService.clearHistory();

    if (!mounted) {
      return;
    }

    setState(() {
      _matches.clear();
      _selectedMatchIds.clear();
      _selectionMode = false;
    });
  }

  Widget _buildHistory(Responsive responsive) {
    final groups = _groupMatchesByDate();

    return RefreshIndicator(
      onRefresh: _loadHistory,
      child: ListView.builder(
        padding: EdgeInsets.all(responsive.paddingL),
        itemCount: groups.length,
        itemBuilder: (context, index) {
          final date = groups.keys.elementAt(index);
          final matches = groups[date]!;

          return Padding(
            padding: EdgeInsets.only(bottom: responsive.spaceL),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  AppFormatters.date(date),
                  style: TextStyle(
                    fontSize: responsive.textS,
                    fontWeight: FontWeight.w900,
                    color: Colors.grey.shade700,
                  ),
                ),

                SizedBox(height: responsive.spaceS),

                ...matches.map(
                  (match) => Padding(
                    padding: EdgeInsets.only(bottom: responsive.spaceS),
                    child: MatchHistoryCard(
                      match: match,
                      selectionMode: _selectionMode,
                      selected: _selectedMatchIds.contains(match.id),

                      onTap: () {
                        if (_selectionMode) {
                          _toggleSelection(match);
                          return;
                        }

                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (context) {
                              return MatchHistoryDetailsPage(match: match);
                            },
                          ),
                        );
                      },

                      onLongPress: () {
                        _toggleSelection(match);
                      },
                    ),
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}

class _EmptyHistory extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final responsive = Responsive.of(context);
    final l10n = AppLocalizations.of(context)!;

    return Center(
      child: Padding(
        padding: EdgeInsets.all(responsive.paddingXL),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.history,
              size: responsive.scale(min: 50, max: 80),
              color: Colors.grey.shade400,
            ),
            SizedBox(height: responsive.spaceM),
            Text(
              l10n.noMatchHistory,
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: responsive.titleM,
                fontWeight: FontWeight.w800,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
