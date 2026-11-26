import 'package:flutter/services.dart';

/// Data contract for the existing native Android host. No demo data is
/// substituted when the native bridge has not yet been connected.
abstract class LeadGateway {
  Future<DashboardSummary> loadOverview();
  Future<LeadPage> loadLeads({
    String query = '',
    String status = '',
    String? cursor,
    int limit = 40,
    bool reviewOnly = false,
  });
  Future<HistoryPage> loadHistory({
    String filter = 'ALL',
    String? cursor,
    int limit = 60,
  });
  Future<Map<String, bool>> loadSettings();
  Future<void> changeSetting(String key, bool enabled);
  Future<void> saveLead(int id);
  Future<void> updateLeadStatus(int id, String status);
  Future<void> updateLeadNotes(int id, String notes);
  Future<void> updateLeadDisplayName(int id, String name);
  Future<void> callLead(String number);
  Future<void> openWhatsAppLead(String number);
  Future<int> createLead({required String phone, String name = ''});
  Future<void> exportHistory();
  Future<void> clearHistory();
  Future<void> openNativeTool(String tool);
  Future<AccountInfo> loadAccountInfo();
  Future<void> returnToNative();
  Future<String> loadCountryCode();
  Future<void> changeCountryCode(String code);
  Future<void> openPermissionSettings(String permission);
  Future<AccessState> checkAccess();
  Future<void> openNativeLogin();
}

/// Safe account access result: no customer data or authentication tokens.
class AccessState {
  const AccessState({required this.allowed, required this.message});
  final bool allowed;
  final String message;

  factory AccessState.fromMap(Map<String, dynamic> data) => AccessState(
    allowed: data['allowed'] == true,
    message: (data['message'] ?? '').toString(),
  );
}

/// Only account summary values authorized by the existing native session.
class AccountInfo {
  const AccountInfo({
    required this.email,
    required this.accountNumber,
    required this.role,
    required this.licenseStatus,
    required this.licenseMessage,
    required this.canSaveContacts,
  });
  final String email;
  final String accountNumber;
  final String role;
  final String licenseStatus;
  final String licenseMessage;
  final bool canSaveContacts;

  factory AccountInfo.fromMap(Map<String, dynamic> data) => AccountInfo(
    email: (data['email'] ?? '').toString(),
    accountNumber: (data['accountNumber'] ?? '').toString(),
    role: (data['role'] ?? '').toString(),
    licenseStatus: (data['licenseStatus'] ?? '').toString(),
    licenseMessage: (data['licenseMessage'] ?? '').toString(),
    canSaveContacts: data['canSaveContacts'] == true,
  );
}

class DashboardSummary {
  const DashboardSummary({
    required this.saved,
    required this.review,
    required this.total,
    this.monitoringActive = false,
    this.accuracyPercentage,
    this.evaluatedCases = 0,
    this.passedCases = 0,
  });
  final int saved;
  final int review;
  final int total;
  final bool monitoringActive;

  /// Null means no measured or user-verified accuracy; never invent 100%.
  final int? accuracyPercentage;
  final int evaluatedCases;
  final int passedCases;

  factory DashboardSummary.fromMap(Map<String, dynamic> data) =>
      DashboardSummary(
        saved: _asInt(data['saved']),
        review: _asInt(data['review']),
        total: _asInt(data['total']),
        monitoringActive: data['monitoringActive'] == true,
        accuracyPercentage: data['accuracyPercentage'] is num
            ? (data['accuracyPercentage'] as num).toInt().clamp(0, 100)
            : null,
        evaluatedCases: _asInt(data['evaluatedCases']),
        passedCases: _asInt(data['passedCases']),
      );
}

class LeadEntry {
  const LeadEntry({
    required this.id,
    required this.name,
    required this.phone,
    required this.status,
    required this.source,
    this.isSaved = false,
    this.notes = '',
  });
  final int id;
  final String name;
  final String phone;
  final String status;
  final String source;
  final bool isSaved;
  final String notes;

  factory LeadEntry.fromMap(Map<String, dynamic> data) => LeadEntry(
        id: _asInt(data['id']),
        name: (data['name'] ?? '').toString(),
        phone: (data['phone'] ?? '').toString(),
        status: (data['status'] ?? '').toString(),
        source: (data['source'] ?? '').toString(),
        isSaved: data['isSaved'] == true,
        notes: (data['notes'] ?? '').toString(),
      );
}

class HistoryEntry {
  const HistoryEntry({
    required this.id,
    required this.status,
    required this.details,
    required this.source,
    required this.timestamp,
  });
  final int id;
  final String status;
  final String details;
  final String source;
  final int timestamp;

  factory HistoryEntry.fromMap(Map<String, dynamic> data) => HistoryEntry(
        id: _asInt(data['id']),
        status: (data['status'] ?? '').toString(),
        details: (data['details'] ?? '').toString(),
        source: (data['source'] ?? '').toString(),
        timestamp: _asInt(data['timestamp']),
      );
}

class LeadPage {
  const LeadPage({
    required this.items,
    required this.hasMore,
    this.nextCursor,
    required this.total,
  });
  final List<LeadEntry> items;
  final bool hasMore;
  final String? nextCursor;
  final int total;

  factory LeadPage.fromMap(Map<String, dynamic> data) => LeadPage(
        items: _asMapList(data['items'])
            .map(LeadEntry.fromMap)
            .toList(growable: false),
        hasMore: data['hasMore'] == true,
        nextCursor: data['nextCursor']?.toString(),
        total: _asInt(data['total']),
      );
}

