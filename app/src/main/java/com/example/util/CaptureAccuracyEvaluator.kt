package com.example.util

import com.example.data.model.CaptureDecision
import com.example.data.model.CaptureEvaluation
import com.example.data.model.ExcludeCategory

/**
 * Benchmark test case representing a real-world labeled WhatsApp notification scenario
 * with a strictly known ground-truth outcome.
 */
data class LabeledTestCase(
    val id: String,
    val title: String,
    val text: String,
    val subText: String = "",
    val bigText: String = "",
    val isGroup: Boolean = false,
    val ignoreGroupMessages: Boolean = true,
    val countryCode: String = "+967",
    val expectedDecision: CaptureDecision,
    val expectedCategory: ExcludeCategory? = null,
    val expectedNormalizedNumber: String? = null,
    val description: String
)

/**
 * Result of evaluating labeled benchmark cases with ground-truth validation.
 */
data class BenchmarkEvaluationResult(
    val totalCases: Int,
    val passedCases: Int,
    val failedCases: Int,
    val accuracyPercentage: Int,
    val evaluatedAt: Long = System.currentTimeMillis(),
    val caseDetails: List<String> = emptyList()
)

object CaptureAccuracyEvaluator {

    /**
     * Curated, verified benchmark suite of labeled test cases covering all realistic WhatsApp notification types.
     * Contains zero dummy or random data. Every case represents an explicit deterministic rule.
     */
    val BENCHMARK_CASES = listOf(
        LabeledTestCase(
            id = "TC-01",
            title = "+967771234567",
            text = "السلام عليكم، هل المنتج متوفر لديكم؟",
            expectedDecision = CaptureDecision.CONFIRMED,
            expectedNormalizedNumber = "+967771234567",
            countryCode = "+967",
            description = "محادثة فردية مباشرة مع رقم يمني جديد"
        ),
        LabeledTestCase(
            id = "TC-02",
            title = "+966501234567",
            text = "مرحباً بكم، أود الاستفسار عن تفاصيل الحجز",
            expectedDecision = CaptureDecision.CONFIRMED,
            expectedNormalizedNumber = "+966501234567",
            countryCode = "+966",
            description = "محادثة فردية مباشرة مع رقم سعودي جديد"
        ),
        LabeledTestCase(
            id = "TC-03",
            title = "+201012345678",
            text = "مساء الخير، لو سمحت عايز أعرف الأسعار",
            expectedDecision = CaptureDecision.CONFIRMED,
            expectedNormalizedNumber = "+201012345678",
            countryCode = "+20",
            description = "محادثة فردية مباشرة مع رقم مصري جديد"
        ),
        LabeledTestCase(
            id = "TC-04",
            title = "+971501234567",
            text = "مرحبا، هل يوجد توصيل إلى دبي؟",
            expectedDecision = CaptureDecision.CONFIRMED,
            expectedNormalizedNumber = "+971501234567",
            countryCode = "+971",
            description = "محادثة فردية مباشرة مع رقم إماراتي جديد"
        ),
        LabeledTestCase(
            id = "TC-05",
            title = "+96599112233",
            text = "السلام عليكم ورحمة الله",
            expectedDecision = CaptureDecision.CONFIRMED,
            expectedNormalizedNumber = "+96599112233",
            countryCode = "+965",
            description = "محادثة فردية مباشرة مع رقم كويتي جديد"
        ),
        LabeledTestCase(
            id = "TC-06",
            title = "+96891234567",
            text = "أرغب في طلب الخدمة من سلطنة عمان",
            expectedDecision = CaptureDecision.CONFIRMED,
            expectedNormalizedNumber = "+96891234567",
            countryCode = "+968",
            description = "محادثة فردية مباشرة مع رقم عماني جديد"
        ),
        LabeledTestCase(
            id = "TC-07",
            title = "مجموعة تجار الأجهزة",
            text = "+967770001122: تم إرسال البضاعة",
            isGroup = true,
            ignoreGroupMessages = true,
            expectedDecision = CaptureDecision.EXCLUDED,
            expectedCategory = ExcludeCategory.GROUP_NOTIFICATION,
            countryCode = "+967",
            description = "إشعار مجموعة مستبعد بالكامل لحماية جهات الاتصال"
        ),
        LabeledTestCase(
            id = "TC-08",
            title = "قروب العقارات والاستثمار",
            text = "+966501112233: عمارة للبيع في شمال الرياض",
            isGroup = true,
            ignoreGroupMessages = false,
            expectedDecision = CaptureDecision.NEEDS_REVIEW,
            expectedNormalizedNumber = "+966501112233",
            countryCode = "+966",
            description = "إشعار مجموعة مع تفعيل المراقبة (يوضع في قائمة المراجعة ولا يحفظ تلقائياً)"
        ),
        LabeledTestCase(
            id = "TC-09",
            title = "خدمة العملاء",
            text = "سعر القطعة هو 3500 ريال يمني مع التوصيل",
            isGroup = false,
            expectedDecision = CaptureDecision.EXCLUDED,
            expectedCategory = ExcludeCategory.INSUFFICIENT_DATA,
            countryCode = "+967",
            description = "رسالة تتضمن سعراً وعملة نقدية فقط دون رقم هاتف (يتم تجاهل السعر)"
        ),
        LabeledTestCase(
            id = "TC-10",
            title = "المتجر الإلكتروني",
            text = "The total price for your cart is $150 USD",
            isGroup = false,
            expectedDecision = CaptureDecision.EXCLUDED,
            expectedCategory = ExcludeCategory.INSUFFICIENT_DATA,
            countryCode = "+966",
            description = "رسالة تتضمن رمز عملة أجنبية ($) دون رقم هاتف"
        ),
        LabeledTestCase(
            id = "TC-11",
            title = "نظام الشحن",
            text = "رقم الفاتورة #847291 ورقم الشحنة #992810 قيد المتابعة",
            isGroup = false,
            expectedDecision = CaptureDecision.EXCLUDED,
            expectedCategory = ExcludeCategory.INSUFFICIENT_DATA,
            countryCode = "+966",
            description = "رسالة تتضمن أرقام فواتير ورموز تتبع بدون أرقام هواتف"
        ),
        LabeledTestCase(
            id = "TC-12",
            title = "معرض التقنية",
            text = "نرحب بكم في فعاليات موسم عام 2026",
            isGroup = false,
            expectedDecision = CaptureDecision.EXCLUDED,
            expectedCategory = ExcludeCategory.INSUFFICIENT_DATA,
            countryCode = "+966",
            description = "رسالة تحتوي على تاريخ أو سنة تقويمية وليست رقم اتصال"
        ),
        LabeledTestCase(
            id = "TC-13",
            title = "رقم تجريبي",
            text = "يرجى اختبار الرقم 0000000000",
            isGroup = false,
            expectedDecision = CaptureDecision.EXCLUDED,
            expectedCategory = ExcludeCategory.INSUFFICIENT_DATA,
            countryCode = "+967",
            description = "أرقام مكررة أو وهمية مستبعدة"
        ),
        LabeledTestCase(
            id = "TC-14",
            title = "رمز التفعيل",
            text = "رمز التحقق الخاص بك هو 54321",
            isGroup = false,
            expectedDecision = CaptureDecision.EXCLUDED,
            expectedCategory = ExcludeCategory.INSUFFICIENT_DATA,
            countryCode = "+967",
            description = "أرقام قصيرة جداً مستبعدة"
        ),
        LabeledTestCase(
            id = "TC-15",
            title = "أحمد المنسق",
            text = "هذا رقم العميل الجديد للتواصل: 771234567 يرجى الاتصال به",
            isGroup = false,
            expectedDecision = CaptureDecision.NEEDS_REVIEW,
            expectedNormalizedNumber = "+967771234567",
            countryCode = "+967",
            description = "رقم هاتف مذكور داخل متن الرسالة (يحتاج مراجعة ولا يحفظ تلقائياً)"
        ),
        LabeledTestCase(
            id = "TC-16",
            title = "خالد عبدالله",
            text = "📷 صورة",
            isGroup = false,
            expectedDecision = CaptureDecision.EXCLUDED,
            expectedCategory = ExcludeCategory.INSUFFICIENT_DATA,
            countryCode = "+966",
            description = "إشعار وسائط من جهة اتصال مسجلة بدون رقم هاتف ظاهر"
        ),
        LabeledTestCase(
            id = "TC-17",
            title = "معرض السيارات",
            text = "السعر 45000 ريال، للتواصل اتصل على 0501234567",
            isGroup = false,
            expectedDecision = CaptureDecision.NEEDS_REVIEW,
            expectedNormalizedNumber = "+966501234567",
            countryCode = "+966",
            description = "رسالة تجمع بين سعر ورقم هاتف (يتم تجاهل السعر واستخراج الرقم للمراجعة)"
        ),
        LabeledTestCase(
            id = "TC-18",
            title = "771234567",
            text = "مرحبا هل الحجز متاح؟",
            isGroup = false,
            expectedDecision = CaptureDecision.CONFIRMED,
            expectedNormalizedNumber = "+967771234567",
            countryCode = "+967",
            description = "رقم عميل يمني بصيغة محلية (77xxxxxxx) في عنوان المحادثة"
        ),
        LabeledTestCase(
            id = "TC-19",
            title = "00967731234567",
            text = "السلام عليكم ورحمة الله",
            isGroup = false,
            expectedDecision = CaptureDecision.CONFIRMED,
            expectedNormalizedNumber = "+967731234567",
            countryCode = "+967",
            description = "رقم عميل يمني يبدأ بـ 00967 في عنوان المحادثة"
        ),
        LabeledTestCase(
            id = "TC-20",
            title = "0711234567",
            text = "طلب استفسار عن المنتج",
            isGroup = false,
            expectedDecision = CaptureDecision.CONFIRMED,
            expectedNormalizedNumber = "+967711234567",
            countryCode = "+967",
            description = "رقم عميل يمني يبدأ بصفر محلي (071xxxxxxx)"
        ),
        LabeledTestCase(
            id = "TC-21",
            title = "خدمة العملاء",
            text = "أرقام التواصل معنا: 771112233 وأيضاً 733334455",
            isGroup = false,
            expectedDecision = CaptureDecision.NEEDS_REVIEW,
            expectedNormalizedNumber = "+967771112233",
            countryCode = "+967",
            description = "إشعار يحتوي على أكثر من رقم هاتف (يتم إرساله للمراجعة لمنع الحفظ التلقائي)"
        ),
        LabeledTestCase(
            id = "TC-22",
            title = "متجر العطور",
            text = "سعر العطر 12000 ريال 771234567",
            isGroup = false,
            expectedDecision = CaptureDecision.NEEDS_REVIEW,
            expectedNormalizedNumber = "+967771234567",
            countryCode = "+967",
            description = "رقم هاتف صالح بجوار سعر وعملة بدون كلمة اتصال (يتم استخراج الرقم واستبعاد السعر)"
        )
    )

