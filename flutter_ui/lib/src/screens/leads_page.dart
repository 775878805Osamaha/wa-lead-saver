import 'dart:async';

import 'package:flutter/material.dart';
import '../data/gateway.dart';
import '../theme/wa_theme.dart';
import '../widgets/shared.dart';

class LeadsPage extends StatefulWidget {
  const LeadsPage({
    super.key, required this.gateway, this.reviewOnly = false,
  });
  final LeadGateway gateway;
  final bool reviewOnly;

  @override
  State<LeadsPage> createState() => _LeadsPageState();
}

class _LeadsPageState extends State<LeadsPage> {
  static const int pageSize = 40;
  final TextEditingController _search = TextEditingController();
  final ScrollController _scroll = ScrollController();
  Timer? _debounce;
  List<LeadEntry> _items = [];
  String? _cursor;
  String _status = '';
  bool _loading = false;
  bool _hasMore = true;
  int _total = 0;
  int _generation = 0;
  String? _error;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_maybeLoadMore);
    _load(reset: true);
  }

  void _maybeLoadMore() {
    if (_scroll.hasClients && _scroll.position.extentAfter < 480) _load();
  }

  void _setStatus(String value) {
    if (_status == value) return;
    setState(() => _status = value);
    _load(reset: true);
  }

  void _onSearch(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 320), () {
      if (mounted) _load(reset: true);
    });
  }

  Future<void> _load({bool reset = false}) async {
    if (reset) {
      ++_generation;
      _items = [];
      _cursor = null;
      _hasMore = true;
      _error = null;
      _loading = false;
    }
    if (_loading || !_hasMore) return;
    final generation = _generation;
    final currentCursor = _cursor;
    setState(() => _loading = true);
    try {
      final response = await widget.gateway.loadLeads(
        query: _search.text.trim(),
        status: _status,
        cursor: currentCursor,
        limit: pageSize,
        reviewOnly: widget.reviewOnly,
      );
      if (!mounted || generation != _generation) return;
      setState(() {
        _items = [..._items, ...response.items];
        _total = response.total;
        _cursor = response.nextCursor;
        _hasMore = response.hasMore &&
            response.items.isNotEmpty() &&
            response.nextCursor != null &&
            response.nextCursor != currentCursor;
        _error = null;
      });
    } catch (_) {
      if (mounted && generation == _generation) {
        setState(() => _error = 'تعذّر تحميل العملاء. تحقق من اتصال الخدمة الأصلية.');
      }
    } finally {
      if (mounted && generation == _generation) setState(() => _loading = false);
    }
  }

  Future<void> _save(LeadEntry lead) async {
    try {
      await widget.gateway.saveLead(lead.id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تم حفظ جهة الاتصال بنجاح')),
      );
      await _load(reset: true);
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('لم يكتمل الحفظ. تحقق من الصلاحيات ثم أعد المحاولة')),
      );
    }
  }

  void _showDetails(LeadEntry lead) {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(22),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(lead.name, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 8),
              SelectableText(lead.phone, textDirection: TextDirection.ltr),
              const SizedBox(height: 10),
              Text('الحالة: ${lead.status}'),
              Text('المصدر: ${lead.source}'),
              if (widget.reviewOnly && !lead.isSaved) ...[
                const SizedBox(height: 18),
                FilledButton.icon(
                  onPressed: () {
                    Navigator.pop(context);
                    _save(lead);
                  },
                  icon: const Icon(Icons.person_add_alt_1),
                  label: const Text('حفظ جهة الاتصال'),
                ),
              ],
              const SizedBox(height: 18),
            ],
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _search.dispose();
    _scroll.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Column(
    children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            WaSectionHeader(
              title: widget.reviewOnly ? 'قائمة المراجعة ($_total)' : 'العملاء ($_total)',
              subtitle: widget.reviewOnly
                  ? 'التحقق من الرقم قبل إضافته إلى جهات الاتصال'
                  : 'ابحث وتابع العملاء من شاشة واحدة',
            ),
            TextField(
              key: const Key('lead_search'),
              controller: _search,
              onChanged: _onSearch,
              textInputAction: TextInputAction.search,
              decoration: const InputDecoration(
                hintText: 'البحث بالاسم أو رقم الهاتف',
                prefixIcon: Icon(Icons.search),
              ),
            ),
            if (!widget.reviewOnly) ...[
              const SizedBox(height: 10),
              SizedBox(
                height: 42,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  children: [
                    for (final option in const <(String, String)>[
                      ('', 'الكل'),
                      ('NEW', 'جديد'),
                      ('CONTACTED', 'تم التواصل'),
                      ('FOLLOW_UP', 'متابعة'),
                      ('INTERESTED', 'مهتم'),
                      ('SOLD', 'تم البيع'),
                    ])
                      Padding(
                        padding: const EdgeInsets.only(left: 6),
                        child: ChoiceChip(
                          label: Text(option.$2),
                          selected: _status == option.$1,
                          onSelected: (_) => _setStatus(option.$1),
                        ),
                      ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
      Expanded(
        child: _items.isEmpty
            ? WaFeedback(
                message: _error ??
                    (_loading ? 'جاري تحميل العملاء' : 'لا توجد نتائج مطابقة'),
                loading: _loading,
                onRetry: _error == null ? null : () => _load(reset: true),
              )
            : RefreshIndicator(
                onRefresh: () => _load(reset: true),
                child: ListView.builder(
                  key: const Key('leads_list'),
                  controller: _scroll,
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                  itemCount: _items.length + ((_loading || _error != null || _hasMore) ? 1 : 0),
                  itemBuilder: (context, index) {
                    if (index == _items.length) {
                      return Padding(
                        padding: const EdgeInsets.all(16),
                        child: _error != null
                            ? WaFeedback(
                                message: _error!,
                                onRetry: () => _load(),
                              )
                            : Center(
                                child: _loading
                                    ? const CircularProgressIndicator()
                                    : TextButton(
                                        onPressed: () => _load(),
                                        child: const Text('عرض المزيد'),
                                      ),
                              ),
                      );
                    }
                    final lead = _items[index];
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: Card(
                        child: InkWell(
                          borderRadius: BorderRadius.circular(20),
                          onTap: () => _showDetails(lead),
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Row(
                                  children: [
                                    CircleAvatar(
                                      backgroundColor: WaPalette.raised,
                                      child: Text(
                                        lead.name.isEmpty ? '؟' : lead.name.characters.first,
                                        style: const TextStyle(color: WaPalette.green),
                                      ),
                                    ),
                                    const SizedBox(width: 10),
                                    Expanded(
                                      child: Text(
                                        lead.name.isEmpty ? 'رقم غير مسمى' : lead.name,
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: const TextStyle(
                                          fontSize: 17, fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ),
                                    if (widget.reviewOnly)
                                      const Icon(Icons.pending_actions,
                                        color: WaPalette.amber),
                                  ],
                                ),
                                const SizedBox(height: 12),
                                Align(
                                  alignment: Alignment.centerRight,
                                  child: Text(
                                    lead.phone,
                                    textDirection: TextDirection.ltr,
                                    style: const TextStyle(color: WaPalette.muted),
                                  ),
                                ),
                                const SizedBox(height: 12),
                                Row(
                                  children: [
                                    Expanded(
                                      child: Text(
                                        lead.source.isNotEmpty
                                            ? lead.source : lead.status,
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: const TextStyle(
                                          color: WaPalette.muted, fontSize: 12,
                                        ),
                                      ),
                                    ),
                                    if (widget.reviewOnly && !lead.isSaved)
                                      FilledButton(
                                        onPressed: () => _save(lead),
                                        child: const Text('حفظ جهة الاتصال'),
                                      )
                                    else
                                      const Icon(Icons.chevron_left,
                                        color: WaPalette.green),
                                  ],
                                ),
                              ],
                            ),
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
