import 'package:flutter/material.dart';
import '../data/gateway.dart';
import '../theme/wa_theme.dart';
import '../widgets/shared.dart';

class DashboardPage extends StatefulWidget {
  const DashboardPage({
    super.key,
    required this.gateway,
    required this.onOpenReview,
    required this.onOpenLeads,
    required this.onOpenHistory,
  });
  final LeadGateway gateway;
  final VoidCallback onOpenReview;
  final VoidCallback onOpenLeads;
  final VoidCallback onOpenHistory;

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  late Future<DashboardSummary> _overview;

  @override
  void initState() {
    super.initState();
    _overview = widget.gateway.loadOverview();
  }

  Future<void> _refresh() async {
    final refreshed = widget.gateway.loadOverview();
    setState(() => _overview = refreshed);
    try {
      await refreshed;
    } catch (_) {
      // FutureBuilder renders the real error state.
    }
  }

  void _nativeActionPending(String action) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(
        'أداة $action تحتاج إلى اكتمال الربط مع خدمات Android الأصلية.',
      )),
    );
  }

  @override
  Widget build(BuildContext context) => RefreshIndicator(
        onRefresh: _refresh,
        child: FutureBuilder<DashboardSummary>(
          future: _overview,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const WaFeedback(
                message: 'جاري تحميل لوحة التحكم',
                loading: true,
              );
            }
            if (snapshot.hasError) {
              return WaFeedback(
                message: 'تعذر الاتصال ببيانات التطبيق. يلزم إكمال ربط Flutter بالخدمة الأصلية.',
                onRetry: _refresh,
              );
            }
            final data = snapshot.requireData;
            return ListView(
              key: const Key('dashboard_list'),
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 24),
              physics: const AlwaysScrollableScrollPhysics(),
              children: [
                const WaSectionHeader(
                  title: 'الرئيسية',
                  subtitle: 'نظرة عامة على النشاط',
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(child: _Metric(
                      icon: Icons.people_outline,
                      count: '${data.total}',
                      label: 'إجمالي العملاء',
                      onTap: widget.onOpenLeads,
                    )),
                    const SizedBox(width: 8),
                    Expanded(child: _Metric(
                      icon: Icons.contacts_outlined,
                      count: '${data.saved}',
                      label: 'العملاء المحفوظون',
                      onTap: widget.onOpenLeads,
                    )),
                    const SizedBox(width: 8),
                    Expanded(child: _Metric(
                      icon: Icons.history_toggle_off_outlined,
                      count: '${data.review}',
                      label: 'قائمة المراجعة',
                      iconColor: WaPalette.amber,
                      onTap: widget.onOpenReview,
                    )),
                  ],
                ),
                const SizedBox(height: 10),
                _AccuracyCard(
                  data: data,
                  onRefresh: _refresh,
                ),
                const SizedBox(height: 12),
                const WaSectionHeader(title: 'أدوات سريعة'),
                Row(
                  children: [
                    Expanded(child: _Shortcut(
                      icon: Icons.file_download_outlined,
                      text: 'تصدير CSV',
                      onTap: () => _nativeActionPending('تصدير CSV'),
                    )),
                    const SizedBox(width: 7),
                    Expanded(child: _Shortcut(
                      icon: Icons.person_add_alt_1_outlined,
                      text: 'إضافة يدوية',
                      onTap: widget.onOpenLeads,
                    )),
                    const SizedBox(width: 7),
                    Expanded(child: _Shortcut(
                      icon: Icons.photo_camera_outlined,
                      text: 'التقاط من الكاميرا',
                      color: const Color(0xFF3879CF),
                      onTap: () => _nativeActionPending('الكاميرا'),
                    )),
                    const SizedBox(width: 7),
                    Expanded(child: _Shortcut(
                      icon: Icons.document_scanner_outlined,
                      text: 'فحص ومزامنة',
                      color: const Color(0xFF804CCB),
                      onTap: widget.onOpenHistory,
                    )),
                  ],
                ),
                const SizedBox(height: 14),
                Card(
                  child: ListTile(
                    leading: Icon(
                      data.monitoringActive
                        ? Icons.verified_user_outlined : Icons.warning_amber_outlined,
                      color: data.monitoringActive
                        ? WaPalette.green : WaPalette.amber,
                    ),
                    title: Text(data.monitoringActive
                        ? 'مراقبة واتساب مفعّلة'
                        : 'مراقبة واتساب غير مفعّلة'),
                    subtitle: const Text('حالة خدمة الإشعارات الأصلية على Android'),
                  ),
                ),
              ],
            );
          },
        ),
      );
}

