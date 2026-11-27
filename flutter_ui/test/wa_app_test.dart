import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:wa_lead_saver_ui/main.dart';
import 'package:wa_lead_saver_ui/preview_main.dart' show PreviewLeadGateway;
import 'package:wa_lead_saver_ui/src/data/gateway.dart';
import 'package:wa_lead_saver_ui/src/screens/app_shell.dart';

class FixtureGateway implements LeadGateway {
  int leadCalls = 0;
  int dashboardCalls = 0;
  int historyCalls = 0;
  int auditClearCalls = 0;
  int auditExportCalls = 0;
  int returnToNativeCalls = 0;
  int nativeLoginCalls = 0;
  AccessState accessState = const AccessState(
    allowed: false,
    message: 'سجّل الدخول لعرض عملائك',
  );
  String countryCode = '+967';
  final List<String> changedCountryCodes = [];
  final List<String> openedPermissionScreens = [];
  final List<String> manuallyCreatedPhones = [];
  final List<String> openedNativeTools = [];
  final List<int> savedIds = [];
  final List<(int, String)> updatedLeadStatuses = [];
  final List<(int, String)> updatedLeadNotes = [];
  final List<(int, String)> updatedLeadNames = [];

  @override
  Future<DashboardSummary> loadOverview() async {
    dashboardCalls++;
    return const DashboardSummary(
      saved: 4, review: 2, total: 6, monitoringActive: true,
    );
  }

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
  }) async {
    historyCalls++;
    return const HistoryPage(
        items: [
          HistoryEntry(
            id: 1, status: 'Saved', details: 'تم الحفظ',
            source: 'WhatsApp', timestamp: 1750000000000,
          )
        ],
        total: 1,
        hasMore: false,
      );
  }

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
  Future<void> updateLeadStatus(int id, String status) async {
    updatedLeadStatuses.add((id, status));
  }

  @override
  Future<void> updateLeadNotes(int id, String notes) async {
    updatedLeadNotes.add((id, notes));
  }

  @override
  Future<void> updateLeadDisplayName(int id, String name) async {
    updatedLeadNames.add((id, name));
  }

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

  @override
  Future<AccessState> checkAccess() async => accessState;

  @override
  Future<void> openNativeLogin() async { nativeLoginCalls++; }
}

class OfflineGateway extends FixtureGateway {
  @override
  Future<DashboardSummary> loadOverview() async {
    throw StateError('Native bridge not connected');
  }
}

class SessionLostGateway extends FixtureGateway {
  @override
  Future<Map<String, bool>> loadSettings() async =>
      throw StateError('Account session has expired');
}

class DelayedAccessGateway extends FixtureGateway {
  Completer<AccessState>? pending;
  @override
  Future<AccessState> checkAccess() async =>
      pending == null ? accessState : await pending!.future;
}

