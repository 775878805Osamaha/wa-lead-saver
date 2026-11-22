import 'package:flutter/material.dart';
import '../data/gateway.dart';
import '../theme/wa_theme.dart';
import 'dashboard_page.dart';
import 'history_page.dart';
import 'leads_page.dart';
import 'settings_page.dart';

class WaAppShell extends StatefulWidget {
  const WaAppShell({super.key, required this.gateway, this.isPreview = false});
  final LeadGateway gateway;
  final bool isPreview;

  @override
  State<WaAppShell> createState() => _WaAppShellState();
}

class _WaAppShellState extends State<WaAppShell> {
  int _selected = 0;
  bool _reviewOpen = false;

  void _navigate(int index) => setState(() {
        _selected = index;
        _reviewOpen = false;
      });

  @override
  Widget build(BuildContext context) {
    final pages = [
      DashboardPage(
        gateway: widget.gateway,
        onOpenReview: () => setState(() => _reviewOpen = true),
        onOpenLeads: () => _navigate(1),
        onOpenHistory: () => _navigate(2),
      ),
      LeadsPage(gateway: widget.gateway),
      HistoryPageView(gateway: widget.gateway),
      SettingsPage(gateway: widget.gateway, isPreview: widget.isPreview),
    ];

    return PopScope(
      canPop: !_reviewOpen,
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop && _reviewOpen) {
          setState(() => _reviewOpen = false);
        }
      },
      child: Scaffold(
        appBar: AppBar(
          toolbarHeight: 64,
          title: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('WA Lead Saver',
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
              const SizedBox(height: 2),
              Text(
                widget.isPreview ? 'معاينة التصميم' : (_reviewOpen ? 'مراجعة العملاء' : 'إدارة عملاء واتساب'),
                style: const TextStyle(fontSize: 12, color: WaPalette.green),
              ),
            ],
          ),
          actions: [
            if (_reviewOpen)
              IconButton(
                key: const Key('exit_review'),
                tooltip: 'العودة',
                onPressed: () => setState(() => _reviewOpen = false),
                icon: const Icon(Icons.arrow_back),
              )
            else
              Padding(
                padding: const EdgeInsets.only(left: 12),
                child: Container(
                  width: 42,
                  height: 42,
                  decoration: BoxDecoration(
                    color: WaPalette.accent,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: const Icon(
                    Icons.chat_bubble_outline, color: WaPalette.ink,
                  ),
                ),
              ),
          ],
        ),
        body: Column(
          children: [
            if (widget.isPreview)
              Container(
                key: const Key('preview_notice'),
                width: double.infinity,
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                color: WaPalette.raised,
                child: const Row(children: [
                  Icon(Icons.info_outline, size: 17, color: WaPalette.green),
                  SizedBox(width: 6),
                  Expanded(child: Text(
                    'معاينة فقط — بيانات تجريبية وليست جهات اتصال هاتفك',
                    style: TextStyle(fontSize: 11, color: WaPalette.green,
                      fontWeight: FontWeight.w700),
                  )),
                ]),
              ),
            Expanded(child: _reviewOpen
              ? LeadsPage(
                  key: const Key('review_page'),
                  gateway: widget.gateway,
                  reviewOnly: true,
                )
              : pages[_selected]),
          ],
        ),
        bottomNavigationBar: NavigationBar(
          height: 68,
          selectedIndex: _selected,
          onDestinationSelected: _navigate,
          destinations: const [
            NavigationDestination(
              icon: Icon(Icons.space_dashboard_outlined),
              selectedIcon: Icon(Icons.space_dashboard),
              label: 'الرئيسية',
            ),
            NavigationDestination(
              icon: Icon(Icons.people_alt_outlined),
              selectedIcon: Icon(Icons.people_alt),
              label: 'العملاء',
            ),
            NavigationDestination(
              icon: Icon(Icons.history_outlined),
              selectedIcon: Icon(Icons.history),
              label: 'السجل',
            ),
            NavigationDestination(
              icon: Icon(Icons.settings_outlined),
              selectedIcon: Icon(Icons.settings),
              label: 'الإعدادات',
            ),
          ],
        ),
      ),
    );
  }
}