class HistoryPage {
  const HistoryPage({
    required this.items,
    required this.hasMore,
    this.nextCursor,
    required this.total,
  });
  final List<HistoryEntry> items;
  final bool hasMore;
  final String? nextCursor;
  final int total;

  factory HistoryPage.fromMap(Map<String, dynamic> data) => HistoryPage(
        items: _asMapList(data['items'])
            .map(HistoryEntry.fromMap)
            .toList(growable: false),
        hasMore: data['hasMore'] == true,
        nextCursor: data['nextCursor']?.toString(),
        total: _asInt(data['total']),
      );
}

int _asInt(Object? value) => value is num
    ? value.toInt()
    : int.tryParse(value?.toString() ?? '') ?? 0;

Map<String, dynamic> _asMap(Object? value) {
  if (value is! Map) return <String, dynamic>{};
  return value.map<String, dynamic>(
    (key, val) => MapEntry(key.toString(), val),
  );
}

List<Map<String, dynamic>> _asMapList(Object? value) {
  if (value is! List) return const [];
  return value.map(_asMap).toList(growable: false);
}

/// Android method channel to be attached to the existing Kotlin/Room host.
/// The missing-channel exception is deliberately surfaced in UI, not masked.
class NativeLeadGateway implements LeadGateway {
  const NativeLeadGateway();

  static const MethodChannel _channel =
      MethodChannel('wa_lead_saver/native_v1');

  Future<Map<String, dynamic>> _invoke(
    String method, [Map<String, Object?>? arguments]
  ) async {
    final result = await _channel.invokeMethod<Object?>(
      method, arguments,
    );
    return _asMap(result);
  }

  @override
  Future<DashboardSummary> loadOverview() async =>
      DashboardSummary.fromMap(await _invoke('getOverview'));

  @override
  Future<LeadPage> loadLeads({
    String query = '',
    String status = '',
    String? cursor,
    int limit = 40,
    bool reviewOnly = false,
  }) async => LeadPage.fromMap(await _invoke('getLeads', {
        'query': query,
        'status': status,
        'cursor': cursor,
        'limit': limit,
        'reviewOnly': reviewOnly,
      }));

  @override
  Future<HistoryPage> loadHistory({
    String filter = 'ALL',
    String? cursor,
    int limit = 60,
  }) async => HistoryPage.fromMap(await _invoke('getHistory', {
        'filter': filter,
        'cursor': cursor,
        'limit': limit,
      }));

  @override
  Future<Map<String, bool>> loadSettings() async {
    final result = await _invoke('getSettings');
    return result.map((key, val) => MapEntry(key, val == true));
  }

  @override
  Future<void> changeSetting(String key, bool enabled) async {
    await _invoke('setSetting', {'key': key, 'enabled': enabled});
  }

  @override
  Future<void> saveLead(int id) async {
    await _invoke('saveLead', {'id': id});
  }

  @override
  Future<void> updateLeadStatus(int id, String status) async {
    await _invoke('updateLeadStatus', {'id': id, 'status': status});
  }

  @override
  Future<void> updateLeadNotes(int id, String notes) async {
    await _invoke('updateLeadNotes', {'id': id, 'notes': notes});
  }

  @override
  Future<void> updateLeadDisplayName(int id, String name) async {
    await _invoke('updateLeadDisplayName', {'id': id, 'name': name});
  }

  @override
  Future<void> callLead(String number) async {
    await _invoke('callLead', {'number': number});
  }

  @override
  Future<void> openWhatsAppLead(String number) async {
    await _invoke('openWhatsAppLead', {'number': number});
  }

  @override
  Future<int> createLead({required String phone, String name = ''}) async {
    final result = await _invoke('createLead', {'phone': phone, 'name': name});
    final id = result['id'];
    if (id is int && id > 0) return id;
    throw StateError('Native host did not return a valid lead ID');
  }

  @override
  Future<void> exportHistory() async {
    await _invoke('exportHistory');
  }

  @override
  Future<void> clearHistory() async {
    await _invoke('clearHistory', {
      'confirmation': 'CLEAR_ALL_AUDIT_HISTORY',
    });
  }

  @override
  Future<void> openNativeTool(String tool) async {
    await _invoke('openNativeTool', {'tool': tool});
  }

  @override
  Future<AccountInfo> loadAccountInfo() async =>
      AccountInfo.fromMap(await _invoke('getAccountInfo'));

  @override
  Future<void> returnToNative() async {
    await _invoke('closeFlutter');
  }

  @override
  Future<String> loadCountryCode() async {
    final data = await _invoke('getCountryCode');
    final code = data['countryCode'];
    if (code is! String || code.isEmpty) {
      throw StateError('Invalid default country code from Android');
    }
    return code;
  }

  @override
  Future<void> changeCountryCode(String code) async {
    await _invoke('setCountryCode', {'countryCode': code});
  }

  @override
  Future<void> openPermissionSettings(String permission) async {
    await _invoke('openPermissionSettings', {'permission': permission});
  }

  @override
  Future<AccessState> checkAccess() async =>
      AccessState.fromMap(await _invoke('getAccessState'));

  @override
  Future<void> openNativeLogin() async {
    await _invoke('openNativeLogin');
  }
}
