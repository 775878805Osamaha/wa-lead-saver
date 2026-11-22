import 'package:flutter/material.dart';
import '../data/gateway.dart';
import '../theme/wa_theme.dart';
import '../widgets/shared.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({
    super.key, required this.gateway, this.isPreview = false,
  });
  final LeadGateway gateway;
  final bool isPreview;

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  Map<String, bool> _settings = {};
  late Future<AccountInfo> _accountInfo;
  bool _loading = true;
  String? _saving;
  String? _error;

  @override
  void initState() {
    super.initState();
    _accountInfo = widget.gateway.loadAccountInfo();
    _load();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final values = await widget.gateway.loadSettings();
      if (!mounted) return;
      setState(() => _settings = values);
    } catch (_) {
      if (mounted) {
        setState(() => _error =
          'تعذر جلب الإعدادات من Android. لم يتم تغيير أي تفضيل.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _change(String key, bool next) async {
    if (_saving != null) return;
    setState(() => _saving = key);
    try {
      await widget.gateway.changeSetting(key, next);
      if (!mounted) return;
      setState(() => _settings = {..._settings, key: next});
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('لم يتم حفظ الإعداد؛ بقيت القيمة السابقة')),
      );
    } finally {
      if (mounted) setState(() => _saving = null);
    }
  }

  Future<void> _returnToNative() async {
    try {
      await widget.gateway.returnToNative();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text(
          'تعذر العودة إلى التطبيق الأصلي من هذه النسخة',
        )),
      );
    }
  }

  Widget _accountCard() => FutureBuilder<AccountInfo>(
    future: _accountInfo,
    builder: (context, snapshot) {
      final info = snapshot.data;
      return Card(
        key: const Key('account_license_card'),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Row(children: [
                Icon(Icons.verified_user_outlined, color: WaPalette.green),
                SizedBox(width: 8),
                Text('الحساب والترخيص', style: TextStyle(
                  fontWeight: FontWeight.w800, fontSize: 16,
                )),
              ]),
              const SizedBox(height: 8),
              if (snapshot.hasError)
                const Text('تعذر قراءة حالة الحساب من Android',
                  style: TextStyle(color: WaPalette.red))
              else if (info == null)
                const LinearProgressIndicator()
              else ...[
                Text(info.licenseStatus.isEmpty ? 'حالة غير معروفة' : info.licenseStatus,
                  key: const Key('license_status'),
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    color: info.canSaveContacts
                        ? WaPalette.green : WaPalette.amber,
                  )),
                if (info.email.isNotEmpty) ...[
                  const SizedBox(height: 5),
                  Text(info.email, maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    textDirection: TextDirection.ltr,
                    style: const TextStyle(fontSize: 13),
                  ),
                ],
                if (info.accountNumber.isNotEmpty)
                  Text(info.accountNumber, maxLines: 1,
                    overflow: TextOverflow.ellipsis),
                Text(info.role, style: const TextStyle(
                  fontSize: 12, color: WaPalette.muted,
                )),
                if (info.licenseMessage.isNotEmpty)
                  Text(info.licenseMessage, style: const TextStyle(
                    fontSize: 12, color: WaPalette.muted,
                  )),
              ],
              if (!widget.isPreview) ...[
                const SizedBox(height: 8),
                OutlinedButton.icon(
                  key: const Key('return_to_native'),
                  onPressed: _returnToNative,
                  icon: const Icon(Icons.arrow_back),
                  label: const Text('العودة لإدارة الحساب'),
                ),
              ],
            ],
          ),
        ),
      );
    },
  );

  Widget _permissionCard({
    required String keyName,
    required String title,
    required String subtitle,
    required IconData icon,
  }) {
    final granted = _settings[keyName];
    return Card(
      child: ListTile(
        dense: true,
        minVerticalPadding: 2,
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 1),
        leading: Icon(icon, color: WaPalette.green),
        title: Text(title, style: const TextStyle(
          fontSize: 14, fontWeight: FontWeight.w800,
        )),
        subtitle: Text(subtitle, style: const TextStyle(
          color: WaPalette.muted, fontSize: 11.5,
        )),
        trailing: Icon(
          granted == null
              ? Icons.help_outline
              : granted
                  ? Icons.check_circle
                  : Icons.error_outline,
          color: granted == null
              ? WaPalette.muted
              : granted ? WaPalette.accent : WaPalette.red,
        ),
      ),
    );
  }

  Widget _switchCard({
    required String keyName,
    required String title,
    required String hint,
    required IconData icon,
  }) {
    return Card(
      child: SwitchListTile.adaptive(
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 0),
        visualDensity: const VisualDensity(vertical: -1),
        secondary: Icon(icon, color: WaPalette.green),
        title: Text(title, style: const TextStyle(
          fontWeight: FontWeight.w800, fontSize: 14,
        )),
        subtitle: Text(hint, style: const TextStyle(
          color: WaPalette.muted, fontSize: 11.5,
        )),
        value: _settings[keyName] ?? false,
        onChanged: _saving == null && _settings.containsKey(keyName)
            ? (next) => _change(keyName, next)
            : null,
        activeTrackColor: WaPalette.accent,
        activeThumbColor: Colors.white,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const WaFeedback(message: 'جاري قراءة الإعدادات', loading: true);
    }
    if (_error != null) {
      return WaFeedback(message: _error!, onRetry: _load);
    }
    return ListView(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 24),
      children: [
        const WaSectionHeader(
          title: 'الإعدادات',
          subtitle: 'تخصيص والتحكم في التطبيق',
        ),
        const SizedBox(height: 6),
        _accountCard(),
        const SizedBox(height: 8),
        _permissionCard(
          keyName: 'notificationPermission',
          title: 'إذن قراءة الإشعارات',
          subtitle: 'حالة الوصول إلى الإشعارات في Android',
          icon: Icons.notifications_outlined,
        ),
        const SizedBox(height: 6),
        _permissionCard(
          keyName: 'contactsPermission',
          title: 'صلاحية جهات الاتصال',
          subtitle: 'السماح بحفظ الأرقام',
          icon: Icons.contact_page_outlined,
        ),
        const SizedBox(height: 6),
        _permissionCard(
          keyName: 'batteryOptimizationIgnored',
          title: 'استثناء تحسين البطارية',
          subtitle: 'حالة استثناء التطبيق من تحسين البطارية',
          icon: Icons.battery_charging_full_outlined,
        ),
        const SizedBox(height: 6),
        _switchCard(
          keyName: 'monitorWhatsApp',
          title: 'مراقبة واتساب الرسمي',
          hint: 'اكتشاف الأرقام من إشعارات WhatsApp',
          icon: Icons.chat_bubble_outline,
        ),
        const SizedBox(height: 6),
        _switchCard(
          keyName: 'monitorWhatsAppBusiness',
          title: 'مراقبة واتساب للأعمال',
          hint: 'اكتشاف الأرقام من WhatsApp Business',
          icon: Icons.business_outlined,
        ),
        const SizedBox(height: 6),
        _switchCard(
          keyName: 'ignoreGroupMessages',
          title: 'تجاهل رسائل المجموعات',
          hint: 'عدم إضافة أرقام من المجموعات',
          icon: Icons.groups_outlined,
        ),
        const SizedBox(height: 16),
        const WaSectionHeader(title: 'الحفظ والترخيص'),
        _switchCard(
          keyName: 'autoSaveLeads',
          title: 'الحفظ التلقائي للعملاء',
          hint: 'الحفظ عند توفر الصلاحيات وتأكيد الرقم',
          icon: Icons.person_add_alt_1_outlined,
        ),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          onPressed: () {
            setState(() => _accountInfo = widget.gateway.loadAccountInfo());
            _load();
          },
          icon: const Icon(Icons.refresh),
          label: const Text('تحديث حالة الإعدادات'),
        ),
      ],
    );
  }
}
