import 'package:badminton_umpire/core/responsive/responsive.dart';
import 'package:badminton_umpire/features/match/presentation/widgets/court/player_card.dart';
import 'package:flutter/material.dart';

class BadmintonCourt extends StatelessWidget {
  final List<String> leftPlayers;
  final List<String> rightPlayers;
  final String serverName;
  final String receiverName;
  final Map<String, Color> playerColors;
  final ValueChanged<String>? onPlayerTap;

  const BadmintonCourt({
    super.key,
    required this.leftPlayers,
    required this.rightPlayers,
    required this.serverName,
    required this.receiverName,
    required this.playerColors,
    this.onPlayerTap,
  });

  @override
  Widget build(BuildContext context) {
    final leftTop = leftPlayers.isNotEmpty ? leftPlayers[0] : '';

    final leftBottom = leftPlayers.length > 1 ? leftPlayers[1] : '';

    final rightTop = rightPlayers.isNotEmpty ? rightPlayers[0] : '';

    final rightBottom = rightPlayers.length > 1 ? rightPlayers[1] : '';
    final responsive = Responsive.of(context);

    return Container(
      width: double.infinity,
      height: double.infinity,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: const Color(0xFF159A62),
        borderRadius: BorderRadius.circular(responsive.radiusS),
        border: Border.all(color: const Color(0xFF0D7A4C), width: 2),
      ),
      child: Stack(
        children: [
          const Positioned.fill(child: _SimpleCourtLines()),
          Positioned.fill(
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    children: [
                      Expanded(
                        child: _PlayerZone(
                          playerName: leftTop,
                          isServer: leftTop == serverName,
                          isReceiver: leftTop == receiverName,
                          shirtColor: playerColors[leftTop] ?? Colors.grey,
                          onTap: leftTop.isEmpty || onPlayerTap == null
                              ? null
                              : () => onPlayerTap!(leftTop),
                        ),
                      ),
                      Expanded(
                        child: _PlayerZone(
                          playerName: leftBottom,
                          isServer: leftBottom == serverName,
                          isReceiver: leftBottom == receiverName,
                          shirtColor: playerColors[leftBottom] ?? Colors.grey,
                          onTap: leftBottom.isEmpty || onPlayerTap == null
                              ? null
                              : () => onPlayerTap!(leftBottom),
                        ),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    children: [
                      Expanded(
                        child: _PlayerZone(
                          playerName: rightTop,
                          isServer: rightTop == serverName,
                          isReceiver: rightTop == receiverName,
                          shirtColor: playerColors[rightTop] ?? Colors.grey,
                          onTap: rightTop.isEmpty || onPlayerTap == null
                              ? null
                              : () => onPlayerTap!(rightTop),
                        ),
                      ),
                      Expanded(
                        child: _PlayerZone(
                          playerName: rightBottom,
                          isServer: rightBottom == serverName,
                          isReceiver: rightBottom == receiverName,
                          shirtColor: playerColors[rightBottom] ?? Colors.grey,
                          onTap: rightBottom.isEmpty || onPlayerTap == null
                              ? null
                              : () => onPlayerTap!(rightBottom),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SimpleCourtLines extends StatelessWidget {
  const _SimpleCourtLines();

  @override
  Widget build(BuildContext context) {
    return CustomPaint(painter: _SimpleCourtPainter());
  }
}

class _SimpleCourtPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final linePaint = Paint()
      ..color = Colors.white.withValues(alpha: 0.8)
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke;

    final outerRect = Rect.fromLTWH(10, 10, size.width - 20, size.height - 20);

    canvas.drawRect(outerRect, linePaint);

    canvas.drawLine(
      Offset(size.width / 2, 10),
      Offset(size.width / 2, size.height - 10),
      linePaint,
    );

    canvas.drawLine(
      Offset(10, size.height / 2),
      Offset(size.width - 10, size.height / 2),
      linePaint,
    );
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) {
    return false;
  }
}

class _PlayerZone extends StatelessWidget {
  final String playerName;
  final bool isServer;
  final bool isReceiver;
  final Color shirtColor;
  final VoidCallback? onTap;

  const _PlayerZone({
    required this.playerName,
    required this.isServer,
    required this.isReceiver,
    required this.shirtColor,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    if (playerName.isEmpty) {
      return const SizedBox.expand();
    }

    return Padding(
      padding: const EdgeInsets.all(8),
      child: Center(
        child: PlayerCard(
          playerName: playerName,
          isServer: isServer,
          isReceiver: isReceiver,
          shirtColor: shirtColor,
          onTap: onTap,
        ),
      ),
    );
  }
}
