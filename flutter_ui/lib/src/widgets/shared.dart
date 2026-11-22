import 'package:flutter/material.dart';
import '../theme/wa_theme.dart';

class WaSectionHeader extends StatelessWidget {
  const WaSectionHeader({super.key, required this.title, this.subtitle});
  final String title;
  final String? subtitle;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: Theme.of(context).textTheme.titleLarge?.copyWith(
              fontWeight: FontWeight.bold,
              color: WaPalette.ink,
            )),
            if (subtitle != null)
              Padding(
                padding: const EdgeInsets.only(top: 6),
                child: Text(subtitle!, style: const TextStyle(
                  color: WaPalette.muted,
                )),
              ),
          ],
        ),
      );
}

class WaFeedback extends StatelessWidget {
  const WaFeedback({
    super.key,
    required this.message,
    this.onRetry,
    this.loading = false,
  });
  final String message;
  final VoidCallback? onRetry;
  final bool loading;

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (loading)
                const CircularProgressIndicator()
              else
                const Icon(Icons.info_outline, color: WaPalette.muted, size: 34),
              const SizedBox(height: 12),
              Text(message, textAlign: TextAlign.center,
                style: const TextStyle(color: WaPalette.muted)),
              if (onRetry != null) ...[
                const SizedBox(height: 12),
                FilledButton.icon(
                  onPressed: onRetry,
                  icon: const Icon(Icons.refresh),
                  label: const Text('إعادة المحاولة'),
                ),
              ],
            ],
          ),
        ),
      );
}

class WaCounterCard extends StatelessWidget {
  const WaCounterCard({
    super.key,
    required this.label,
    required this.value,
    required this.icon,
    this.onTap,
    this.horizontal = false,
  });
  final String label;
  final String value;
  final IconData icon;
  final VoidCallback? onTap;
  final bool horizontal;

  @override
  Widget build(BuildContext context) => Card(
    child: InkWell(
      borderRadius: BorderRadius.circular(20),
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: horizontal
          ? Row(
              children: [
                Icon(icon, color: WaPalette.green),
                const SizedBox(width: 14),
                Expanded(
                  child: Text(label, style: const TextStyle(
                    color: WaPalette.muted,
                    fontWeight: FontWeight.w600,
                  )),
                ),
                Text(value, style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                  color: WaPalette.ink, fontWeight: FontWeight.bold,
                )),
              ],
            )
          : Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: WaPalette.green),
            const SizedBox(height: 16),
            Text(value, style: Theme.of(context).textTheme.headlineMedium?.copyWith(
              color: WaPalette.ink, fontWeight: FontWeight.bold,
            )),
            const SizedBox(height: 5),
            Text(label, style: const TextStyle(color: WaPalette.muted)),
          ],
        ),
      ),
    ),
  );
}
