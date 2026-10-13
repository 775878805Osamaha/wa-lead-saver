package com.example.util

import com.example.data.model.CaptureEvaluation
import com.example.data.model.ExcludeCategory

/**
 * Deterministic 3-tier capture review engine.
 * Never uses random confidence or guesses. Every decision is based on verified notification data.
 */
object CaptureReviewEngine {

    /**
     * Evaluates incoming notification metadata and produces zero or more CaptureEvaluation results.
     */
    fun evaluateNotification(
        isGroup: Boolean,
        ignoreGroupMessages: Boolean,
        title: String,
        text: String,
        subText: String,
        bigText: String,
        conversationTitle: String,
        source: String,
        defaultCountryCode: String
    ): List<CaptureEvaluation> {
        // 1. Group Notification check
        if (isGroup) {
            if (ignoreGroupMessages) {
                val groupName = conversationTitle.ifBlank { title }.ifBlank { "مجموعة واتساب" }
                return listOf(
                    CaptureEvaluation.Excluded(
                        reason = "إشعار مجموعة مستبعد لحماية جهات الاتصال",
                        source = source,
                        candidatePreview = groupName,
                        category = ExcludeCategory.GROUP_NOTIFICATION
                    )
                )
            } else {
                // User enabled group monitoring: Only extract sender prefix if it's an unsaved number
                val textCandidate = if (text.contains(":")) text.substringBefore(":") else ""
                val bigTextCandidate = if (bigText.contains(":")) bigText.substringBefore(":") else ""
                val senderPrefix = when {
                    textCandidate.isNotBlank() -> textCandidate
                    bigTextCandidate.isNotBlank() -> bigTextCandidate
                    else -> ""
                }.trim()

                val hasLetters = senderPrefix.any { it.isLetter() }
                if (senderPrefix.isNotBlank() && senderPrefix != conversationTitle && !hasLetters) {
                    val candidateNumbers = PhoneNumberHelper.extractPhoneNumbers(senderPrefix, defaultCountryCode)
                    if (candidateNumbers.isNotEmpty()) {
                        return candidateNumbers.distinct().map { num ->
                            val normalized = PhoneNumberHelper.normalize(num, defaultCountryCode)
                            CaptureEvaluation.NeedsReview(
                                rawNumber = num,
                                normalizedNumber = normalized,
                                senderTitle = senderPrefix,
                                source = source,
                                reason = "رقم مستخرج من عضو مجموعة واتساب (يحتاج لتأكيد)"
                            )
                        }
                    }
                }
                return listOf(
                    CaptureEvaluation.Excluded(
                        reason = "إشعار مجموعة بدون مرسل ذي رقم هاتف ظاهر",
                        source = source,
                        candidatePreview = conversationTitle.ifBlank { title },
                        category = ExcludeCategory.INSUFFICIENT_DATA
                    )
                )
            }
        }

        // 2. Direct 1-to-1 Chat rules:
        val cleanTitle = title.trim()
        val hasLettersInTitle = cleanTitle.any { it.isLetter() }

        // Check if title itself is a phone number (typical WhatsApp notification from unsaved sender)
        if (cleanTitle.isNotBlank()) {
            val titleNumbers = PhoneNumberHelper.extractPhoneNumbers(cleanTitle, defaultCountryCode)
            if (titleNumbers.isNotEmpty()) {
                val candidate = titleNumbers.first()
                val normalized = PhoneNumberHelper.normalize(candidate, defaultCountryCode)
                if (PhoneNumberHelper.isValidPhoneNumber(normalized)) {
                    val results = mutableListOf<CaptureEvaluation>()
                    results.add(
                        CaptureEvaluation.Confirmed(
                            rawNumber = candidate,
                            normalizedNumber = normalized,
                            senderTitle = cleanTitle,
                            source = source,
                            reason = "محادثة فردية مباشرة مع رقم هاتف جديد"
                        )
                    )

                    // Also check if message body has ADDITIONAL numbers shared by the sender
                    val bodyContent = "$text $bigText".trim()
                    if (bodyContent.isNotBlank()) {
                        val bodyNumbers = PhoneNumberHelper.extractPhoneNumbers(bodyContent, defaultCountryCode)
                        val additionalNumbers = bodyNumbers.filter {
                            val norm = PhoneNumberHelper.normalize(it, defaultCountryCode)
                            norm != normalized && PhoneNumberHelper.isValidPhoneNumber(norm)
                        }.distinct()

                        for (rawNum in additionalNumbers) {
                            val norm = PhoneNumberHelper.normalize(rawNum, defaultCountryCode)
                            results.add(
                                CaptureEvaluation.NeedsReview(
                                    rawNumber = rawNum,
                                    normalizedNumber = norm,
                                    senderTitle = cleanTitle,
                                    source = source,
                                    reason = "رقم إضافي مذكور داخل نص المحادثة (يحتاج لمراجعة بشرية لتجنب الحفظ التلقائي)"
                                )
                            )
                        }
                    }

                    return results
                }
            }
        }

        // Title has contact name or no phone in title: Check message body for shared phone numbers
        val bodyContent = "$text $bigText".trim()
        if (bodyContent.isNotBlank()) {
            val bodyNumbers = PhoneNumberHelper.extractPhoneNumbers(bodyContent, defaultCountryCode)
            val validBodyNumbers = bodyNumbers.filter {
                val norm = PhoneNumberHelper.normalize(it, defaultCountryCode)
                PhoneNumberHelper.isValidPhoneNumber(norm)
            }.distinct()

            if (validBodyNumbers.isNotEmpty()) {
                val reasonMsg = if (validBodyNumbers.size > 1) {
                    "أرقام متعددة واردة في نص الرسالة (تتطلب مراجعة بشرية لتحديد رقم العميل وتجنب الحفظ التلقائي)"
                } else {
                    "رقم مذكور داخل نص المحادثة (يحتاج لمراجعة وتأكيد)"
                }
                // Mentioned inside message text: Marked as NEEDS_REVIEW so it is not auto-saved without confirmation
                return validBodyNumbers.map { rawNum ->
                    val normalized = PhoneNumberHelper.normalize(rawNum, defaultCountryCode)
                    CaptureEvaluation.NeedsReview(
                        rawNumber = rawNum,
                        normalizedNumber = normalized,
                        senderTitle = cleanTitle.ifBlank { "محادثة فردية" },
                        source = source,
                        reason = reasonMsg
                    )
                }
            }
        }

        // No numbers found or only contact name / media notification
        return listOf(
            CaptureEvaluation.Excluded(
                reason = "إشعار فردي بدون رقم هاتف متاح (جهة اتصال مسجلة أو رسالة وسائط)",
                source = source,
                candidatePreview = cleanTitle.ifBlank { "محادثة واتساب" },
                category = ExcludeCategory.INSUFFICIENT_DATA
            )
        )
    }

    /**
     * Determines whether a raw candidate number is a false positive and returns exclusion details if so.
     */
    fun checkCandidateExclusion(candidate: String, textContext: String = ""): Pair<Boolean, String?> {
        val digits = candidate.filter { it.isDigit() }
        if (PhoneNumberHelper.isYear(candidate)) {
            return true to "تاريخ أو سنة تقويمية وليس رقم هاتف"
        }
        if (digits.length < 7) {
            return true to "رقم قصير جداً وغير صالح كجهة اتصال"
        }
        if (digits.length > 16) {
            return true to "رقم طويل جداً ويتجاوز المعايير الدولية"
        }
        if (PhoneNumberHelper.isDummyNumber(digits)) {
            return true to "رقم وهمي متسلسل أو مكرر الخانات"
        }
        return false to null
    }
}
