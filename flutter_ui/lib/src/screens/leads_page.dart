import 'dart:async';

import 'package:flutter/material.dart';
import '../data/gateway.dart';
import '../data/page_guard.dart';
import '../theme/wa_theme.dart';
import '../widgets/shared.dart';

class LeadsPage extends StatefulWidget {
  const LeadsPage({
    super.key, required this.gateway, this.reviewOnly = false,
    this.openManualAdd = false, this.onManualAddHandled,
  });
  final LeadGateway gateway;
  final bool reviewOnly;
  final bool openManualAdd;
  final VoidCallback? onManualAddHandled;

  @override
  State<LeadsPage> createState() => _LeadsPageState();
}

class _LeadsPageState extends State<LeadsPage> with WidgetsBindingObserver {
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
  final _pages = PageGuard<LeadEntry>((item) => item.id);
  bool _searchPending = false;
  bool _manualAddOpen = false;
  final Set<int> _savingIds = {};

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _scroll.addListener(_maybeLoadMore);
    _load(reset: true);
    _scheduleRequestedManualAdd();
  }

  @override
  void didUpdateWidget(covariant LeadsPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!oldWidget.openManualAdd && widget.openManualAdd) {
      _scheduleRequestedManualAdd();
    }
  }

  void _scheduleRequestedManualAdd() {
    if (!widget.openManualAdd || widget.reviewOnly) return;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || !widget.openManualAdd) return;
      widget.onManualAddHandled?.call();
      _addManualLead();
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // Reload original Room records after returning from Android/WhatsApp.
    // Preserve the user's search and selected customer status.
    if (state == AppLifecycleState.resumed) {
      _debounce?.cancel();
      _load(reset: true);
    }
  }

  void _maybeLoadMore() {
    if (_scroll.hasClients && _scroll.position.extentAfter < 480 && _error == null) _load();
  }

  void _setStatus(String value) {
    if (_status == value) return;
    _debounce?.cancel();
    setState(() => _status = value);
    _load(reset: true);
  }

  void _onSearch(String value) {
    _debounce?.cancel();
    // Immediately invalidate in-flight results and pagination of the old query.
    ++_generation;
    setState(() {
      _searchPending = true;
      _loading = true;
      _items = [];
      _total = 0;
      _error = null;
    });
    _debounce = Timer(const Duration(milliseconds: 320), () {
      if (mounted) _load(reset: true);
    });
  }

  Future<void> _load({bool reset = false}) async {
    if (reset) {
      ++_generation;
      _pages.reset();
      _searchPending = false;
      _items = [];
      _total = 0;
      _cursor = null;
      _hasMore = true;
      _error = null;
      _loading = false;
    }
    if (_searchPending || _loading || !_hasMore) return;
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
        _items = [..._items, ..._pages.unique(response.items)];
        _total = response.total;
        _cursor = response.nextCursor;
        _hasMore = _pages.canContinue(response.hasMore,
            response.items.isNotEmpty, response.nextCursor);
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
    if (!_savingIds.add(lead.id)) return;
    setState(() {});
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
    } finally {
      _savingIds.remove(lead.id);
      if (mounted) setState(() {});
    }
  }

  Future<void> _performContactAction(
    Future<void> Function() perform,
  ) async {
    try {
      await perform();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text(
          'تعذر تنفيذ الإجراء؛ يلزم إكمال ربط Flutter بخدمات Android.',
        )),
      );
    }
  }

  Future<void> _addManualLead() async {
    if (_manualAddOpen) return;
    setState(() => _manualAddOpen = true);
    try {
    String name = '';
    String phone = '';
    final newLead = await showDialog<({String name, String phone})>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (dialogContext, updateDialog) => AlertDialog(
          title: const Text('إضافة عميل يدويًا'),
          content: SingleChildScrollView(child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                key: const Key('manual_lead_name'),
                decoration: const InputDecoration(
                  labelText: 'اسم العميل (اختياري)',
                ),
                textInputAction: TextInputAction.next,
                maxLength: 120,
                onChanged: (value) => name = value,
              ),
              const SizedBox(height: 8),
              TextField(
                key: const Key('manual_lead_phone'),
                keyboardType: TextInputType.phone,
                textDirection: TextDirection.ltr,
                maxLength: 24,
                decoration: const InputDecoration(
                  labelText: 'رقم الهاتف',
                  hintText: '+967...',
                ),
                onChanged: (value) =>
                    updateDialog(() => phone = value),
              ),
              const SizedBox(height: 8),
              const Text(
                'الإضافة هنا إلى قائمة العملاء أولاً، ولا تحفظ رقمًا في جهات الاتصال تلقائيًا.',
                style: TextStyle(color: WaPalette.muted, fontSize: 12),
              ),
            ],
          )),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext),
              child: const Text('إلغاء'),
            ),
            FilledButton(
              key: const Key('submit_manual_lead'),
              onPressed: !RegExp(r'^\+?[0-9\s().-]+
                  ? null
                  : () => Navigator.pop(dialogContext, (
                      name: name.trim(),
                      phone: phone.trim(),
                    )),
              child: const Text('إضافة إلى العملاء'),
            ),
          ],
        ),
      ),
    );
    if (!mounted || newLead == null) return;
    try {
      await widget.gateway.createLead(
        phone: newLead.phone,
        name: newLead.name,
      );
      if (!mounted) return;
      await _load(reset: true);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تمت إضافة العميل إلى القائمة')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تعذرت إضافة العميل؛ تحقق من الرقم والترخيص')),
      );
    }
    } finally {
      if (mounted) setState(() => _manualAddOpen = false);
    }
  }

  static const List<(String, String)> _statuses = [
    ('NEW', 'جديد'),
    ('CONTACTED', 'تم التواصل'),
    ('INTERESTED', 'مهتم'),
    ('FOLLOW_UP', 'متابعة'),
    ('SOLD', 'تم البيع'),
    ('NOT_INTERESTED', 'غير مهتم'),
  ];

  String _statusLabel(String status) {
    for (final item in _statuses) {
      if (item.$1 == status) return item.$2;
    }
    return status;
  }

  Future<void> _updateStatus(LeadEntry lead, String newStatus) async {
    if (lead.status == newStatus) return;
    try {
      await widget.gateway.updateLeadStatus(lead.id, newStatus);
      if (!mounted) return;
      await _load(reset: true);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('تم تحديث حالة العميل إلى ${_statusLabel(newStatus)}')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تعذر تحديث حالة العميل، لم يُحفظ التغيير')),
      );
    }
  }

  Future<void> _editNotes(LeadEntry lead) async {
    final notes = await showDialog<String>(
      context: context,
      builder: (_) => _LeadNotesDialog(initialNotes: lead.notes),
    );
    if (!mounted || notes == null || notes.trim() == lead.notes.trim()) return;
    try {
      await widget.gateway.updateLeadNotes(lead.id, notes.trim());
      if (!mounted) return;
      await _load(reset: true);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تم حفظ ملاحظات العميل')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تعذر حفظ الملاحظات؛ لم تُحدّث البيانات')),
      );
    }
  }

  Future<void> _renameLead(LeadEntry lead) async {
    final name = await showDialog<String>(
      context: context,
      builder: (_) => _LeadNameDialog(initialName: lead.name),
    );
    if (!mounted || name == null) return;
    final cleanName = name.trim();
    if (cleanName.isEmpty || cleanName == lead.name.trim()) return;
    try {
      await widget.gateway.updateLeadDisplayName(lead.id, cleanName);
      if (!mounted) return;
      await _load(reset: true);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تم تحديث اسم العميل داخل التطبيق')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تعذر تعديل اسم العميل؛ لم يتغير السجل')),
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
          padding: const EdgeInsets.all(18),
          child: ConstrainedBox(
            constraints: BoxConstraints(
              maxHeight: MediaQuery.sizeOf(context).height * 0.72,
            ),
            child: SingleChildScrollView(
              child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      lead.name,
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  IconButton(
                    key: const Key('edit_customer_name_button'),
                    tooltip: 'تعديل اسم العميل داخل التطبيق',
                    icon: const Icon(Icons.drive_file_rename_outline),
                    onPressed: () {
                      Navigator.pop(context);
                      _renameLead(lead);
                    },
                  ),
                ],
              ),
              const Text(
                'الاسم هنا داخل التطبيق فقط، ولا يغير جهة اتصال الهاتف',
                style: TextStyle(fontSize: 11, color: WaPalette.muted),
              ),
              const SizedBox(height: 8),
              SelectableText(lead.phone, textDirection: TextDirection.ltr),
              const SizedBox(height: 10),
              Text('الحالة: ${_statusLabel(lead.status)}'),
              Text('المصدر: ${lead.source}'),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      lead.notes.isEmpty ? 'لا توجد ملاحظات' : lead.notes,
                      key: const Key('customer_notes_preview'),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: WaPalette.muted, fontSize: 12),
                    ),
                  ),
                  TextButton.icon(
                    key: const Key('edit_customer_notes_button'),
                    onPressed: () {
                      Navigator.pop(context);
                      _editNotes(lead);
                    },
                    icon: const Icon(Icons.edit_note_outlined, size: 19),
                    label: const Text('تعديل'),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              const Text('تغيير حالة العميل',
                style: TextStyle(fontWeight: FontWeight.w700)),
              const SizedBox(height: 6),
              Wrap(
                key: const Key('lead_status_options'),
                spacing: 7,
                runSpacing: 5,
                children: [
                  for (final option in _statuses)
                    ChoiceChip(
                      key: Key('status_${option.$1}'),
                      label: Text(option.$2),
                      selected: lead.status == option.$1,
                      onSelected: (_) {
                        Navigator.pop(context);
                        _updateStatus(lead, option.$1);
                      },
                    ),
                ],
              ),
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
        ),
      ),
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _debounce?.cancel();
    _search.dispose();
    _scroll.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Column(
    children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(12, 6, 12, 6),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            WaSectionHeader(
              title: widget.reviewOnly ? 'قائمة المراجعة ($_total)' : 'العملاء ($_total)',
              subtitle: widget.reviewOnly
                  ? 'التحقق من الرقم قبل إضافته إلى جهات الاتصال'
                  : 'ابحث وتابع العملاء من شاشة واحدة',
            ),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    key: const Key('lead_search'),
                    controller: _search,
                    onChanged: _onSearch,
                    textInputAction: TextInputAction.search,
                    decoration: InputDecoration(
                      hintText: 'البحث بالاسم أو رقم الهاتف',
                      prefixIcon: const Icon(Icons.search),
                      suffixIcon: _search.text.isEmpty ? null : IconButton(
                        tooltip: 'مسح البحث',
                        icon: const Icon(Icons.close),
                        onPressed: () { _search.clear(); _onSearch(''); },
                      ),
                    ),
                  ),
                ),
                if (!widget.reviewOnly) ...[
                  const SizedBox(width: 8),
                  IconButton.filled(
                    key: const Key('add_manual_lead'),
                    tooltip: 'إضافة عميل يدويًا',
                    onPressed: _manualAddOpen ? null : _addManualLead,
                    icon: const Icon(Icons.person_add_alt_1_outlined),
                  ),
                ],
              ],
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
                      ('NOT_INTERESTED', 'غير مهتم'),
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
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.fromLTRB(12, 6, 12, 18),
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
                      padding: const EdgeInsets.only(bottom: 7),
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
                                          fontSize: 15, fontWeight: FontWeight.bold,
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
                                        onPressed: _savingIds.contains(lead.id) ? null : () => _save(lead),
                                        child: const Text('حفظ جهة الاتصال'),
                                      )
                                    else
                                      const Icon(Icons.chevron_left,
                                        color: WaPalette.green),
                                  ],
                                ),
                                const SizedBox(height: 10),
                                Row(
                                  children: [
                                    Expanded(
                                      child: OutlinedButton.icon(
                                        onPressed: () => _performContactAction(
                                          () => widget.gateway.callLead(lead.phone),
                                        ),
                                        icon: const Icon(Icons.call_outlined, size: 17),
                                        label: const Text('اتصال'),
                                        style: OutlinedButton.styleFrom(
                                          backgroundColor: WaPalette.raised,
                                          minimumSize: const Size(0, 40),
                                          padding: const EdgeInsets.symmetric(horizontal: 3),
                                        ),
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    Expanded(
                                      child: OutlinedButton.icon(
                                        onPressed: () => _performContactAction(
                                          () => widget.gateway.openWhatsAppLead(lead.phone),
                                        ),
                                        icon: const Icon(Icons.chat_outlined, size: 17),
                                        label: const Text('واتساب'),
                                        style: OutlinedButton.styleFrom(
                                          backgroundColor: WaPalette.raised,
                                          minimumSize: const Size(0, 40),
                                          padding: const EdgeInsets.symmetric(horizontal: 3),
                                        ),
                                      ),
                                    ),
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

/// Owns its controller until the dialog route has completely disposed.
/// Disposing immediately after showDialog() resolves breaks reverse animations.
class _LeadNotesDialog extends StatefulWidget {
  const _LeadNotesDialog({required this.initialNotes});
  final String initialNotes;

  @override
  State<_LeadNotesDialog> createState() => _LeadNotesDialogState();
}

class _LeadNotesDialogState extends State<_LeadNotesDialog> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.initialNotes);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text('ملاحظات العميل'),
    content: TextField(
      key: const Key('edit_customer_notes'),
      controller: _controller,
      maxLength: 1500,
      minLines: 2,
      maxLines: 5,
      textInputAction: TextInputAction.newline,
      decoration: const InputDecoration(
        hintText: 'اكتب ملاحظة للمتابعة أو اتركها فارغة للمسح',
      ),
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('إلغاء'),
      ),
      FilledButton(
        key: const Key('save_customer_notes'),
        onPressed: () => Navigator.pop(context, _controller.text),
        child: const Text('حفظ الملاحظات'),
      ),
    ],
  );
}