class _Metric extends StatelessWidget {
  const _Metric({
    required this.icon,
    required this.count,
    required this.label,
    required this.onTap,
    this.iconColor = WaPalette.green,
  });

  final IconData icon;
  final String count;
  final String label;
  final VoidCallback onTap;
  final Color iconColor;

  @override
  Widget build(BuildContext context) => Card(
    child: InkWell(
      borderRadius: BorderRadius.circular(20),
      onTap: onTap,
      child: SizedBox(
        height: 120,
        child: Padding(
          padding: const EdgeInsets.all(6),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, color: iconColor, size: 22),
              const SizedBox(height: 4),
              Flexible(
                fit: FlexFit.loose,
                child: FittedBox(
                  fit: BoxFit.scaleDown,
                  child: Text(count, style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.w900,
                    color: WaPalette.ink,
                  )),
                ),
              ),
              const SizedBox(height: 4),
              Flexible(
                fit: FlexFit.loose,
                child: Text(label,
                  textAlign: TextAlign.center,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: WaPalette.muted,
                    fontWeight: FontWeight.w700,
                    fontSize: 10.5,
                  )),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}

class _AccuracyCard extends StatelessWidget {
  const _AccuracyCard({required this.data, required this.onRefresh});
  final DashboardSummary data;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    final percent = data.accuracyPercentage;
    final valid = percent != null && data.evaluatedCases > 0;
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(14, 16, 14, 14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                const Icon(Icons.check_circle_outline, color: WaPalette.green),
                const SizedBox(width: 8),
                const Expanded(
                  child: Text('دقة الالتقاط', style: TextStyle(
                    fontWeight: FontWeight.w900, fontSize: 17,
                  )),
                ),
                DecoratedBox(
                  decoration: BoxDecoration(
                    color: WaPalette.raised,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    child: Text(valid ? '$percent%' : '—',
                      style: const TextStyle(
                        fontWeight: FontWeight.w900,
                        color: WaPalette.green,
                      )),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            Text(valid
                ? 'تم فحص ${data.evaluatedCases} حالة · نجح ${data.passedCases}'
                : 'لم تُقيّم الدقة بعد ببيانات موثوقة',
              textAlign: TextAlign.right,
              style: const TextStyle(color: WaPalette.muted, fontSize: 12),
            ),
            const SizedBox(height: 10),
            LinearProgressIndicator(
              value: valid ? percent / 100 : 0,
              minHeight: 8,
              borderRadius: BorderRadius.circular(12),
              backgroundColor: WaPalette.raised,
              color: WaPalette.accent,
            ),
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: onRefresh,
              icon: const Icon(Icons.refresh),
              label: const Text('إعادة الفحص'),
              style: OutlinedButton.styleFrom(
                backgroundColor: WaPalette.raised,
                minimumSize: const Size(0, 44),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Shortcut extends StatelessWidget {
  const _Shortcut({
    required this.icon,
    required this.text,
    required this.onTap,
    this.color = WaPalette.green,
  });
  final IconData icon;
  final String text;
  final VoidCallback onTap;
  final Color color;

  @override
  Widget build(BuildContext context) => Card(
    child: InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: SizedBox(
        height: 80,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 3),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, color: color, size: 25),
              const SizedBox(height: 8),
              Text(text,
                maxLines: 2,
                textAlign: TextAlign.center,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontSize: 10.5, fontWeight: FontWeight.w700,
                )),
            ],
          ),
        ),
      ),
    ),
  );
}
