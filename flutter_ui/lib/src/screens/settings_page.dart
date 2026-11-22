import 'package:flutter/material.dart';
import '../data/gateway.dart';
import '../theme/wa_theme.dart';
import '../widgets/shared.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key, required this.gateway});
  final LeadGateway gateway;

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  Map<String, bool> _settings = {};
  bool _loading = true;
  String? _saving;
  String? _error;

  @override
  void initState() {
    super.initState();
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

  Widget _switch({
    required String keyName,
    required String title,
    required String hint,
    required IconData icon,
  }) {
    return SwitchListTile.adaptive(
      secondary: Icon(icon, color: WaPalette.green),
      title: Text(title, style: const TextStyle(
        fontWeight: FontWeight.w600, fontSize: 16,
      )),
      subtitle: Text(hint, style: const TextStyle(color: WaPalette.muted)),
      value: _settings[keyName] ?? false,
      onChanged: _saving == null && _settings.containsKey(keyName)
          ? (next) => _change(keyName, next)
          : null,
      activeTrackColor: WaPalette.green,
      activeThumbColor: WaPalette.background,
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
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      children: [
        const WaSectionHeader(
          title: 'الإعدادات',
          subtitle: 'خيارات الالتقاط والأذونات',
        ),
        Card(
          child: Column(children: [
            _switch(
              keyName: 'autoSaveLeads',
              title: 'الحفظ التلقائي للعملاء',
              hint: 'حفظ الأرقام بعد التحقق من صلاحيات النظام',
              icon: Icons.person_add_alt_1_outlined,
            ),
            const Divider(height: 1),
            _switch(
              keyName: 'monitorWhatsApp',
              title: 'مراقبة واتساب الرسمي',
              hint: 'متابعة الإشعارات الجديدة',
              icon: Icons.chat_outlined,
            ),
            const Divider(height: 1),
            _switch(
              keyName: 'monitorWhatsAppBusiness',
              title: 'مراقبة واتساب للأعمال',
              hint: 'متابعة إشعارات WhatsApp Business',
              icon: Icons.business_outlined,
            ),
            const Divider(height: 1),
            _switch(
              keyName: 'ignoreGroupMessages',
              title: 'تجاهل رسائل المجموعات',
              hint: 'تقليل الأرقام غير المرتبطة بمحادثات فردية',
              icon: Icons.groups_outlined,
            ),
          ]),
        ),
        const SizedBox(height: 18),
        const WaSectionHeader(title: 'المراقبة والترخيص'),
        const Card(
          child: ListTile(
            leading: Icon(Icons.verified_user_outlined,
              color: WaPalette.green),
            title: Text('خدمات Android الأصلية'),
            subtitle: Text(
              'خدمة الإشعارات، جهات الاتصال، الجلسات والتراخيص باقية في تطبيق Kotlin أثناء الترحيل',
            ),
          ),
        ),
        const SizedBox(height: 14),
        OutlinedButton.icon(
          onPressed: _load,
          icon: const Icon(Icons.refresh),
          label: const Text('تحديث الإعدادات'),
        ),
      ],
    );
  }
}
