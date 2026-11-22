// Standalone visual preview only. No access to actual customers, Supabase,
// notification listeners, WhatsApp, Contacts, or the production Room database.
// Built under a DIFFERENT Android application ID from WA Lead Saver.
import 'package:flutter/material.dart';
import 'main.dart';
import 'src/data/gateway.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(Banner(
    message: 'DEMO',
    location: BannerLocation.topStart,
    child: WaLeadSaverApp(gateway: PreviewLeadGateway()),
  ));
}

class PreviewLeadGateway implements LeadGateway {
  final Map<String, bool> _settings = {
    'autoSaveLeads': false,
    'monitorWhatsApp': true,
    'monitorWhatsAppBusiness': true,
    'ignoreGroupMessages': true,
    'notificationPermission': true,
    'contactsPermission': true,
    'batteryOptimizationIgnored': true,
  };

  static const List<LeadEntry> sampleLeads = [
    LeadEntry(id: 1, name: 'عميل تجريبي ١', phone: '+967700000001',
      status: 'NEW', source: 'WhatsApp Business'),
    LeadEntry(id: 2, name: 'عميل تجريبي ٢', phone: '+967700000002',
      status: 'FOLLOW_UP', source: 'WhatsApp Business'),
    LeadEntry(id: 3, name: 'عميل تجريبي ٣', phone: '+967700000003',
      status: 'INTERESTED', source: 'WhatsApp'),
    LeadEntry(id: 4, name: 'عميل تجريبي ٤', phone: '+967700000004',
      status: 'NEW', source: 'WhatsApp'),
  ];

  @override
  Future<DashboardSummary> loadOverview() async => const DashboardSummary(
        total: 1260, saved: 1149, review: 111, monitoringActive: true,
      );

  @override
  Future<LeadPage> loadLeads({
    String query = '', String status = '', String? cursor, int limit = 40,
    bool reviewOnly = false,
  }) async {
    final visible = sampleLeads.where((lead) =>
      (query.isEmpty || lead.name.contains(query) || lead.phone.contains(query)) &&
      (status.isEmpty || lead.status == status)
    ).take(limit).toList(growable: false);
    return LeadPage(items: visible, total: visible.length,
        hasMore: false);
  }

  @override
  Future<HistoryPage> loadHistory({
    String filter = 'ALL', String? cursor, int limit = 60,
  }) async {
    final entries = <HistoryEntry>[
      const HistoryEntry(
        id: 1, status: 'Saved', details: 'تم حفظ جهة اتصال تجريبية',
        source: 'WhatsApp Business', timestamp: 1795000000000,
      ),
      const HistoryEntry(
        id: 2, status: 'Needs Review', details: 'رقم تجريبي ينتظر المراجعة',
        source: 'WhatsApp', timestamp: 1794999000000,
      ),
      const HistoryEntry(
        id: 3, status: 'Duplicate', details: 'رقم تجريبي مكرر',
        source: 'WhatsApp Business', timestamp: 1794998000000,
      ),
    ];
    final visible = entries.where((entry) {
      switch (filter) {
        case 'SAVED': return entry.status == 'Saved';
        case 'NEEDS_REVIEW': return entry.status == 'Needs Review';
        case 'DUPLICATE': return entry.status == 'Duplicate';
        case 'ALL': return true;
        default: return false;
      }
    }).toList(growable: false);
    return HistoryPage(items: visible.take(limit).toList(),
        total: visible.length, hasMore: false);
  }

  @override
  Future<Map<String, bool>> loadSettings() async => {..._settings};

  @override
  Future<void> changeSetting(String key, bool enabled) async {
    if (!_settings.containsKey(key)) throw UnsupportedError('Unknown preference');
    if (const {'notificationPermission', 'contactsPermission',
      'batteryOptimizationIgnored'}.contains(key)) {
      throw UnsupportedError('Read-only permission');
    }
    _settings[key] = enabled; // only the in-memory preview, never Android.
  }

  @override
  Future<void> saveLead(int id) async =>
      throw UnsupportedError('Preview cannot write to contacts');

  @override
  Future<void> callLead(String number) async =>
      throw UnsupportedError('Preview cannot dial phone numbers');

  @override
  Future<void> openWhatsAppLead(String number) async =>
      throw UnsupportedError('Preview cannot send WhatsApp messages');
}
