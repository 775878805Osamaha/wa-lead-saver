package com.example.data.model

/**
 * 3-tier review policy decision.
 */
enum class CaptureDecision {
    CONFIRMED,      // مؤكد: بيانات كافية لتحديد محادثة فردية ورقم هاتف صالح
    NEEDS_REVIEW,   // يحتاج إلى مراجعة: رقم محتمل لا يمكن إثبات أنه رقم عميل فردي
    EXCLUDED        // مستبعد: إشعار مجموعة، رقم غير صالح، سعر، فاتورة، رقم محظور
}

/**
 * Category of exclusion for detailed diagnostic reporting.
 */
enum class ExcludeCategory {
    GROUP_NOTIFICATION,
    PRICE_OR_CURRENCY,
    ORDER_OR_TRACKING_CODE,
    DATE_OR_YEAR,
    DUMMY_OR_SEQUENCE,
    INVALID_FORMAT,
    SYSTEM_NOTIFICATION,
    INSUFFICIENT_DATA,
    DUPLICATE,
    BLOCKED_PATTERN
}

/**
 * Result of the deterministic 3-tier capture review engine.
 */
sealed class CaptureEvaluation {
    data class Confirmed(
        val rawNumber: String,
        val normalizedNumber: String,
        val senderTitle: String,
        val source: String,
        val reason: String = "محادثة فردية مؤكدة ورقم صالح"
    ) : CaptureEvaluation()

    data class NeedsReview(
        val rawNumber: String,
        val normalizedNumber: String,
        val senderTitle: String,
        val source: String,
        val reason: String
    ) : CaptureEvaluation()

    data class Excluded(
        val reason: String,
        val source: String,
        val candidatePreview: String = "",
        val category: ExcludeCategory
    ) : CaptureEvaluation()
}

/**
 * Real-time capture accuracy statistics computed strictly from labeled benchmark evaluations
 * and verified classified cases with known ground truth.
 */
data class CaptureStats(
    val totalReceived: Int = 0,
    val totalAnalyzed: Int = 0,
    val candidatesCount: Int = 0,
    val savedCount: Int = 0,
    val duplicatesCount: Int = 0,
    val groupExcludedCount: Int = 0,
    val invalidOrExcludedCount: Int = 0,
    val needsReviewCount: Int = 0,
    val accuracyPercentage: Int? = null,
    val evaluatedCasesCount: Int = 0,
    val passedCasesCount: Int = 0,
    val evaluationMethodNote: String = ""
)

/**
 * Diagnostic Audit Log filter options.
 */
enum class AuditFilter(val title: String) {
    ALL("الكل"),
    SAVED("المحفوظة"),
    NEEDS_REVIEW("تحتاج مراجعة"),
    GROUP_EXCLUDED("مجموعات مستبعدة"),
    DUPLICATE("مكررة"),
    EXCLUDED("غير صالحة ومستبعدة")
}
