import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'src/data/gateway.dart';
import 'src/screens/app_shell.dart';
import 'src/theme/wa_theme.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const WaLeadSaverApp(gateway: NativeLeadGateway()));
}

class WaLeadSaverApp extends StatelessWidget {
  const WaLeadSaverApp({super.key, required this.gateway});
  final LeadGateway gateway;

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'WA Lead Saver',
        debugShowCheckedModeBanner: false,
        theme: makeWaTheme(),
        locale: const Locale('ar'),
        supportedLocales: const [Locale('ar'), Locale('en')],
        localizationsDelegates: const [
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        builder: (context, child) => Directionality(
          textDirection: TextDirection.rtl,
          child: child ?? const SizedBox.shrink(),
        ),
        home: WaAppShell(gateway: gateway),
      );
}
