import 'package:flutter/material.dart';
import '../data/gateway.dart';
import '../theme/wa_theme.dart';
import 'app_shell.dart';

/// Launches the new light Flutter UI by default on the QA APK.
/// Customer data is never shown until native Supabase auth/license verifies.
class AuthenticatedGate extends StatefulWidget {
  const AuthenticatedGate({super.key, required this.gateway});
  final LeadGateway gateway;

  @override
  State<AuthenticatedGate> createState() => _AuthenticatedGateState();
}

class _AuthenticatedGateState extends State<AuthenticatedGate>
    with WidgetsBindingObserver {
  late Future<AccessState> _access;
  bool _openingLogin = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _access = widget.gateway.checkAccess();
  }

  void _reload() {
    if (mounted) {
      setState(() {
        _access = widget.gateway.checkAccess();
      });
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _reload();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  Future<void> _openLogin() async {
    if (_openingLogin) return;
    setState(() => _openingLogin = true);
    try {
      await widget.gateway.openNativeLogin();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text(
          'تعذر فتح تسجيل الدخول. أعد المحاولة.',
        )),
      );
    } finally {
      if (mounted) setState(() => _openingLogin = false);
    }
  }

  @override
  Widget build(BuildContext context) => FutureBuilder<AccessState>(
    future: _access,
    builder: (context, snapshot) {
      // FutureBuilder retains old data while a new future is pending.
      // Fail closed: no customer UI until this verification completes.
      if (snapshot.connectionState == ConnectionState.done &&
          !snapshot.hasError && snapshot.data?.allowed == true) {
        return WaAppShell(
          key: const Key('authenticated_flutter_shell'),
          gateway: widget.gateway,
        );
      }

      final loading = snapshot.connectionState != ConnectionState.done;
      final message = loading
          ? 'نتحقق من جلسة الحساب...'
          : snapshot.hasError
          ? 'تعذر التحقق من الحساب. تحقق من اتصالك ثم أعد المحاولة.'
          : snapshot.data?.message ?? 'نتحقق من جلسة الحساب...';

      return Scaffold(
        key: const Key('flutter_login_gate'),
        backgroundColor: WaPalette.background,
        body: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 430),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Align(
                      alignment: Alignment.center,
                      child: Container(
                        width: 76,
                        height: 76,
                        decoration: BoxDecoration(
                          color: WaPalette.accent,
                          borderRadius: BorderRadius.circular(22),
                        ),
                        child: const Icon(
                          Icons.chat_bubble_outline,
                          size: 38,
                          color: WaPalette.ink,
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    const Text(
                      'WA Lead Saver',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 27,
                        fontWeight: FontWeight.w900,
                        color: WaPalette.ink,
                      ),
                    ),
                    const SizedBox(height: 6),
                    const Text(
                      'إدارة عملاء واتساب',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 16,
                        color: WaPalette.green,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 32),
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(20),
                        child: Column(
                          children: [
                            Icon(
                              loading ? Icons.shield_outlined : Icons.lock_outline,
                              size: 32,
                              color: WaPalette.green,
                            ),
                            const SizedBox(height: 12),
                            Text(
                              loading ? 'التحقق من الحساب...' : 'الدخول إلى التطبيق',
                              style: const TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.w800,
                                color: WaPalette.ink,
                              ),
                            ),
                            const SizedBox(height: 10),
                            Text(
                              message,
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                color: WaPalette.muted,
                                fontSize: 14,
                              ),
                            ),
                            const SizedBox(height: 20),
                            if (loading)
                              const CircularProgressIndicator()
                            else ...[
                              FilledButton.icon(
                                key: const Key('login_via_native'),
                                onPressed: _openingLogin ? null : _openLogin,
                                icon: const Icon(Icons.login),
                                label: const Text('تسجيل الدخول'),
                              ),
                              const SizedBox(height: 8),
                              TextButton.icon(
                                key: const Key('recheck_access'),
                                onPressed: _reload,
                                icon: const Icon(Icons.refresh),
                                label: const Text('إعادة التحقق'),
                              ),
                            ],
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 18),
                    const Text(
                      'نسخة اختبار Flutter. لا تعرض بيانات العملاء '
                      'إلا بعد تسجيل الدخول والتحقق من الترخيص.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 12,
                        color: WaPalette.muted,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      );
    },
  );
}
