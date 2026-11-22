import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:wa_lead_saver_ui/main.dart';
import 'package:wa_lead_saver_ui/src/data/gateway.dart';
import 'package:wa_lead_saver_ui/src/screens/app_shell.dart';

class FixtureGateway implements LeadGateway {
  int leadCalls = 0;

  @override
  Future<DashboardSummary> loadOverview() async =>
      const DashboardSummary(
        saved: 4, review: 2, total: 6, monitoringActive: true,
      );

  @override
  Future<LeadPage> loadLeads({
    String query = '',
    String status = '',
    String? cursor,
    int limit = 40,
    bool reviewOnly = false,
  }) async {
    leadCalls++;
    if (query.isNotEmpty && !'عميل تجريبي'.contains(query)) {
      return const LeadPage(items: [], total: 0, hasMore: false);
    }
    return const LeadPage(
      items: [
        LeadEntry(
          id: 1, name: 'عميل تجريبي', phone: '+967000000000',
          source: 'WHATSAPP', status: 'NEW',
        )
      ],
      total: 1,
      hasMore: false,
    );
  }

  @override
  Future<HistoryPage> loadHistory({
    String filter = 'ALL',
    String? cursor,
    int limit = 60,
  }) async => const HistoryPage(
        items: [
          HistoryEntry(
            id: 1, status: 'Saved', details: 'تم الحفظ',
            source: 'WhatsApp', timestamp: 1750000000000,
          )
        ],
        total: 1,
        hasMore: false,
      );

  @override
  Future<Map<String, bool>> loadSettings() async => {
    'autoSaveLeads': false,
    'monitorWhatsApp': true,
    'monitorWhatsAppBusiness': true,
    'ignoreGroupMessages': true,
  };

  @override
  Future<void> changeSetting(String key, bool enabled) async {}

  @override
  Future<void> saveLead(int id) async {}
}

void main() {
  testWidgets('renders real summary values with Arabic RTL', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    expect(Directionality.of(tester.element(find.byType(WaAppShell))),
        TextDirection.rtl);
    expect(Theme.of(tester.element(find.byType(WaAppShell))).brightness, Brightness.light);
    expect(find.text('لوحة التحكم'), findsOneWidget);
    expect(find.text('6'), findsOneWidget);
    expect(find.text('4'), findsOneWidget);
    expect(find.text('2'), findsOneWidget);
  });

  testWidgets('loads only selected tab and displays customer list', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    expect(gateway.leadCalls, 0);
    await tester.tap(find.text('العملاء').last);
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('leads_list')), findsOneWidget);
    expect(find.text('عميل تجريبي'), findsOneWidget);
    expect(gateway.leadCalls, 1);
  });

  testWidgets('opens audit history and settings from bottom bar', (tester) async {
    await tester.pumpWidget(WaLeadSaverApp(gateway: FixtureGateway()));
    await tester.pumpAndSettle();
    await tester.tap(find.text('السجل'));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('history_list')), findsOneWidget);
    expect(find.text('سجل العمليات (1)'), findsOneWidget);
    await tester.tap(find.text('الإعدادات'));
    await tester.pumpAndSettle();
    expect(find.text('مراقبة واتساب للأعمال'), findsOneWidget);
  });
}
