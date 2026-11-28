import 'package:flutter/material.dart';
import '../theme/wa_theme.dart';

String _normalized(String value) =>
    value.trim().toLowerCase().replaceAll('_', ' ');

String historyStatusLabel(String value) {
  switch (_normalized(value)) {
    case 'saved': return 'تم الحفظ';
    case 'needs review': return 'بانتظار المراجعة';
    case 'duplicate': return 'مكرر';
    case 'group excluded': return 'مجموعة مستبعدة';
    case 'excluded': return 'مستبعد';
    case 'failed': return 'فشل الحفظ';
    case 'invalid number': return 'رقم غير صالح';
    default: return value.trim().isEmpty ? 'حالة غير معروفة' : value;
  }
}

Color historyStatusColor(String value) {
  switch (_normalized(value)) {
    case 'saved': return WaPalette.green;
    case 'failed':
    case 'invalid number': return WaPalette.red;
    case 'needs review': return WaPalette.amber;
    default: return WaPalette.muted;
  }
}