class DelayedSearchGateway extends FixtureGateway {
  final oldResponse = Completer<LeadPage>();
  @override
  Future<LeadPage> loadLeads({
    String query = '', String status = '', String? cursor,
    int limit = 40, bool reviewOnly = false,
  }) {
    if (query.isEmpty) return oldResponse.future;
    return Future.value(const LeadPage(
      items: [LeadEntry(id: 7, name: 'نتيجة حديثة', phone: '0000000',
        source: 'MANUAL', status: 'NEW')],
      total: 1, hasMore: false,
    ));
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
    // No licensed font asset is bundled in public QA: test the actual fallback.
    expect(appTheme.textTheme.bodyMedium?.fontFamily,
        ThemeData(brightness: Brightness.light, useMaterial3: true)
            .textTheme.bodyMedium?.fontFamily);
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

  testWidgets('editing customer status uses the native gateway', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('العملاء').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('عميل تجريبي').first);
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('lead_status_options')), findsOneWidget);
    await tester.ensureVisible(find.byKey(const Key('status_CONTACTED')));
    await tester.tap(find.byKey(const Key('status_CONTACTED')));
    await tester.pumpAndSettle();
    expect(gateway.updatedLeadStatuses, [(1, 'CONTACTED')]);
  });

  testWidgets('settings failure still provides return to native login', (tester) async {
    final gateway = SessionLostGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('الإعدادات').last);
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('return_native_on_settings_error')),
        findsOneWidget);
    await tester.tap(find.byKey(const Key('return_native_on_settings_error')));
    await tester.pumpAndSettle();
    expect(gateway.returnToNativeCalls, 1);
  });

  testWidgets('customer notes can be edited and cleared via native gateway', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('العملاء').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('عميل تجريبي').first);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('edit_customer_notes_button')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('edit_customer_notes')), findsOneWidget);
    await tester.enterText(
      find.byKey(const Key('edit_customer_notes')), 'اتصل بعد يومين',
    );
    await tester.tap(find.byKey(const Key('save_customer_notes')));
    await tester.pumpAndSettle();
    expect(gateway.updatedLeadNotes, [(1, 'اتصل بعد يومين')]);
    expect(tester.takeException(), isNull);
  });

  testWidgets('canceling customer notes preserves existing data', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('العملاء').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('عميل تجريبي').first);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('edit_customer_notes_button')));
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byKey(const Key('edit_customer_notes')), 'تعديل غير مرغوب',
    );
    await tester.tap(find.text('إلغاء').last);
    await tester.pumpAndSettle();
    expect(gateway.updatedLeadNotes, isEmpty);
    expect(tester.takeException(), isNull);
  });

  testWidgets('dashboard re-fetches real counts when app resumes', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    final before = gateway.dashboardCalls;
    expect(before, greaterThan(0));
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();
    expect(gateway.dashboardCalls, greaterThan(before));
    expect(tester.takeException(), isNull);
  });

  testWidgets('customer search is retained and list refreshed on resume', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('العملاء').last);
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('lead_search')), 'عميل');
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pumpAndSettle();
    final before = gateway.leadCalls;
    expect(before, greaterThan(1));
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();
    expect(gateway.leadCalls, greaterThan(before));
    final field = tester.widget<TextField>(find.byKey(const Key('lead_search')));
    expect(field.controller?.text, 'عميل');
    expect(tester.takeException(), isNull);
  });

  testWidgets('audit history refreshes after returning from Android', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('السجل').last);
    await tester.pumpAndSettle();
    final before = gateway.historyCalls;
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();
    expect(gateway.historyCalls, greaterThan(before));
    expect(find.text('سجل العمليات (1)'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('dashboard manual-add shortcut opens real form directly once', (tester) async {
    tester.view.physicalSize = const Size(420, 1000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('إضافة يدوية'));
    await tester.tap(find.text('إضافة يدوية'));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('manual_lead_phone')), findsOneWidget);
    await tester.tap(find.text('إلغاء').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('السجل').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('العملاء').last);
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('manual_lead_phone')), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('renaming a customer updates CRM only through the gateway', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('العملاء').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('عميل تجريبي').first);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('edit_customer_name_button')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('edit_customer_name')), findsOneWidget);
    await tester.enterText(
      find.byKey(const Key('edit_customer_name')), '  العميل الجديد  ',
    );
    await tester.tap(find.byKey(const Key('save_customer_name')));
    await tester.pumpAndSettle();
    expect(gateway.updatedLeadNames, [(1, 'العميل الجديد')]);
    expect(tester.takeException(), isNull);
  });

  testWidgets('canceling a name edit does not modify the customer', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('العملاء').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('عميل تجريبي').first);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('edit_customer_name_button')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('edit_customer_name')), 'اسم مرفوض');
    await tester.tap(find.text('إلغاء').last);
    await tester.pumpAndSettle();
    expect(gateway.updatedLeadNames, isEmpty);
    expect(tester.takeException(), isNull);
  });

  testWidgets('an empty customer name disables the save button', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('العملاء').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('عميل تجريبي').first);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('edit_customer_name_button')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('edit_customer_name')), '   ');
    await tester.pumpAndSettle();
    expect(
      tester.widget<FilledButton>(find.byKey(const Key('save_customer_name'))).onPressed,
      isNull,
    );
    expect(gateway.updatedLeadNames, isEmpty);
  });

  testWidgets('QA APK opens the light Flutter login front door, not Kotlin dashboard', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(
      gateway: gateway, requiresAuthentication: true,
    ));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('flutter_login_gate')), findsOneWidget);
    expect(find.byType(WaAppShell), findsNothing);
    final theme = Theme.of(tester.element(find.byKey(const Key('flutter_login_gate'))));
    expect(theme.brightness, Brightness.light);
    await tester.tap(find.byKey(const Key('login_via_native')));
    await tester.pumpAndSettle();
    expect(gateway.nativeLoginCalls, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets('verified account opens all four real Flutter tabs', (tester) async {
    final gateway = FixtureGateway()
      ..accessState = const AccessState(allowed: true, message: 'مفعل');
    await tester.pumpWidget(WaLeadSaverApp(
      gateway: gateway, requiresAuthentication: true,
    ));
    await tester.pumpAndSettle();
    expect(find.byType(WaAppShell), findsOneWidget);
    // The home title and navigation destination legitimately share a label.
    expect(find.text('الرئيسية'), findsWidgets);
    expect(find.text('العملاء'), findsOneWidget);
    expect(find.text('السجل'), findsOneWidget);
    expect(find.text('الإعدادات'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('return from native authentication unlocks Flutter on resume', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(WaLeadSaverApp(
      gateway: gateway, requiresAuthentication: true,
    ));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('flutter_login_gate')), findsOneWidget);
    gateway.accessState = const AccessState(allowed: true, message: 'مفعل');
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pumpAndSettle();
    expect(find.byType(WaAppShell), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('pending and revoked authentication hide previously visible customer UI', (tester) async {
    final gateway = DelayedAccessGateway()
      ..accessState = const AccessState(allowed: true, message: 'مفعل');
    await tester.pumpWidget(WaLeadSaverApp(
      gateway: gateway, requiresAuthentication: true,
    ));
    await tester.pumpAndSettle();
    expect(find.byType(WaAppShell), findsOneWidget);
    gateway.pending = Completer<AccessState>();
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
    expect(find.byType(WaAppShell), findsNothing);
    expect(find.byKey(const Key('flutter_login_gate')), findsOneWidget);
    gateway.pending!.complete(const AccessState(allowed: false, message: 'انتهت الجلسة'));
    await tester.pumpAndSettle();
    expect(find.byType(WaAppShell), findsNothing);
    expect(find.text('انتهت الجلسة'), findsOneWidget);
  });

  testWidgets('obsolete search response cannot appear during debounce', (tester) async {
    final gateway = DelayedSearchGateway();
    await tester.pumpWidget(WaLeadSaverApp(gateway: gateway));
    await tester.pumpAndSettle();
    await tester.tap(find.text('العملاء').last);
    await tester.pump();
    await tester.enterText(find.byKey(const Key('lead_search')), 'حديث');
    gateway.oldResponse.complete(const LeadPage(
      items: [LeadEntry(id: 1, name: 'نتيجة قديمة', phone: '0000000',
        source: 'MANUAL', status: 'NEW')],
      total: 1, hasMore: false,
    ));
    await tester.pump(const Duration(milliseconds: 100));
    expect(find.text('نتيجة قديمة'), findsNothing);
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pumpAndSettle();
    expect(find.text('نتيجة حديثة'), findsOneWidget);
    expect(find.text('نتيجة قديمة'), findsNothing);
  });

}
