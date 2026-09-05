package com.example.util

/**
 * Confidence level assigned to an extracted phone candidate.
 * - HIGH: Fully verified candidate, e.g. clean sender title or direct phone number matching carrier structure.
 *         Only HIGH is eligible for Auto-Save.
 * - MEDIUM: Extracted from explicit contact cues in text or non-standard format. Requires manual review in Queue.
 * - LOW: Ambiguous, short, or low assurance. Rejected and never added to Queue.
 */
enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Source metadata indicating where the candidate came from.
 */
enum class CandidateSourceType {
    NOTIFICATION_TITLE_UNSAVED_SENDER,
    NOTIFICATION_CONVERSATION_METADATA,
    NOTIFICATION_BODY_EXPLICIT_PHONE,
    NOTIFICATION_BODY_UNVERIFIED,
    MANUAL_INPUT_OR_SCAN,
    GROUP_NOTIFICATION
}

/**
 * Result of phone validation on a single candidate string.
 */
data class PhoneValidationResult(
    val isValid: Boolean,
    val normalizedNumber: String = "",
    val confidence: ConfidenceLevel = ConfidenceLevel.LOW,
    val rejectionReason: String = "",
    val detectedCountryCode: String = "",
    val carrierHint: String = ""
)

/**
 * Comprehensive result of parsing an entire incoming notification.
 */
data class NotificationParseResult(
    val isGroup: Boolean = false,
    val sourceLabel: String = "WhatsApp",
    val candidateFound: Boolean = false,
    val candidate: String = "",
    val normalizedNumber: String = "",
    val confidence: ConfidenceLevel = ConfidenceLevel.LOW,
    val sourceType: CandidateSourceType = CandidateSourceType.NOTIFICATION_BODY_UNVERIFIED,
    val isAccepted: Boolean = false, // true only if confidence == HIGH || confidence == MEDIUM
    val rejectionReason: String = "",
    val debugDetails: String = ""
)

object PhoneNumberValidator {

    // Common words indicating prices, currencies, counts, order IDs, timestamps, or years in Arabic and English
    private val PRICE_OR_COUNT_KEYWORDS = listOf(
        "سعر", "السعر", "ريال", "دولار", "ر.ي", "$", "usd", "yer", "sar",
        "رسالة", "رسائل", "messages", "new messages", "رسالة جديدة",
        "طلب", "الطلب", "order", "id", "موديل", "model", "كود", "code", "pin",
        "عام", "سنة", "year", "date", "تاريخ", "الساعة", "دقيقة", "minute"
    )

    // Explicit phone contact cues in Arabic and English that indicate a deliberate phone share in message text
    private val EXPLICIT_PHONE_CUES = listOf(
        "تواصل معي", "تواصل معنا", "تواصل مع", "للتواصل", "اتصل بي", "اتصل بنا", "اتصل على", "رقمي", "رقم الهاتف", "رقم الواتس",
        "رقمه", "رقم", "واتساب", "واتس", "جوال", "هاتف", "call me", "contact", "whatsapp", "wa.me", "phone", "mobile", "tel"
    )

