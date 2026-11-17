part of '../match_page.dart';

extension _MatchPageNavigation on _MatchPageState {
  Future<void> _goBack() async {
    if (matchController.matchIsFinished) {
      await _saveMatchHistoryIfNeeded();
      await _queueScoreEvents();
      await _draftService.clear();
    } else {
      await _saveTemporaryMatch();
    }

    if (!mounted) {
      return;
    }

    Navigator.of(context).popUntil((route) => route.isFirst);
  }

  Future<void> _confirmLeaveMatch() async {
    if (matchController.matchIsFinished) {
      await _goBack();
      return;
    }

    final shouldLeave = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        return AlertDialog(
          title: const Text('Quitter le match ?'),
          content: const Text(
            'Le match est toujours en cours. '
            'Êtes-vous sûr de vouloir quitter ?\n\n'
            'Votre match sera sauvegardé et pourra être repris.',
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(dialogContext).pop(false);
              },
              child: const Text('Rester'),
            ),
            FilledButton(
              onPressed: () {
                Navigator.of(dialogContext).pop(true);
              },
              child: const Text('Quitter'),
            ),
          ],
        );
      },
    );

    if (shouldLeave != true) {
      return;
    }

    await _saveTemporaryMatch();

    if (!mounted) {
      return;
    }

    Navigator.of(context).popUntil((route) => route.isFirst);
  }

  Future<void> _startNewMatch() async {
    await _saveMatchHistoryIfNeeded();
    await _queueScoreEvents();
    await _draftService.clear();

    if (!mounted) {
      return;
    }

    Navigator.of(context).pop(true);
  }
}