    /**
     * Executes the labeled ground-truth benchmark suite against CaptureReviewEngine.
     * Evaluates accuracy strictly against known expected outcomes.
     */
    fun runBenchmark(
        cases: List<LabeledTestCase> = BENCHMARK_CASES
    ): BenchmarkEvaluationResult {
        var passed = 0
        var failed = 0
        val details = mutableListOf<String>()

        for (tc in cases) {
            val evaluations = CaptureReviewEngine.evaluateNotification(
                isGroup = tc.isGroup,
                ignoreGroupMessages = tc.ignoreGroupMessages,
                title = tc.title,
                text = tc.text,
                subText = tc.subText,
                bigText = tc.bigText,
                conversationTitle = tc.title,
                source = "WhatsApp",
                defaultCountryCode = tc.countryCode
            )

            val isMatch = when (tc.expectedDecision) {
                CaptureDecision.CONFIRMED -> {
                    evaluations.any { eval ->
                        eval is CaptureEvaluation.Confirmed &&
                                (tc.expectedNormalizedNumber == null || eval.normalizedNumber == tc.expectedNormalizedNumber)
                    }
                }
                CaptureDecision.NEEDS_REVIEW -> {
                    evaluations.any { eval ->
                        eval is CaptureEvaluation.NeedsReview &&
                                (tc.expectedNormalizedNumber == null || eval.normalizedNumber == tc.expectedNormalizedNumber)
                    }
                }
                CaptureDecision.EXCLUDED -> {
                    evaluations.all { it is CaptureEvaluation.Excluded } &&
                            (tc.expectedCategory == null || evaluations.any { (it as CaptureEvaluation.Excluded).category == tc.expectedCategory })
                }
            }

            if (isMatch) {
                passed++
                details.add("✓ [${tc.id}] ${tc.description}: مطابقة صحيحة")
            } else {
                failed++
                details.add("✗ [${tc.id}] ${tc.description}: عدم مطابقة مع النتيجة المعروفة")
            }
        }

        val total = cases.size
        val accuracy = if (total > 0) ((passed * 100) / total).coerceIn(0, 100) else 0

        return BenchmarkEvaluationResult(
            totalCases = total,
            passedCases = passed,
            failedCases = failed,
            accuracyPercentage = accuracy,
            caseDetails = details
        )
    }
}