/// Dialog-local controller avoids use-after-dispose during route exit
/// animations. This operation never renames an Android Contacts entry.
class _LeadNameDialog extends StatefulWidget {
  const _LeadNameDialog({required this.initialName});
  final String initialName;

  @override
  State<_LeadNameDialog> createState() => _LeadNameDialogState();
}

class _LeadNameDialogState extends State<_LeadNameDialog> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.initialName);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text('تعديل اسم العميل'),
    content: Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        TextField(
          key: const Key('edit_customer_name'),
          controller: _controller,
          maxLength: 120,
          textInputAction: TextInputAction.done,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(
            labelText: 'الاسم داخل التطبيق',
            hintText: 'اكتب اسم العميل',
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'لن يتم تغيير رقم الهاتف أو اسم جهة الاتصال المحفوظة.',
          style: TextStyle(fontSize: 12, color: WaPalette.muted),
        ),
      ],
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('إلغاء'),
      ),
      FilledButton(
        key: const Key('save_customer_name'),
        onPressed: _controller.text.trim().isEmpty
            ? null
            : () => Navigator.pop(context, _controller.text.trim()),
        child: const Text('حفظ الاسم'),
      ),
    ],
  );
}
).hasMatch(phone.trim()) ||
                  phone.replaceAll(RegExp(r'[^0-9]'), '').length < 7 ||
                  phone.replaceAll(RegExp(r'[^0-9]'), '').length > 15
                  ? null
                  : () => Navigator.pop(dialogContext, (
                      name: name.trim(),
                      phone: phone.trim(),
                    )),
              child: const Text('إضافة إلى العملاء'),
            ),
          ],
        ),
      ),
    );
    if (!mounted || newLead == null) return;
    try {
      await widget.gateway.createLead(
        phone: newLead.phone,
        name: newLead.name,
      );
      if (!mounted) return;
      await _load(reset: true);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تمت إضافة العميل إلى القائمة')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تعذرت إضافة العميل؛ تحقق من الرقم والترخيص')),
      );
    }
    } finally {
      if (mounted) setState(() => _manualAddOpen = false);
    }
  }

  static const List<(String, String)> _statuses = [
    ('NEW', 'جديد'),
    ('CONTACTED', 'تم التواصل'),
    ('INTERESTED', 'مهتم'),
    ('FOLLOW_UP', 'متابعة'),
    ('SOLD', 'تم البيع'),
    ('NOT_INTERESTED', 'غير مهتم'),
  ];

  String _statusLabel(String status) {
    for (final item in _statuses) {
      if (item.$1 == status) return item.$2;
    }
    return status;
  }

  Future<void> _updateStatus(LeadEntry lead, String newStatus) async {
    if (lead.status == newStatus) return;
    try {
      await widget.gateway.updateLeadStatus(lead.id, newStatus);
      if (!mounted) return;
      await _load(reset: true);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('تم تحديث حالة العميل إلى ${_statusLabel(newStatus)}')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تعذر تحديث حالة العميل، لم يُحفظ التغيير')),
      );
    }
  }

  Future<void> _editNotes(LeadEntry lead) async {
    final notes = await showDialog<String>(
      context: context,
      builder: (_) => _LeadNotesDialog(initialNotes: lead.notes),
    );
    if (!mounted || notes == null || notes.trim() == lead.notes.trim()) return;
    try {
      await widget.gateway.updateLeadNotes(lead.id, notes.trim());
      if (!mounted) return;
      await _load(reset: true);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تم حفظ ملاحظات العميل')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تعذر حفظ الملاحظات؛ لم تُحدّث البيانات')),
      );
    }
  }

  Future<void> _renameLead(LeadEntry lead) async {
    final name = await showDialog<String>(
      context: context,
      builder: (_) => _LeadNameDialog(initialName: lead.name),
    );
    if (!mounted || name == null) return;
    final cleanName = name.trim();
    if (cleanName.isEmpty || cleanName == lead.name.trim()) return;
    try {
      await widget.gateway.updateLeadDisplayName(lead.id, cleanName);
      if (!mounted) return;
      await _load(reset: true);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تم تحديث اسم العميل داخل التطبيق')),
      );
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('تعذر تعديل اسم العميل؛ لم يتغير السجل')),
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
          padding: const EdgeInsets.all(18),
          child: ConstrainedBox(
            constraints: BoxConstraints(
              maxHeight: MediaQuery.sizeOf(context).height * 0.72,
            ),
            child: SingleChildScrollView(
              child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      lead.name,
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  IconButton(
                    key: const Key('edit_customer_name_button'),
                    tooltip: 'تعديل اسم العميل داخل التطبيق',
                    icon: const Icon(Icons.drive_file_rename_outline),
                    onPressed: () {
                      Navigator.pop(context);
                      _renameLead(lead);
                    },
                  ),
                ],
              ),
              const Text(
                'الاسم هنا داخل التطبيق فقط، ولا يغير جهة اتصال الهاتف',
                style: TextStyle(fontSize: 11, color: WaPalette.muted),
              ),
              const SizedBox(height: 8),
              SelectableText(lead.phone, textDirection: TextDirection.ltr),
              const SizedBox(height: 10),
              Text('الحالة: ${_statusLabel(lead.status)}'),
              Text('المصدر: ${lead.source}'),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      lead.notes.isEmpty ? 'لا توجد ملاحظات' : lead.notes,
                      key: const Key('customer_notes_preview'),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: WaPalette.muted, fontSize: 12),
                    ),
                  ),
                  TextButton.icon(
                    key: const Key('edit_customer_notes_button'),
                    onPressed: () {
                      Navigator.pop(context);
                      _editNotes(lead);
                    },
                    icon: const Icon(Icons.edit_note_outlined, size: 19),
                    label: const Text('تعديل'),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              const Text('تغيير حالة العميل',
                style: TextStyle(fontWeight: FontWeight.w700)),
              const SizedBox(height: 6),
              Wrap(
                key: const Key('lead_status_options'),
                spacing: 7,
                runSpacing: 5,
                children: [
                  for (final option in _statuses)
                    ChoiceChip(
                      key: Key('status_${option.$1}'),
                      label: Text(option.$2),
                      selected: lead.status == option.$1,
                      onSelected: (_) {
                        Navigator.pop(context);
                        _updateStatus(lead, option.$1);
                      },
                    ),
                ],
              ),
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
        ),
      ),
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _debounce?.cancel();
    _search.dispose();
    _scroll.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Column(
    children: [
      Padding(
        padding: const EdgeInsets.fromLTRB(12, 6, 12, 6),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            WaSectionHeader(
              title: widget.reviewOnly ? 'قائمة المراجعة ($_total)' : 'العملاء ($_total)',
              subtitle: widget.reviewOnly
                  ? 'التحقق من الرقم قبل إضافته إلى جهات الاتصال'
                  : 'ابحث وتابع العملاء من شاشة واحدة',
            ),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    key: const Key('lead_search'),
                    controller: _search,
                    onChanged: _onSearch,
                    textInputAction: TextInputAction.search,
                    decoration: const InputDecoration(
                      hintText: 'البحث بالاسم أو رقم الهاتف',
                      prefixIcon: Icon(Icons.search),
                    ),
                  ),
                ),
                if (!widget.reviewOnly) ...[
                  const SizedBox(width: 8),
                  IconButton.filled(
                    key: const Key('add_manual_lead'),
                    tooltip: 'إضافة عميل يدويًا',
                    onPressed: _addManualLead,
                    icon: const Icon(Icons.person_add_alt_1_outlined),
                  ),
                ],
              ],
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
                      ('NOT_INTERESTED', 'غير مهتم'),
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
                  padding: const EdgeInsets.fromLTRB(12, 6, 12, 18),
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
                      padding: const EdgeInsets.only(bottom: 7),
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
                                        lead.name.isEmpty ? '؟' : lead.name.substring(0, 1),
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
                                          fontSize: 15, fontWeight: FontWeight.bold,
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
                                const SizedBox(height: 10),
                                Row(
                                  children: [
                                    Expanded(
                                      child: OutlinedButton.icon(
                                        onPressed: () => _performContactAction(
                                          () => widget.gateway.callLead(lead.phone),
                                        ),
                                        icon: const Icon(Icons.call_outlined, size: 17),
                                        label: const Text('اتصال'),
                                        style: OutlinedButton.styleFrom(
                                          backgroundColor: WaPalette.raised,
                                          minimumSize: const Size(0, 40),
                                          padding: const EdgeInsets.symmetric(horizontal: 3),
                                        ),
                                      ),
                                    ),
                                    const SizedBox(width: 8),
                                    Expanded(
                                      child: OutlinedButton.icon(
                                        onPressed: () => _performContactAction(
                                          () => widget.gateway.openWhatsAppLead(lead.phone),
                                        ),
                                        icon: const Icon(Icons.chat_outlined, size: 17),
                                        label: const Text('واتساب'),
                                        style: OutlinedButton.styleFrom(
                                          backgroundColor: WaPalette.raised,
                                          minimumSize: const Size(0, 40),
                                          padding: const EdgeInsets.symmetric(horizontal: 3),
                                        ),
                                      ),
                                    ),
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

/// Owns its controller until the dialog route has completely disposed.
/// Disposing immediately after showDialog() resolves breaks reverse animations.
class _LeadNotesDialog extends StatefulWidget {
  const _LeadNotesDialog({required this.initialNotes});
  final String initialNotes;

  @override
  State<_LeadNotesDialog> createState() => _LeadNotesDialogState();
}

class _LeadNotesDialogState extends State<_LeadNotesDialog> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.initialNotes);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text('ملاحظات العميل'),
    content: TextField(
      key: const Key('edit_customer_notes'),
      controller: _controller,
      maxLength: 1500,
      minLines: 2,
      maxLines: 5,
      textInputAction: TextInputAction.newline,
      decoration: const InputDecoration(
        hintText: 'اكتب ملاحظة للمتابعة أو اتركها فارغة للمسح',
      ),
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('إلغاء'),
      ),
      FilledButton(
        key: const Key('save_customer_notes'),
        onPressed: () => Navigator.pop(context, _controller.text),
        child: const Text('حفظ الملاحظات'),
      ),
    ],
  );
}

/// Dialog-local controller avoids use-after-dispose during route exit
/// animations. This operation never renames an Android Contacts entry.
class _LeadNameDialog extends StatefulWidget {
  const _LeadNameDialog({required this.initialName});
  final String initialName;

  @override
  State<_LeadNameDialog> createState() => _LeadNameDialogState();
}

class _LeadNameDialogState extends State<_LeadNameDialog> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.initialName);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text('تعديل اسم العميل'),
    content: Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        TextField(
          key: const Key('edit_customer_name'),
          controller: _controller,
          maxLength: 120,
          textInputAction: TextInputAction.done,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(
            labelText: 'الاسم داخل التطبيق',
            hintText: 'اكتب اسم العميل',
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'لن يتم تغيير رقم الهاتف أو اسم جهة الاتصال المحفوظة.',
          style: TextStyle(fontSize: 12, color: WaPalette.muted),
        ),
      ],
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('إلغاء'),
      ),
      FilledButton(
        key: const Key('save_customer_name'),
        onPressed: _controller.text.trim().isEmpty
            ? null
            : () => Navigator.pop(context, _controller.text.trim()),
        child: const Text('حفظ الاسم'),
      ),
    ],
  );
}
