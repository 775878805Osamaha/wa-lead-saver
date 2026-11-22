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
  int returnToNativeCalls = 0;
  String countryCode = '+967';
  final List<String> changedCountryCodes = [];
  final List<String> openedPermissionScreens = [];
  final List<String> manuallyCreatedPhones = [];
  final List<String> openedNativeTools = [];
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

  @override
  Future<void> openNativeTool(String tool) async {
    openedNativeTools.add(tool);
  }

  @override
  Future<AccountInfo> loadAccountInfo() async => const AccountInfo(
        email: 'test@example.com',
        accountNumber: 'OMX-TEST',
        role: 'مستخدم',
        licenseStatus: 'نشط',
        licenseMessage: 'الحساب نشط ومفعل',
        canSaveContacts: true,
      );

  @override
  Future<void> returnToNative() async { returnToNativeCalls++; }

  @override
  Future<String> loadCountryCode() async => countryCode;

  @override
  Future<void> changeCountryCode(String code) async {
    changedCountryCodes.add(code);
    countryCode = code;
  }

  @override
  Future<void> openPermissionSettings(String permission) async {
    openedPermissionScreens.add(permission);
  }
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
    expect(find.text('إذن قراءة الإشعارات'), findsOneWidget);
    expect(find.byKey(const Key('account_license_card')), findsOneWidget);
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
    // ListView lazily builds permission cards, so only inspect visible
    // unknown statuses; never assume every card is mounted at once.
    expect(find.byIcon(Icons.help_outline), findsWidgets);
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

  testWidgets('manual add requires phone and calls native gateway', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('العملاء').last);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('add_manual_lead')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('submit_manual_lead')), findsOneWidget);
    final submit = tester.widget<FilledButton>(
      find.byKey(const Key('submit_manual_lead')),
    );
    expect(submit.onPressed, isNull);
    await tester.enterText(
      find.byKey(const Key('manual_lead_name')), 'عميل جديد',
    );
    await tester.enterText(
      find.byKey(const Key('manual_lead_phone')), '+967777123456',
    );
    await tester.pumpAndSettle();
    tester.testTextInput.hide();
    await tester.tap(find.byKey(const Key('submit_manual_lead')));
    await tester.pumpAndSettle();
    expect(gateway.manuallyCreatedPhones, contains('+967777123456'));
  });

  testWidgets('audit deletion requires a second explicit confirmation', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('السجل').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('مسح السجل'));
    await tester.pumpAndSettle();
    expect(gateway.auditClearCalls, 0);
    await tester.tap(find.text('إلغاء'));
    await tester.pumpAndSettle();
    expect(gateway.auditClearCalls, 0);
    await tester.tap(find.text('مسح السجل'));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('confirm_clear_history')));
    await tester.pumpAndSettle();
    expect(gateway.auditClearCalls, 1);
  });

  testWidgets('history CSV calls the real export gateway', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('السجل').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('تصدير CSV'));
    await tester.pumpAndSettle();
    expect(gateway.auditExportCalls, 1);
  });

  testWidgets('camera and scan actions dispatch to native Kotlin tools', (tester) async {
    // This tests action dispatch, not scrolling: give the quick-action
    // row adequate vertical room so taps cannot hit the bottom navbar.
    tester.view.physicalSize = const Size(420, 1000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('التقاط من الكاميرا'));
    await tester.tap(find.text('التقاط من الكاميرا'));
    await tester.pumpAndSettle();
    expect(gateway.openedNativeTools, contains('camera'));

    await tester.ensureVisible(find.text('فحص ومزامنة'));
    await tester.tap(find.text('فحص ومزامنة'));
    await tester.pumpAndSettle();
    expect(gateway.openedNativeTools, contains('scan'));
  });

  testWidgets('account card exposes license but no credentials, and returns to native', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('الإعدادات').last);
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('account_license_card')), findsOneWidget);
    expect(find.text('test@example.com'), findsOneWidget);
    expect(find.text('نشط'), findsOneWidget);
    expect(find.text('OMX-TEST'), findsOneWidget);
    await tester.ensureVisible(find.byKey(const Key('return_to_native')));
    await tester.tap(find.byKey(const Key('return_to_native')));
    await tester.pumpAndSettle();
    expect(gateway.returnToNativeCalls, 1);
  });

  testWidgets('preview account status is explicitly synthetic', (tester) async {
    await tester.pumpWidget(WaLeadSaverApp(
      gateway: PreviewLeadGateway(), isPreview: true,
    ));
    await tester.pumpAndSettle();
    await tester.tap(find.text('الإعدادات').last);
    await tester.pumpAndSettle();
    expect(find.text('غير متاح في المعاينة'), findsOneWidget);
    expect(find.byKey(const Key('return_to_native')), findsNothing);
  });

  testWidgets('country prefix validates input before persisting in Android', (tester) async {
    tester.view.physicalSize = const Size(420, 1000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('الإعدادات').last);
    await tester.pumpAndSettle();
    expect(
      tester.widget<TextField>(find.byKey(const Key('country_code_input')))
          .controller!.text,
      '+967',
    );
    await tester.enterText(find.byKey(const Key('country_code_input')), '123');
    await tester.ensureVisible(find.byKey(const Key('save_country_code')));
    await tester.tap(find.byKey(const Key('save_country_code')));
    await tester.pumpAndSettle();
    expect(gateway.changedCountryCodes, isEmpty);
    expect(find.textContaining('الرمز غير صالح'), findsOneWidget);
    await tester.enterText(find.byKey(const Key('country_code_input')), '+966');
    await tester.tap(find.byKey(const Key('save_country_code')));
    await tester.pumpAndSettle();
    expect(gateway.changedCountryCodes, ['+966']);
  });

  testWidgets('permission row opens the Android permission control', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('الإعدادات').last);
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('إذن قراءة الإشعارات'));
    await tester.tap(find.text('إذن قراءة الإشعارات'));
    await tester.pumpAndSettle();
    expect(gateway.openedPermissionScreens, ['notificationPermission']);
  });

}
