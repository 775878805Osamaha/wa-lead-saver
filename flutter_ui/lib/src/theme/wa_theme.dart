import 'package:flutter/material.dart';

/// Light-only palette for the Flutter redesign.
///
/// Deep green is reserved for accessible text/icons on white surfaces,
/// while bright WhatsApp green remains available for decorative accents.
abstract final class WaPalette {
  static const background = Color(0xFFF7FAF8);
  static const surface = Color(0xFFFFFFFF);
  static const raised = Color(0xFFE7F6EC);
  static const border = Color(0xFFE0EAE4);
  static const green = Color(0xFF147D45);
  static const accent = Color(0xFF25D366);
  static const ink = Color(0xFF17251E);
  static const muted = Color(0xFF64736B);
  static const amber = Color(0xFF986117);
  static const red = Color(0xFFB6333D);
}

ThemeData makeWaTheme() {
  final scheme = ColorScheme.fromSeed(
    seedColor: WaPalette.green,
    brightness: Brightness.light,
    surface: WaPalette.surface,
  ).copyWith(
    primary: WaPalette.green,
    onPrimary: Colors.white,
    secondary: WaPalette.accent,
    onSecondary: WaPalette.ink,
    surface: WaPalette.surface,
    onSurface: WaPalette.ink,
    outline: WaPalette.border,
    error: WaPalette.red,
  );

  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.light,
    scaffoldBackgroundColor: WaPalette.background,
    colorScheme: scheme,
    splashColor: WaPalette.raised,
    appBarTheme: const AppBarTheme(
      backgroundColor: WaPalette.surface,
      foregroundColor: WaPalette.ink,
      scrolledUnderElevation: 0,
      elevation: 0,
      surfaceTintColor: Colors.transparent,
      titleTextStyle: TextStyle(
        color: WaPalette.ink,
        fontSize: 22,
        fontWeight: FontWeight.w700,
      ),
    ),
    cardTheme: const CardThemeData(
      color: WaPalette.surface,
      surfaceTintColor: Colors.transparent,
      elevation: 1,
      shadowColor: Color(0x140B3A20),
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(20)),
        side: BorderSide(color: WaPalette.border),
      ),
    ),
    dividerColor: WaPalette.border,
    inputDecorationTheme: const InputDecorationTheme(
      filled: true,
      fillColor: WaPalette.surface,
      hintStyle: TextStyle(color: WaPalette.muted),
      labelStyle: TextStyle(color: WaPalette.muted),
      prefixIconColor: WaPalette.green,
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.all(Radius.circular(16)),
        borderSide: BorderSide(color: WaPalette.border),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.all(Radius.circular(16)),
        borderSide: BorderSide(color: WaPalette.green, width: 1.5),
      ),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.all(Radius.circular(16)),
        borderSide: BorderSide(color: WaPalette.border),
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: WaPalette.green,
        foregroundColor: Colors.white,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
        ),
        minimumSize: const Size(0, 44),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(
        foregroundColor: WaPalette.green,
        side: const BorderSide(color: WaPalette.border),
      ),
    ),
    chipTheme: ChipThemeData(
      backgroundColor: WaPalette.surface,
      selectedColor: WaPalette.raised,
      side: const BorderSide(color: WaPalette.border),
      labelStyle: const TextStyle(color: WaPalette.ink),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
      ),
    ),
    snackBarTheme: const SnackBarThemeData(
      backgroundColor: WaPalette.ink,
      contentTextStyle: TextStyle(color: Colors.white),
    ),
    navigationBarTheme: NavigationBarThemeData(
      backgroundColor: WaPalette.surface,
      indicatorColor: WaPalette.raised,
      surfaceTintColor: Colors.transparent,
      elevation: 3,
      iconTheme: WidgetStateProperty.resolveWith((states) {
        return IconThemeData(
          color: states.contains(WidgetState.selected)
              ? WaPalette.green : WaPalette.muted,
        );
      }),
      labelTextStyle: WidgetStateProperty.resolveWith((states) {
        return TextStyle(
          color: states.contains(WidgetState.selected)
              ? WaPalette.green : WaPalette.muted,
          fontSize: 12,
          fontWeight: FontWeight.w600,
        );
      }),
    ),
  );
}
