import 'package:badminton_umpire/core/utils/uuid.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('generates version 4 UUIDs', () {
    final uuidV4 = RegExp(
      r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
    );

    for (var i = 0; i < 100; i++) {
      expect(generateUuidV4(), matches(uuidV4));
    }
  });

  test('never repeats an id', () {
    final ids = {for (var i = 0; i < 10000; i++) generateUuidV4()};

    expect(ids, hasLength(10000));
  });
}
