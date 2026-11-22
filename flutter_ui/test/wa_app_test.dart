import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:wa_lead_saver_ui/main.dart';
import 'package:wa_lead_saver_ui/preview_main.dart' show PreviewLeadGateway;
import 'package:wa_lead_saver_ui/src/data/gateway.dart';
import 'package:wa_lead_saver_ui/src/screens/app_shell.dart';

class FixtureGateway implements LeadGateway {
  int leadCalls = 0;
  int auditClearCalls = 0;
  int auditExportCalls = 0;
  final List<String> manuallyCreatedPhones = [];
  final List<int> savedIds = [];

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
  Future<void> saveLead(int id) async { savedIds.add(id); }

  @override
  Future<void> callLead(String number) async {}

  @override
  Future<void> openWhatsAppLead(String number) async {}

  @override
  Future<int> createLead({required String phone, String name = ''}) async {
    manuallyCreatedPhones.add(phone);
    return manuallyCreatedPhones.length + 10;
  }

  @override
  Future<void> exportHistory() async { auditExportCalls++; }

  @override
  Future<void> clearHistory() async { auditClearCalls++; }
}

class OfflineGateway extends FixtureGateway {
  @override
  Future<DashboardSummary> loadOverview() async {
    throw StateError('Native bridge not connected');
  }
}

void main() {
  testWidgets('renders real summary values with Arabic RTL', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    expect(Directionality.of(tester.element(find.byType(WaAppShell))),
        TextDirection.rtl);
    final appTheme = Theme.of(tester.element(find.byType(WaAppShell)));
    expect(appTheme.brightness, Brightness.light);
    expect(appTheme.textTheme.bodyMedium?.fontFamily, 'ThmanyahSans');
    expect(find.text('الرئيسية'), findsWidgets);
    expect(find.text('6'), findsOneWidget);
    expect(find.text('4'), findsOneWidget);
    expect(find.text('2'), findsOneWidget);
    expect(find.text('دقة الالتقاط'), findsOneWidget);
    expect(find.text('لم تُقيّم الدقة بعد ببيانات موثوقة'), findsOneWidget);
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
    expect(find.text('تصدير CSV'), findsOneWidget);
    expect(find.text('مسح السجل'), findsOneWidget);
    await tester.tap(find.text('الإعدادات'));
    await tester.pumpAndSettle();
    expect(find.text('مراقبة واتساب للأعمال'), findsOneWidget);
    expect(find.text('إذن قراءة الإشعارات'), findsOneWidget);
  });
  testWidgets('small Android viewport keeps dashboard readable', (tester) async {
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(WaLeadSaverApp(gateway: FixtureGateway()));
    await tester.pumpAndSettle();
    expect(find.text('إجمالي العملاء'), findsOneWidget);
    expect(find.text('دقة الالتقاط'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('shows an error instead of invented counts when native bridge is absent', (tester) async {
    await tester.pumpWidget(WaLeadSaverApp(gateway: OfflineGateway()));
    await tester.pumpAndSettle();
    expect(find.textContaining('تعذر الاتصال ببيانات التطبيق'), findsOneWidget);
    expect(find.text('1,260'), findsNothing);
  });

  testWidgets('review save action uses real gateway contract', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('قائمة المراجعة').first);
    await tester.pumpAndSettle();
    expect(find.text('عميل تجريبي'), findsOneWidget);
    await tester.tap(find.text('حفظ جهة الاتصال').first);
    await tester.pumpAndSettle();
    expect(gateway.savedIds, contains(1));
  });

  testWidgets('demo labels synthetic data and does not claim Android permissions', (tester) async {
    await tester.pumpWidget(WaLeadSaverApp(
      gateway: PreviewLeadGateway(),
      isPreview: true,
    ));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('preview_notice')), findsOneWidget);
    expect(find.textContaining('بيانات تجريبية'), findsOneWidget);
    expect(find.text('1260'), findsNothing);
    expect(find.text('4'), findsWidgets);

    await tester.tap(find.text('الإعدادات').last);
    await tester.pumpAndSettle();
    expect(find.byIcon(Icons.help_outline), findsNWidgets(3));
    expect(find.text('صلاحية جهات الاتصال'), findsOneWidget);
  });

  testWidgets('audit status uses Arabic labels', (tester) async {
    await tester.pumpWidget(WaLeadSaverApp(gateway: FixtureGateway()));
    await tester.pumpAndSettle();
    await tester.tap(find.text('السجل').last);
    await tester.pumpAndSettle();
    expect(find.text('Saved'), findsNothing);
    expect(find.text('تم الحفظ'), findsWidgets);
  });

  testWidgets('compact 360dp settings and history do not overflow', (tester) async {
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(WaLeadSaverApp(gateway: FixtureGateway()));
    await tester.pumpAndSettle();
    await tester.tap(find.text('الإعدادات').last);
    await tester.pumpAndSettle();
    expect(find.text('إذن قراءة الإشعارات'), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.tap(find.text('السجل').last);
    await tester.pumpAndSettle();
    expect(find.text('سجل العمليات (1)'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

}
