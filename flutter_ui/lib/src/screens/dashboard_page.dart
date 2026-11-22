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
    final updated = widget.gateway.loadOverview();
    setState(() => _overview = updated);
    try {
      await updated;
    } catch (_) {
      // The error state is rendered by the FutureBuilder below.
    }
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
                message: 'تعذر الاتصال ببيانات التطبيق. يلزم ربط واجهة Flutter بالخدمة الأصلية.',
                onRetry: _refresh,
              );
            }
            final summary = snapshot.requireData;
            return ListView(
              key: const Key('dashboard_list'),
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
              physics: const AlwaysScrollableScrollPhysics(),
              children: [
                const WaSectionHeader(
                  title: 'لوحة التحكم',
                  subtitle: 'العملاء والعمليات في مكان واحد',
                ),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Row(
                      children: [
                        const Icon(Icons.shield_outlined, color: WaPalette.green),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            summary.monitoringActive
                                ? 'مراقبة إشعارات واتساب مفعّلة'
                                : 'مراقبة الإشعارات غير مفعّلة',
                            style: const TextStyle(fontWeight: FontWeight.w600),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 14),
                LayoutBuilder(
                  builder: (context, constraints) => GridView.count(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    crossAxisCount: constraints.maxWidth < 480 ? 2 : 3,
                    mainAxisSpacing: 12,
                    crossAxisSpacing: 12,
                    childAspectRatio: 1.18,
                    children: [
                      WaCounterCard(
                        label: 'العملاء',
                        value: '${summary.total}',
                        icon: Icons.people_alt_outlined,
                        onTap: widget.onOpenLeads,
                      ),
                      WaCounterCard(
                        label: 'المحفوظون',
                        value: '${summary.saved}',
                        icon: Icons.contacts_outlined,
                        onTap: widget.onOpenLeads,
                      ),
                      WaCounterCard(
                        label: 'قائمة المراجعة',
                        value: '${summary.review}',
                        icon: Icons.fact_check_outlined,
                        onTap: widget.onOpenReview,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 18),
                const WaSectionHeader(title: 'أدوات سريعة'),
                Card(
                  child: Column(
                    children: [
                      ListTile(
                        leading: const Icon(Icons.fact_check_outlined,
                          color: WaPalette.amber),
                        title: const Text('مراجعة الأرقام الملتقطة'),
                        subtitle: const Text('احفظ العملاء بعد التحقق من أرقامهم'),
                        trailing: const Icon(Icons.chevron_left),
                        onTap: widget.onOpenReview,
                      ),
                      const Divider(height: 1),
                      ListTile(
                        leading: const Icon(Icons.history,
                          color: WaPalette.green),
                        title: const Text('سجل العمليات'),
                        subtitle: const Text('عرض السجل على دفعات لتقليل استهلاك الذاكرة'),
                        trailing: const Icon(Icons.chevron_left),
                        onTap: widget.onOpenHistory,
                      ),
                    ],
                  ),
                ),
              ],
            );
          },
        ),
      );
}
