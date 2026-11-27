import 'package:flutter_test/flutter_test.dart';
import 'package:wa_lead_saver_ui/src/data/page_guard.dart';

void main() {
  test('deduplicates overlap and duplicate rows within a page', () {
    final guard = PageGuard<int>((value) => value);
    expect(guard.unique([1, 2, 2]), [1, 2]);
    expect(guard.unique([2, 3, 1, 4]), [3, 4]);
  });
  test('stops repeated cursor cycles', () {
    final guard = PageGuard<int>((value) => value);
    expect(guard.canContinue(true, true, 'a'), isTrue);
    expect(guard.canContinue(true, true, 'b'), isTrue);
    expect(guard.canContinue(true, true, 'a'), isFalse);
  });
  test('terminal and malformed pages stop pagination', () {
    final guard = PageGuard<int>((value) => value);
    expect(guard.canContinue(false, true, 'a'), isFalse);
    expect(guard.canContinue(true, false, 'a'), isFalse);
    expect(guard.canContinue(true, true, null), isFalse);
    expect(guard.canContinue(true, true, ''), isFalse);
  });
  test('new filter resets both ID and cursor history', () {
    final guard = PageGuard<int>((value) => value);
    guard.unique([1]);
    guard.canContinue(true, true, 'a');
    guard.reset();
    expect(guard.unique([1]), [1]);
    expect(guard.canContinue(true, true, 'a'), isTrue);
  });
}
