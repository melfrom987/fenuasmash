/// What a score event records, as sent to the backend
/// (`ScoreEventType` in `contracts/openapi.yaml`).
enum ScoreEventType {
  /// A rally won by [ScoreEvent.side].
  point('POINT'),

  /// Cancels the last point that is not cancelled yet.
  undo('UNDO');

  const ScoreEventType(this.apiValue);

  final String apiValue;

  static ScoreEventType fromApiValue(String value) {
    return values.firstWhere(
      (type) => type.apiValue == value,
      orElse: () => throw FormatException('Unknown score event type', value),
    );
  }
}

/// One of the two players or pairs of a match (`MatchSide` in the contract).
enum MatchSide {
  a('A', 'teamA'),
  b('B', 'teamB');

  const MatchSide(this.apiValue, this.teamId);

  final String apiValue;

  /// Team id used by the match controller.
  final String teamId;

  static MatchSide fromTeamId(String teamId) {
    return values.firstWhere(
      (side) => side.teamId == teamId,
      orElse: () => throw ArgumentError.value(teamId, 'teamId'),
    );
  }

  static MatchSide fromApiValue(String value) {
    return values.firstWhere(
      (side) => side.apiValue == value,
      orElse: () => throw FormatException('Unknown match side', value),
    );
  }
}

/// A point or an undo entered by the umpire, with a unique id generated on the
/// tablet and its position in the match.
///
/// The backend applies events in [sequence] order and ignores an event it has
/// already received with the same [id], so the tablet can send the same event
/// again after a network cut without counting the point twice.
class ScoreEvent {
  /// Unique id (UUID), kept when the event is sent again.
  final String id;

  /// Position of the event in the match, from 1, without gaps.
  final int sequence;

  final ScoreEventType type;

  /// The side that won the rally; set for points only.
  final MatchSide? side;

  /// When the umpire entered the event, in UTC.
  final DateTime occurredAt;

  ScoreEvent({
    required this.id,
    required this.sequence,
    required this.type,
    required this.side,
    required DateTime occurredAt,
  }) : occurredAt = occurredAt.toUtc() {
    if (sequence < 1) {
      throw ArgumentError.value(sequence, 'sequence', 'must be at least 1');
    }
    if ((type == ScoreEventType.point) != (side != null)) {
      throw ArgumentError('A point needs a side, an undo has none');
    }
  }

  /// JSON as expected by `POST /matches/{matchId}/events`.
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'sequence': sequence,
      'type': type.apiValue,
      if (side != null) 'side': side!.apiValue,
      'occurredAt': occurredAt.toIso8601String(),
    };
  }

  factory ScoreEvent.fromJson(Map<String, dynamic> json) {
    final side = json['side'] as String?;

    return ScoreEvent(
      id: json['id'] as String,
      sequence: json['sequence'] as int,
      type: ScoreEventType.fromApiValue(json['type'] as String),
      side: side == null ? null : MatchSide.fromApiValue(side),
      occurredAt: DateTime.parse(json['occurredAt'] as String),
    );
  }
}
