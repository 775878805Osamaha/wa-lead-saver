/// Per-query pagination state. Overlapping pages cannot duplicate rows, and a
/// repeated cursor cannot trap the UI in a never-ending pagination loop.
class PageGuard<T> {
  PageGuard(this.idOf);
  final int Function(T) idOf;
  final Set<int> _ids = {};
  final Set<String> _cursors = {};

  List<T> unique(Iterable<T> items) =>
      items.where((item) => _ids.add(idOf(item))).toList();

  bool canContinue(bool hasMore, bool hasItems, String? nextCursor) =>
      hasMore && hasItems && nextCursor != null &&
      nextCursor.isNotEmpty && _cursors.add(nextCursor);

  void reset() {
    _ids.clear();
    _cursors.clear();
  }
}
