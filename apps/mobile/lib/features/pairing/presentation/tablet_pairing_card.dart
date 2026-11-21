import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/pairing/application/tablet_session_controller.dart';
import 'package:badminton_umpire/features/pairing/presentation/pairing_page.dart';
import 'package:badminton_umpire/l10n/app_localizations.dart';
import 'package:flutter/material.dart';

/// Home screen card: whether the tablet is paired with a court, and the way
/// to pair it, pair it again or unpair it.
class TabletPairingCard extends StatelessWidget {
  const TabletPairingCard({super.key, required this.controller});

  final TabletSessionController controller;

  Future<void> _openPairing(BuildContext context) async {
    final messenger = ScaffoldMessenger.of(context);
    final l10n = AppLocalizations.of(context)!;
    final paired = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (context) => PairingPage(controller: controller),
      ),
    );
    final session = controller.session;
    if (paired == true && session != null) {
      messenger.showSnackBar(
        SnackBar(content: Text(l10n.pairingSuccess(session.courtNumber))),
      );
    }
  }

  Future<void> _confirmUnpair(BuildContext context) async {
    final l10n = AppLocalizations.of(context)!;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(l10n.tabletUnpairTitle),
        content: Text(l10n.tabletUnpairMessage),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: Text(l10n.cancel),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: Text(l10n.tabletUnpair),
          ),
        ],
      ),
    );
    if (confirmed == true) {
      await controller.unpair();
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) {
        final l10n = AppLocalizations.of(context)!;
        final responsive = Responsive.of(context);
        final scheme = Theme.of(context).colorScheme;
        final session = controller.session;

        final (icon, color, title, description) = switch (controller.state) {
          TabletPairingState.loading => (
            Icons.link,
            Colors.grey.shade600,
            l10n.tabletPairingLoading,
            '',
          ),
          TabletPairingState.unpaired => (
            Icons.link,
            scheme.primary,
            l10n.tabletNotPairedTitle,
            l10n.tabletNotPairedDescription,
          ),
          TabletPairingState.paired => (
            Icons.check_circle_outline,
            const Color(0xFF2E7D32),
            l10n.tabletPairedTitle(session!.courtNumber),
            l10n.tabletPairedDescription,
          ),
          TabletPairingState.revoked => (
            Icons.link_off,
            scheme.error,
            l10n.tabletRevokedTitle,
            l10n.tabletRevokedDescription,
          ),
        };
        final paired = controller.state == TabletPairingState.paired;
        final loading = controller.state == TabletPairingState.loading;

        return Material(
          color: color.withValues(alpha: 0.06),
          borderRadius: BorderRadius.circular(responsive.radiusL),
          child: InkWell(
            key: const Key('tabletPairingCard'),
            onTap: paired || loading ? null : () => _openPairing(context),
            borderRadius: BorderRadius.circular(responsive.radiusL),
            child: Container(
              padding: EdgeInsets.all(responsive.paddingL),
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(responsive.radiusL),
                border: Border.all(color: color.withValues(alpha: 0.35)),
              ),
              child: Row(
                children: [
                  Icon(icon, color: color, size: responsive.iconL),
                  SizedBox(width: responsive.spaceM),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          title,
                          style: TextStyle(
                            fontSize: responsive.titleS,
                            fontWeight: FontWeight.w900,
                            color: color,
                          ),
                        ),
                        if (description.isNotEmpty) ...[
                          SizedBox(height: responsive.spaceXXS),
                          Text(
                            description,
                            style: TextStyle(
                              fontSize: responsive.textS,
                              color: Colors.grey.shade700,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                  if (paired)
                    PopupMenuButton<String>(
                      key: const Key('tabletPairingMenu'),
                      onSelected: (action) => action == 'pair'
                          ? _openPairing(context)
                          : _confirmUnpair(context),
                      itemBuilder: (context) => [
                        PopupMenuItem(
                          value: 'pair',
                          child: Text(l10n.tabletPairAgain),
                        ),
                        PopupMenuItem(
                          value: 'unpair',
                          child: Text(l10n.tabletUnpair),
                        ),
                      ],
                    )
                  else if (!loading)
                    Icon(Icons.chevron_right, color: color),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
