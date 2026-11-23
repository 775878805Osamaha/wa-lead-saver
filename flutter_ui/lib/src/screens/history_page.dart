import 'package:flutter/material.dart';
import '../data/gateway.dart';
import '../theme/wa_theme.dart';
import '../widgets/shared.dart';

class HistoryPageView extends StatefulWidget {
  const HistoryPageView({super.key, required this.gateway});
  final LeadGateway gateway;

  @override
  State<HistoryPageView> createState() => _HistoryPageViewState();
}

class _HistoryPageViewState extends State<HistoryPageView> with WidgetsBindingObserver {
  final ScrollController _scroll = ScrollController();
  List<HistoryEntry> _records = [];
  String _filter = 'ALL';
  String? _cursor;
  bool _loading = false;
  bool _hasMore = true;
  String? _error;
  int _total = 0;
  int _generation = 0;
  bool _actionInProgress = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _scroll.addListener(() {
      if (_scroll.hasClients && _scroll.position.extentAfter < 420) _load();
    });
    _load(reset: true);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _load(reset: true);
  }

  Future<void> _load({bool reset = false}) async {
    if (reset) {
      ++_generation;
      _records = [];
      _cursor = null;
      _error = null;
      _loading = false;
      _hasMore = true;
    }
    if (_loading || !_hasMore) return;
    final generation = _generation;
    final cursor = _cursor;
    setState(() => _loading = true);
    try {
      final result = await widget.gateway.loadHistory(
        filter: _filter,
        cursor: cursor,
        limit: 60,
      );
      if (!mounted || generation != _generation) return;
      setState(() {
        _records = [..._records, ...result.items];
        _total = result.total;
        _cursor = result.nextCursor;
        _hasMore = result.hasMore && result.items.isNotEmpty &&
            result.nextCursor != null && result.nextCursor != cursor;
        _error = null;
      });
    } catch (_) {
      if (mounted && generation == _generation) {
        setState(() => _error = 'تعذر قراءة السجل. أعد المحاولة بعد التحقق من الاتصال.');
      }
    } finally {
      if (mounted && generation == _generation) setState(() => _loading = false);
    }
  }

  String _arabicStatus(String status) {
    switch (status.trim().toLowerCase()) {
      case 'saved': return 'تم الحفظ';
      case 'needs review': return 'بانتظار المراجعة';
      case 'duplicate': return 'مكرر';
      case 'group excluded': return 'مجموعة مستبعدة';
      case 'excluded': return 'مستبعد';
      case 'failed': return 'فشل الحفظ';
      case 'invalid number': return 'رقم غير صالح';
      default: return status;
    }
  }

  String _date(int ms) {
    if (ms <= 0) return 'تاريخ غير متاح';
    final d = DateTime.fromMillisecondsSinceEpoch(ms);
    String two(int value) => value.toString().padLeft(2, '0');
    return '${d.year}/${two(d.month)}/${two(d.day)}  ${two(d.hour)}:${two(d.minute)}';
  }

  Future<void> _exportHistory() async {
    if (_actionInProgress) return;
    setState(() => _actionInProgress = true);
    try {
      await widget.gateway.exportHistory();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تم تجهيز ملف CSV — اختر تطبيق المشاركة')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تعذر تصدير السجل؛ العملية غير متاحة في المعاينة')),
      );
    } finally {
      if (mounted) setState(() => _actionInProgress = false);
    }
  }

  Future<void> _confirmClearHistory() async {
    if (_actionInProgress) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('مسح سجل العمليات؟'),
        content: const Text(
          'سيتم حذف سجل العمليات المحلي بالكامل. لا يمكن التراجع عن الحذف. '
          'لن تُحذف جهات الاتصال المحفوظة.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('إلغاء'),
          ),
          FilledButton(
            key: const Key('confirm_clear_history'),
            style: FilledButton.styleFrom(
              backgroundColor: WaPalette.red, foregroundColor: Colors.white,
            ),
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('تأكيد المسح'),
          ),
        ],
      ),
    );
    if (!mounted || confirmed != true) return;
    setState(() => _actionInProgress = true);
    try {
      await widget.gateway.clearHistory();
      if (!mounted) return;
      await _load(reset: true);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تم مسح سجل العمليات بنجاح')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تعذر مسح السجل؛ لم يتم حذف بياناتك')),
      );
    } finally {
      if (mounted) setState(() => _actionInProgress = false);
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _scroll.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Column(
    children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(12, 6, 12, 6),
        child: Column(
          children: [
            WaSectionHeader(
              title: 'سجل العمليات ($_total)',
              subtitle: 'تحميل تدريجي دون قراءة السجل بالكامل',
            ),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _actionInProgress ? null : _confirmClearHistory,
                    icon: const Icon(Icons.delete_outline, size: 17),
                    label: const Text('مسح السجل'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: WaPalette.red,
                      minimumSize: const Size(0, 42),
                    ),
                  ),
                ),
                const SizedBox(width: 9),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _actionInProgress ? null : _exportHistory,
                    icon: const Icon(Icons.file_download_outlined, size: 17),
                    label: const Text('تصدير CSV'),
                    style: OutlinedButton.styleFrom(
                      minimumSize: const Size(0, 42),
                      backgroundColor: WaPalette.raised,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            SizedBox(
              height: 44,
              child: ListView(
                scrollDirection: Axis.horizontal,
                children: [
                  for (final option in const <(String, String)>[
                    ('ALL', 'الكل'),
                    ('SAVED', 'حُفظ'),
                    ('DUPLICATE', 'مكرر'),
                    ('NEEDS_REVIEW', 'مراجعة'),
                    ('GROUP_EXCLUDED', 'المجموعات'),
                    ('EXCLUDED', 'مستبعد'),
                  ])
                    Padding(
                      padding: const EdgeInsets.only(left: 6),
                      child: ChoiceChip(
                        label: Text(option.$2),
                        selected: _filter == option.$1,
                        onSelected: (_) {
                          if (_filter == option.$1) return;
                          setState(() => _filter = option.$1);
                          _load(reset: true);
                        },
                      ),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
      Expanded(
        child: _records.isEmpty
            ? WaFeedback(
                message: _error ??
                    (_loading ? 'جاري تحميل سجل العمليات' : 'لا توجد عمليات مطابقة'),
                loading: _loading,
                onRetry: _error == null ? null : () => _load(reset: true),
              )
            : RefreshIndicator(
                onRefresh: () => _load(reset: true),
                child: ListView.builder(
                  key: const Key('history_list'),
                  controller: _scroll,
                  padding: const EdgeInsets.fromLTRB(12, 8, 12, 18),
                  itemCount: _records.length + ((_loading || _hasMore || _error != null) ? 1 : 0),
                  itemBuilder: (context, index) {
                    if (index == _records.length) {
                      if (_error != null) {
                        return WaFeedback(message: _error!, onRetry: () => _load());
                      }
                      return Padding(
                        padding: const EdgeInsets.all(18),
                        child: Center(
                          child: _loading
                              ? const CircularProgressIndicator()
                              : TextButton(
                                  onPressed: () => _load(),
                                  child: const Text('تحميل المزيد'),
                                ),
                        ),
                      );
                    }
                    final entry = _records[index];
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: Card(
                        child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Expanded(
                                    child: Text(
                                      _arabicStatus(entry.status),
                                      style: const TextStyle(
                                        fontSize: 16, fontWeight: FontWeight.bold,
                                        color: WaPalette.green,
                                      ),
                                    ),
                                  ),
                                  Text(entry.source, style: const TextStyle(
                                    color: WaPalette.muted, fontSize: 12,
                                  )),
                                ],
                              ),
                              if (entry.details.isNotEmpty) ...[
                                const SizedBox(height: 10),
                                Text(entry.details, maxLines: 3,
                                  overflow: TextOverflow.ellipsis),
                              ],
                              const SizedBox(height: 12),
                              Text(_date(entry.timestamp),
                                style: const TextStyle(
                                  color: WaPalette.muted, fontSize: 12,
                                )),
                            ],
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
      ),
    ],
  );
}