    /**
     * Converts Eastern Arabic-Indic numerals (٠١٢٣٤٥٦٧٨٩) and Persian numerals (۰۱۲۳۴۵۶۷۸۹) to standard ASCII digits.
     */
    fun convertArabicNumerals(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                '٠', '۰' -> sb.append('0')
                '١', '۱' -> sb.append('1')
                '٢', '۲' -> sb.append('2')
                '٣', '۳' -> sb.append('3')
                '٤', '۴' -> sb.append('4')
                '٥', '۵' -> sb.append('5')
                '٦', '۶' -> sb.append('6')
                '٧', '۷' -> sb.append('7')
                '٨', '۸' -> sb.append('8')
                '٩', '۹' -> sb.append('9')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Cleans up raw phone strings by converting numerals and stripping formatting characters.
     */
    fun cleanRawDigits(raw: String): String {
        val ascii = convertArabicNumerals(raw.trim())
        val hasLeadingPlus = ascii.startsWith("+")
        val digitsOnly = ascii.filter { it.isDigit() }
        return if (hasLeadingPlus) "+$digitsOnly" else digitsOnly
    }

    /**
     * Validates a candidate phone number string according to strict rules:
     * - Discards prices, counters, years (e.g. 2020..2035), short numbers (<7 digits), excessively long numbers (>15 digits)
     * - Performs Yemen (+967) validation and proper prefix mapping
     * - Returns a PhoneValidationResult with confidence and rejection reasons
     */
    fun validateCandidate(
        rawCandidate: String,
        defaultCountryCode: String = "+967",
        contextText: String = "",
        isExplicitTitle: Boolean = false
    ): PhoneValidationResult {
        val trimmed = rawCandidate.trim()
        if (trimmed.isEmpty()) {
            return PhoneValidationResult(isValid = false, rejectionReason = "Empty number candidate")
        }

        // Convert Arabic digits to ASCII
        val converted = convertArabicNumerals(trimmed)
        val cleanCountry = if (defaultCountryCode.startsWith("+")) defaultCountryCode else "+$defaultCountryCode"
        val countryDigits = cleanCountry.removePrefix("+")

        // Reject if matches counter pattern like "2 new messages" or "3 رسائل"
        if (converted.contains("message", ignoreCase = true) || converted.contains("رسال", ignoreCase = true)) {
            return PhoneValidationResult(isValid = false, rejectionReason = "Matches notification counter text")
        }

        val digitsOnly = converted.filter { it.isDigit() }
        val hasPlus = converted.startsWith("+") || converted.startsWith("00")

        // 1. Length check: Phone numbers must be between 7 and 15 digits
        if (digitsOnly.length < 7) {
            return PhoneValidationResult(
                isValid = false,
                rejectionReason = "Number too short (${digitsOnly.length} digits, minimum 7 required)"
            )
        }

        if (digitsOnly.length > 15) {
            return PhoneValidationResult(
                isValid = false,
                rejectionReason = "Number too long (${digitsOnly.length} digits, maximum 15 allowed)"
            )
        }

        // 2. Reject years (e.g. 1990..2040)
        val asLong = digitsOnly.toLongOrNull()
        if (asLong != null && digitsOnly.length == 4 && asLong in 1900..2050) {
            return PhoneValidationResult(isValid = false, rejectionReason = "Candidate is a calendar year ($asLong)")
        }

        // 3. Reject round price/currency patterns e.g. 50000, 10000, 25000 if without phone formatting
        if (digitsOnly.length in 5..6 && (digitsOnly.endsWith("000") || digitsOnly.endsWith("0000"))) {
            return PhoneValidationResult(isValid = false, rejectionReason = "Candidate matches currency price amount ($digitsOnly)")
        }

        // 4. Reject all-repeating digits like 11111111, 00000000, 99999999
        if (digitsOnly.all { it == digitsOnly[0] }) {
            return PhoneValidationResult(isValid = false, rejectionReason = "Candidate contains only identical repeated digits")
        }

        // 5. Yemen (+967) validation:
        // Yemen mobile numbers are 9 digits total:
        // Starts with 70, 71, 73, 77, 78 (Mobile GSM/CDMA/VoLTE)
        // or landline: 1 (Sanaa), 2 (Aden), 3 (Taiz/Hodeidah), 4 (Ibb), 5 (Mukalla), 6 (Dhamar), etc. (usually 7-8 digits)
        val isYemenCountry = cleanCountry == "+967" || digitsOnly.startsWith("967")

        var normalized = ""
        var carrierHint = ""
        var confidence = ConfidenceLevel.MEDIUM

        if (isYemenCountry) {
            var localDigits = digitsOnly
            if (localDigits.startsWith("967")) {
                localDigits = localDigits.removePrefix("967")
            } else if (localDigits.startsWith("0")) {
                localDigits = localDigits.removePrefix("0")
            }

            // Mobile Yemen numbers: exactly 9 digits starting with 7
            val isYemenMobilePrefix = localDigits.length == 9 && (
                    localDigits.startsWith("77") || // Yemen Mobile (CDMA/LTE)
                            localDigits.startsWith("73") || // YOU (formerly MTN)
                            localDigits.startsWith("71") || // Sabafon
                            localDigits.startsWith("70") || // Y Telecom
                            localDigits.startsWith("78")    // Yemen Mobile 4G/VoLTE
                    )

            val isYemenLandline = (localDigits.length in 7..8) &&
                    (localDigits.startsWith("1") || localDigits.startsWith("2") || localDigits.startsWith("3") ||
                            localDigits.startsWith("4") || localDigits.startsWith("5") || localDigits.startsWith("6"))

            if (isYemenMobilePrefix) {
                normalized = "+967$localDigits"
                carrierHint = when {
                    localDigits.startsWith("77") || localDigits.startsWith("78") -> "Yemen Mobile"
                    localDigits.startsWith("73") -> "YOU (MTN)"
                    localDigits.startsWith("71") -> "Sabafon"
                    localDigits.startsWith("70") -> "Y Telecom"
                    else -> "Yemen GSM"
                }
                confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            } else if (isYemenLandline) {
                normalized = "+967$localDigits"
                carrierHint = "Yemen Fixed Line"
                confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            } else {
                // If it claims to be Yemen (+967) or starts with country code or has +:
                if (digitsOnly.startsWith("967") || hasPlus) {
                    if (digitsOnly.length in 9..15) {
                        normalized = if (digitsOnly.startsWith("967")) "+$digitsOnly" else "+967$localDigits"
                        confidence = ConfidenceLevel.MEDIUM
                    } else {
                        return PhoneValidationResult(
                            isValid = false,
                            rejectionReason = "Invalid Yemen number structure: length ${digitsOnly.length} is not a valid Yemen mobile or landline"
                        )
                    }
                } else if (digitsOnly.length in 7..10) {
                    // Local number with non-standard prefix
                    return PhoneValidationResult(
                        isValid = false,
                        rejectionReason = "Invalid Yemen local prefix: '${localDigits.take(2)}' does not match 70, 71, 73, 77, 78 or landline"
                    )
                } else {
                    return PhoneValidationResult(
                        isValid = false,
                        rejectionReason = "Number length ${digitsOnly.length} does not match Yemen mobile specification"
                    )
                }
            }
        } else {
            // General International validation
            if (hasPlus) {
                normalized = "+$digitsOnly"
                confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            } else if (digitsOnly.startsWith(countryDigits)) {
                normalized = "+$digitsOnly"
                confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            } else if (digitsOnly.startsWith("0")) {
                normalized = "$cleanCountry${digitsOnly.removePrefix("0")}"
                confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            } else {
                normalized = "$cleanCountry$digitsOnly"
                confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            }
        }

        // If contextText contains strong currency/price clues near the candidate, downgrade or reject
        if (contextText.isNotBlank()) {
            val lowerContext = contextText.lowercase()
            for (keyword in PRICE_OR_COUNT_KEYWORDS) {
                if (lowerContext.contains(keyword)) {
                    // If the candidate appears immediately after a price word, e.g. "السعر 50000"
                    val pattern = Regex("""(?i)$keyword\s*[:\s\-]*$digitsOnly""")
                    if (pattern.find(lowerContext) != null) {
                        return PhoneValidationResult(
                            isValid = false,
                            rejectionReason = "Candidate follows price/counter keyword '$keyword'"
                        )
                    }
                }
            }
        }

        return PhoneValidationResult(
            isValid = true,
            normalizedNumber = normalized,
            confidence = confidence,
            detectedCountryCode = cleanCountry,
            carrierHint = carrierHint
        )
    }

    /**
     * Checks if a given text contains explicit contact sharing cues (e.g. "تواصل معي 771234567").
     */
    fun hasExplicitContactCue(text: String): Boolean {
        val lower = text.lowercase()
        return EXPLICIT_PHONE_CUES.any { lower.contains(it) }
    }

    /**
     * Determines whether notification metadata or titles indicate a Group conversation.
     */
    fun isGroupNotification(
        title: String,
        text: String,
        subText: String = "",
        isGroupConversation: Boolean = false
    ): Boolean {
        if (isGroupConversation) return true

        // Group chats usually have patterns in text like: "ContactName: message text"
        // or title has typical group symbols or subText has group participants count
        if (subText.contains("group", ignoreCase = true) || subText.contains("مجموعة", ignoreCase = true)) {
            return true
        }

        // WhatsApp group notification title is the Group Name, and text starts with "Sender: message"
        // e.g. "محمد: السعر 50000" or "Ali: hello"
        val senderPrefixRegex = Regex("""^[\p{L}\s\d_+.-]{1,30}\s*:\s*.+""")
        val textHasSenderPrefix = senderPrefixRegex.matches(text.trim())

        // If title does not look like a phone number at all, and text has "Sender: ...", it's almost certainly a group!
        val titleLooksLikePhone = title.trim().matches(Regex("""^[+]?[\d\s\-()]{7,20}$"""))
        if (!titleLooksLikePhone && textHasSenderPrefix) {
            return true
        }

        return false
    }

    /**
     * Master notification parser implementing strict parsing rules.
     * Rule Priority:
     * A. Phone number exposed directly in notification title if sender is unsaved (Title is "+967..." or "771...")
     * B. Group notification: NEVER extract numbers from message body or group title unless sender metadata is proven.
     * C. Message Body: NEVER blindly extract numbers from message body unless it has an explicit contact cue AND passes strict candidate validation.
     * D. Otherwise: Phone number unavailable.
     */
    fun parseNotification(
        packageName: String,
        title: String,
        text: String,
        subText: String = "",
        bigText: String = "",
        isGroupConversation: Boolean = false,
        defaultCountryCode: String = "+967"
    ): NotificationParseResult {
        val isBusiness = packageName == "com.whatsapp.w4b"
        val baseSource = if (isBusiness) "WhatsApp Business" else "WhatsApp"

        val cleanTitle = title.trim()
        val cleanText = text.trim()
        val cleanSubText = subText.trim()
        val cleanBigText = bigText.trim()

        val isGroup = isGroupNotification(cleanTitle, cleanText, cleanSubText, isGroupConversation)
        val sourceLabel = if (isGroup) "WhatsApp Group" else baseSource

        // 1. Group Rule:
        // "إذا كان الإشعار من Group Chat فلا تستخرج أي رقم من نص الرسالة أو اسم المجموعة باعتباره رقم العميل."
        if (isGroup) {
            // Check if sender identity in title itself is an unsaved phone number (rare for groups, but possible in 1:1 broadcast or if title is number)
            val titleValidation = validateCandidate(cleanTitle, defaultCountryCode, isExplicitTitle = true)
            if (titleValidation.isValid && titleValidation.confidence == ConfidenceLevel.HIGH) {
                return NotificationParseResult(
                    isGroup = true,
                    sourceLabel = sourceLabel,
                    candidateFound = true,
                    candidate = cleanTitle,
                    normalizedNumber = titleValidation.normalizedNumber,
                    confidence = ConfidenceLevel.HIGH,
                    sourceType = CandidateSourceType.GROUP_NOTIFICATION,
                    isAccepted = true,
                    debugDetails = "Group title was a verified phone number: ${titleValidation.carrierHint}"
                )
            }

            // In groups, text often has "Sender: message body".
            // Rule: Do NOT extract random numbers from group body.
            // Only if text contains an EXPLICIT phone contact cue ("تواصل معي على 771234567") AND valid candidate
            if (hasExplicitContactCue(cleanText)) {
                val candidateMatch = extractExplicitCandidateFromText(cleanText)
                if (candidateMatch != null) {
                    val bodyValidation = validateCandidate(candidateMatch, defaultCountryCode, cleanText, isExplicitTitle = false)
                    if (bodyValidation.isValid) {
                        return NotificationParseResult(
                            isGroup = true,
                            sourceLabel = sourceLabel,
                            candidateFound = true,
                            candidate = candidateMatch,
                            normalizedNumber = bodyValidation.normalizedNumber,
                            confidence = ConfidenceLevel.MEDIUM,
                            sourceType = CandidateSourceType.NOTIFICATION_BODY_EXPLICIT_PHONE,
                            isAccepted = true,
                            debugDetails = "Explicit contact cue in group text: '${bodyValidation.normalizedNumber}' (Review Queue)"
                        )
                    }
                }
            }

            // If not proved: Phone number unavailable
            return NotificationParseResult(
                isGroup = true,
                sourceLabel = sourceLabel,
                candidateFound = false,
                confidence = ConfidenceLevel.LOW,
                sourceType = CandidateSourceType.GROUP_NOTIFICATION,
                isAccepted = false,
                rejectionReason = "Phone number unavailable (Group chat notification without reliable sender phone)",
                debugDetails = "Group chat detected. Prevented body number extraction. Title='$cleanTitle'"
            )
        }

        // 2. Private 1:1 Chat: Priority A & C
        // WhatsApp sets Title as the unsaved phone number when receiving a message from an unknown sender!
        // e.g. Title: "+967771234567" or "0771234567" or "771234567"
        val titleCandidate = convertArabicNumerals(cleanTitle)
        val titleOnlyDigits = titleCandidate.filter { it.isDigit() }

        // If title consists mostly of digits or leading plus, test it as an explicit sender phone number
        val titleIsPhoneNumberForm = titleCandidate.startsWith("+") ||
                titleCandidate.startsWith("00") ||
                (titleOnlyDigits.length in 7..15 && titleCandidate.all { it.isDigit() || it.isWhitespace() || it == '-' || it == '+' || it == '(' || it == ')' })

        if (titleIsPhoneNumberForm) {
            val titleVal = validateCandidate(cleanTitle, defaultCountryCode, isExplicitTitle = true)
            if (titleVal.isValid) {
                return NotificationParseResult(
                    isGroup = false,
                    sourceLabel = sourceLabel,
                    candidateFound = true,
                    candidate = cleanTitle,
                    normalizedNumber = titleVal.normalizedNumber,
                    confidence = ConfidenceLevel.HIGH,
                    sourceType = CandidateSourceType.NOTIFICATION_TITLE_UNSAVED_SENDER,
                    isAccepted = true,
                    debugDetails = "Direct unsaved sender in notification title: ${titleVal.carrierHint}"
                )
            } else {
                return NotificationParseResult(
                    isGroup = false,
                    sourceLabel = sourceLabel,
                    candidateFound = false,
                    confidence = ConfidenceLevel.LOW,
                    sourceType = CandidateSourceType.NOTIFICATION_TITLE_UNSAVED_SENDER,
                    isAccepted = false,
                    rejectionReason = "Rejected invalid phone candidate in title: ${titleVal.rejectionReason}",
                    debugDetails = "Title='$cleanTitle'"
                )
            }
        }

        // 3. Title is a contact name or general text (e.g. "Ahmed", "Office", "2 new messages")
        // Check if message is a system notification (e.g. backup, web active, checking messages)
        val lowerFull = "$cleanTitle $cleanText $cleanSubText $cleanBigText".lowercase()
        if (lowerFull.contains("backup") ||
            lowerFull.contains("checking for new messages") ||
            lowerFull.contains("web is currently active") ||
            lowerFull.contains("new messages") ||
            lowerFull.contains("رسائل جديدة")
        ) {
            return NotificationParseResult(
                isGroup = false,
                sourceLabel = sourceLabel,
                candidateFound = false,
                confidence = ConfidenceLevel.LOW,
                sourceType = CandidateSourceType.NOTIFICATION_BODY_UNVERIFIED,
                isAccepted = false,
                rejectionReason = "WhatsApp system status / count notification",
                debugDetails = "System status notification ignored"
            )
        }

        // 4. Rule 1 & Rule 5:
        // "ممنوع اعتبار أي رقم داخل نص رسالة WhatsApp رقم هاتف تلقائيًا."
        // "لا تستخرج رقم العميل من MESSAGE BODY إلا إذا كان الرقم واضحًا جدًا أنه رقم هاتف."
        // Only if text contains an explicit contact cue ("تواصل معي", "اتصل بي", "رقمي", etc.)
        if (hasExplicitContactCue(cleanText)) {
            val candidateInText = extractExplicitCandidateFromText(cleanText)
            if (candidateInText != null) {
                val bodyVal = validateCandidate(candidateInText, defaultCountryCode, cleanText, isExplicitTitle = false)
                if (bodyVal.isValid) {
                    return NotificationParseResult(
                        isGroup = false,
                        sourceLabel = sourceLabel,
                        candidateFound = true,
                        candidate = candidateInText,
                        normalizedNumber = bodyVal.normalizedNumber,
                        confidence = ConfidenceLevel.MEDIUM,
                        sourceType = CandidateSourceType.NOTIFICATION_BODY_EXPLICIT_PHONE,
                        isAccepted = true, // Valid for Review Queue
                        debugDetails = "Extracted from explicit message cue: '${bodyVal.normalizedNumber}' (Review Queue)"
                    )
                } else {
                    return NotificationParseResult(
                        isGroup = false,
                        sourceLabel = sourceLabel,
                        candidateFound = false,
                        confidence = ConfidenceLevel.LOW,
                        sourceType = CandidateSourceType.NOTIFICATION_BODY_UNVERIFIED,
                        isAccepted = false,
                        rejectionReason = "Rejected invalid phone candidate: ${bodyVal.rejectionReason}",
                        debugDetails = "Cue found but candidate invalid"
                    )
                }
            }
        }

        // If no explicit cue, reject any numbers found in text body to avoid capturing prices, models, years!
        return NotificationParseResult(
            isGroup = false,
            sourceLabel = sourceLabel,
            candidateFound = false,
            confidence = ConfidenceLevel.LOW,
            sourceType = CandidateSourceType.NOTIFICATION_BODY_UNVERIFIED,
            isAccepted = false,
            rejectionReason = "Phone number unavailable (Title is not a phone number and text lacks explicit contact share)",
            debugDetails = "Sender name: '$cleanTitle'. No reliable phone number exposed."
        )
    }

    /**
     * Extracts a phone number sequence from text specifically when preceded or followed by contact cues.
     */
    private fun extractExplicitCandidateFromText(text: String): String? {
        val converted = convertArabicNumerals(text)
        // Match numbers with 7 to 15 digits, optional +, spaces, dashes
        val phoneRegex = Regex("""(?:\+?\d{1,4}[\s\-]?)?(?:\(?\d{2,4}\)?[\s\-]?)?\d{3,5}[\s\-]?\d{3,5}""")
        val matches = phoneRegex.findAll(converted)

        for (m in matches) {
            val candidate = m.value.trim()
            val digits = candidate.filter { it.isDigit() }
            if (digits.length in 7..15) {
                return candidate
            }
        }
        return null
    }
}
