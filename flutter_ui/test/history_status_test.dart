import 'package:flutter_test/flutter_test.dart';
import 'package:wa_lead_saver_ui/src/theme/wa_theme.dart';
import 'package:wa_lead_saver_ui/src/widgets/history_status.dart';

void main() {
  test('recognizes native uppercase and display statuses', () {
    expect(historyStatusLabel('NEEDS_REVIEW'), 'بانتظار المراجعة');
    expect(historyStatusLabel('Saved'), 'تم الحفظ');
    expect(historyStatusLabel('GROUP_EXCLUDED'), 'مجموعة مستبعدة');
    expect(historyStatusLabel('INVALID_NUMBER'), 'رقم غير صالح');
  });
  test('never presents failure or unknown results as success', () {
    expect(historyStatusColor('FAILED'), WaPalette.red);
    expect(historyStatusColor('INVALID_NUMBER'), WaPalette.red);
    expect(historyStatusColor('NEEDS_REVIEW'), WaPalette.amber);
    expect(historyStatusColor('SAVED'), WaPalette.green);
    expect(historyStatusColor('unknown'), WaPalette.muted);
    expect(historyStatusLabel(''), 'حالة غير معروفة');
  });
}
