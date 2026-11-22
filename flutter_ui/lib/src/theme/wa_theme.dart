import 'package:flutter/material.dart';

abstract final class WaPalette {
  static const background = Color(0xFF080E0B);
  static const surface = Color(0xFF111A15);
  static const raised = Color(0xFF173426);
  static const border = Color(0xFF273A30);
  static const green = Color(0xFF29E377);
  static const white = Color(0xFFF4F8F5);
  static const muted = Color(0xFF98A8A0);
  static const amber = Color(0xFFF3B44D);
  static const red = Color(0xFFFF6D73);
}

ThemeData makeWaTheme() {
  final scheme = ColorScheme.fromSeed(
    seedColor: WaPalette.green,
    brightness: Brightness.dark,
    surface: WaPalette.surface,
  );
  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    scaffoldBackgroundColor: WaPalette.background,
    colorScheme: scheme.copyWith(primary: WaPalette.green),
    appBarTheme: const AppBarTheme(
      backgroundColor: WaPalette.background,
      foregroundColor: WaPalette.white,
      elevation: 0,
    ),
    cardTheme: const CardThemeData(
      color: WaPalette.surface,
      elevation: 0,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(20)),
        side: BorderSide(color: WaPalette.border),
      ),
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: WaPalette.surface,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(18),
        borderSide: const BorderSide(color: WaPalette.border),
      ),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: WaPalette.surface,
      indicatorColor: WaPalette.raised,
      labelTextStyle: WidgetStateProperty.all(
        const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
      ),
    ),
  );
}
