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
     * Cleans invisible Unicode bidi markers, isolates, zero-width characters,
     * and normalizes non-breaking spaces and Eastern Arabic numerals to standard digits.
     */
    fun cleanInvisibleAndBidiChars(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                // Invisible bidirectional and isolate markers
                '\u200E', '\u200F', // LRM, RLM
                '\u202A', '\u202B', '\u202C', '\u202D', '\u202E', // Embeddings, overrides, PDF
                '\u2066', '\u2067', '\u2068', '\u2069', // Isolates: LRI, RLI, FSI, PDI
                '\uFEFF', // Zero-width no-break space (BOM)
                '\u200B', '\u200C', '\u200D', // Zero-width space, ZWNJ, ZWJ
                '\u0000', '\u0001', '\u0002', '\u0003', '\u0004' -> {
                    // Skip completely
                }
                // Non-breaking and special whitespace -> standard ASCII space
                '\u00A0', '\u2007', '\u202F', '\u3000' -> sb.append(' ')
                // Eastern Arabic-Indic numerals
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
        return sb.toString().trim()
    }

    /**
     * Converts Eastern Arabic-Indic numerals (٠١٢٣٤٥٦٧٨٩) and Persian numerals (۰۱۲۳۴۵۶۷۸۹) to standard ASCII digits.
     */
    fun convertArabicNumerals(input: String): String {
        return cleanInvisibleAndBidiChars(input)
    }

    /**
     * Cleans up raw phone strings by converting numerals and stripping formatting characters.
     */
    fun cleanRawDigits(raw: String): String {
        val ascii = cleanInvisibleAndBidiChars(raw)
        val hasLeadingPlus = ascii.startsWith("+") || ascii.startsWith("00")
        val digitsOnly = ascii.filter { it.isDigit() }
        val effectiveDigits = if (ascii.startsWith("00")) digitsOnly.removePrefix("00") else digitsOnly
        return if (hasLeadingPlus) "+$effectiveDigits" else digitsOnly
    }

    /**
     * Validates a candidate phone number string according to strict rules:
     * - Discards prices, counters, years (e.g. 2020..2035), short numbers (<7 digits), excessively long numbers (>15 digits)
     * - Performs Yemen (+967) validation and proper prefix mapping (70, 71, 73, 77, 78 and landlines)
     * - Preserves and normalizes international numbers with leading + or 00
     * - Returns a PhoneValidationResult with confidence and rejection reasons
     */
    fun validateCandidate(
        rawCandidate: String,
        defaultCountryCode: String = "+967",
        contextText: String = "",
        isExplicitTitle: Boolean = false
    ): PhoneValidationResult {
        val cleaned = cleanInvisibleAndBidiChars(rawCandidate)
        if (cleaned.isEmpty()) {
            return PhoneValidationResult(isValid = false, rejectionReason = "Empty number candidate")
        }

        val cleanCountry = if (defaultCountryCode.startsWith("+")) defaultCountryCode else "+$defaultCountryCode"
        val countryDigits = cleanCountry.removePrefix("+")

        // Reject if candidate is just counter text
        if (cleaned.contains("message", ignoreCase = true) || cleaned.contains("رسال", ignoreCase = true)) {
            return PhoneValidationResult(isValid = false, rejectionReason = "Matches notification counter text")
        }

        val rawDigits = cleaned.filter { it.isDigit() }
        val hasPlus = cleaned.startsWith("+") || cleaned.startsWith("00")

        // Convert leading 00 to international format
        val digitsOnly = if (cleaned.startsWith("00")) rawDigits.removePrefix("00") else rawDigits

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

        // 2. Reject years (e.g. 1900..2050)
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

        // Determine if candidate targets Yemen (+967)
        val isExplicitYemenPrefix = digitsOnly.startsWith("967")
        val isYemenCountry = isExplicitYemenPrefix || (!hasPlus && cleanCountry == "+967")

        var normalized = ""
        var carrierHint = ""
        var confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM

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
                            localDigits.startsWith("4") || localDigits.startsWith("5") || localDigits.startsWith("6") ||
                            localDigits.startsWith("7"))

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
                if (digitsOnly.startsWith("967") || hasPlus) {
                    if (digitsOnly.length in 9..15) {
                        normalized = "+$digitsOnly"
                        carrierHint = "International / Yemen"
                        confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
                    } else {
                        return PhoneValidationResult(
                            isValid = false,
                            rejectionReason = "Invalid Yemen number structure: length ${digitsOnly.length} is not a valid Yemen mobile or landline"
                        )
                    }
                } else if (digitsOnly.length in 7..10) {
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
                carrierHint = "International Number"
                confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            } else if (digitsOnly.startsWith(countryDigits)) {
                normalized = "+$digitsOnly"
                carrierHint = "Default Country ($cleanCountry)"
                confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            } else if (digitsOnly.startsWith("0")) {
                normalized = "$cleanCountry${digitsOnly.removePrefix("0")}"
                carrierHint = "Default Country ($cleanCountry)"
                confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            } else {
                normalized = "$cleanCountry$digitsOnly"
                carrierHint = "Default Country ($cleanCountry)"
                confidence = if (isExplicitTitle) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            }
        }

        // If contextText contains strong currency/price clues near the candidate, downgrade or reject
        if (contextText.isNotBlank()) {
            val lowerContext = contextText.lowercase()
            for (keyword in PRICE_OR_COUNT_KEYWORDS) {
                if (lowerContext.contains(keyword)) {
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
            detectedCountryCode = if (isYemenCountry) "+967" else cleanCountry,
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
     * Extracts a candidate phone number from title text.
     * Supports formats such as:
     * - "+967 730 232 807"
     * - "+967730232807"
     * - "00967730232807"
     * - "730232807"
     * - "0730232807"
     * - "+967 730 232 807 (2 messages)"
     * - "~Ahmed (+967 730 232 807)"
     */
    fun extractCandidateFromTitle(cleanTitle: String): String? {
        var trimmed = cleanInvisibleAndBidiChars(cleanTitle).trim()
        if (trimmed.isEmpty()) return null

        // Strip trailing message count indicators such as " (2 messages)", " (2)", " (2 رسائل)", " (رسالتان)"
        trimmed = trimmed.replace(Regex("""\s*\(\s*\d+\s*(?:messages?|new messages?|رسائل|رسالة|واردة)?\s*\)$""", RegexOption.IGNORE_CASE), "")
        trimmed = trimmed.replace(Regex("""\s*:\s*\d+\s*(?:new messages?|messages?|رسائل|رسالة)?$""", RegexOption.IGNORE_CASE), "")
        trimmed = trimmed.trim()

        // 1. Check if the string begins with or contains international phone: +[\d\s\-]{6,20}\d ending in a digit
        val internationalRegex = Regex("""\+[\d\s\-]{6,20}\d""")
        val intlMatch = internationalRegex.find(trimmed)
        if (intlMatch != null) {
            val candidate = intlMatch.value.trim()
            val matchDigits = candidate.filter { it.isDigit() }
            if (matchDigits.length in 7..15) {
                return candidate
            }
        }

        // 2. Check if begins with 00 prefix: 00[\d\s\-]{6,20}\d
        val dblZeroRegex = Regex("""00[\d\s\-]{6,20}\d""")
        val dblZeroMatch = dblZeroRegex.find(trimmed)
        if (dblZeroMatch != null) {
            val candidate = dblZeroMatch.value.trim()
            val matchDigits = candidate.filter { it.isDigit() }
            if (matchDigits.length in 7..15) {
                return candidate
            }
        }

        // 3. Check for local digits: e.g. 07XXXXXXXX or 7XXXXXXXX
        val localRegex = Regex("""\b0?7\d{8}\b""")
        val localMatch = localRegex.find(trimmed)
        if (localMatch != null) {
            return localMatch.value.trim()
        }

        // 4. If title is primarily digits with spacing/hyphen
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length in 7..15) {
            val generalPhone = Regex("""[\d\s\-]{6,20}\d""")
            val genMatch = generalPhone.find(trimmed)
            if (genMatch != null) {
                val candidate = genMatch.value.trim()
                val matchDigits = candidate.filter { it.isDigit() }
                if (matchDigits.length in 7..15) {
                    return candidate
                }
            }
        }

        return null
    }

    /**
     * Extracts a phone number from a WhatsApp JID or tag string (e.g. "967730232807@s.whatsapp.net").
     */
    fun extractCandidateFromJid(jidOrTag: String): String? {
        if (jidOrTag.isBlank()) return null
        val cleaned = cleanInvisibleAndBidiChars(jidOrTag)
        val jidRegex = Regex("""(?:\+?(\d{7,15}))@(s\.whatsapp\.net|c\.us)""")
        val match = jidRegex.find(cleaned)
        return match?.groupValues?.getOrNull(1)
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

        if (subText.contains("group", ignoreCase = true) || subText.contains("مجموعة", ignoreCase = true)) {
            return true
        }

        // WhatsApp group notification title is the Group Name, and text starts with "Sender: message"
        val senderPrefixRegex = Regex("""^[\p{L}\s\d_+.-]{1,30}\s*:\s*.+""")
        val textHasSenderPrefix = senderPrefixRegex.matches(text.trim())

        val titleLooksLikePhone = extractCandidateFromTitle(title) != null
        if (!titleLooksLikePhone && textHasSenderPrefix) {
            return true
        }

        return false
    }

    /**
     * Master notification parser implementing strict parsing rules.
     * Priority:
     * 1. Notification Title / Big Title (e.g. "+967 730 232 807")
     * 2. SubText / Conversation Title
     * 3. Person / Sender URI (e.g. "tel:+967730232807")
     * 4. SBN Tag / JID (e.g. "967730232807@s.whatsapp.net")
     * 5. Message Body with EXPLICIT contact cues ONLY (Queue review, never auto-saved)
     */
    fun parseNotification(
        packageName: String,
        title: String,
        text: String,
        subText: String = "",
        bigText: String = "",
        titleBig: String = "",
        conversationTitle: String = "",
        messagingPersonName: String = "",
        messagingPersonUri: String = "",
        tag: String = "",
        isGroupConversation: Boolean = false,
        defaultCountryCode: String = "+967"
    ): NotificationParseResult {
        val isBusiness = packageName == "com.whatsapp.w4b"
        val baseSource = if (isBusiness) "WhatsApp Business" else "WhatsApp"

        val cleanTitle = cleanInvisibleAndBidiChars(if (title.isNotBlank()) title else titleBig)
        val cleanText = cleanInvisibleAndBidiChars(if (bigText.length > text.length) bigText else text)
        val cleanSubText = cleanInvisibleAndBidiChars(subText)
        val cleanConversationTitle = cleanInvisibleAndBidiChars(conversationTitle)

        val isGroup = isGroupNotification(cleanTitle, cleanText, cleanSubText, isGroupConversation)
        val sourceLabel = if (isGroup) "WhatsApp Group" else baseSource

        // 1. Group Rule: Never extract random body numbers in groups
        if (isGroup) {
            val groupTitleCandidate = extractCandidateFromTitle(cleanTitle)
            if (groupTitleCandidate != null) {
                val titleValidation = validateCandidate(groupTitleCandidate, defaultCountryCode, isExplicitTitle = true)
                if (titleValidation.isValid && titleValidation.confidence == ConfidenceLevel.HIGH) {
                    return NotificationParseResult(
                        isGroup = true,
                        sourceLabel = sourceLabel,
                        candidateFound = true,
                        candidate = groupTitleCandidate,
                        normalizedNumber = titleValidation.normalizedNumber,
                        confidence = ConfidenceLevel.HIGH,
                        sourceType = CandidateSourceType.GROUP_NOTIFICATION,
                        isAccepted = true,
                        debugDetails = "Group title was a verified phone number: ${titleValidation.carrierHint}"
                    )
                }
            }

            // Only if text contains an EXPLICIT phone contact cue ("تواصل معي على 771234567")
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

        // 2. Priority 1: Check Notification Title (Sender Phone Number)
        // e.g. "+967 730 232 807" or "+967730232807" or "730232807"
        val titleCandidate = extractCandidateFromTitle(cleanTitle)
        if (titleCandidate != null) {
            val titleVal = validateCandidate(titleCandidate, defaultCountryCode, isExplicitTitle = true)
            if (titleVal.isValid) {
                return NotificationParseResult(
                    isGroup = false,
                    sourceLabel = sourceLabel,
                    candidateFound = true,
                    candidate = titleCandidate,
                    normalizedNumber = titleVal.normalizedNumber,
                    confidence = ConfidenceLevel.HIGH,
                    sourceType = CandidateSourceType.NOTIFICATION_TITLE_UNSAVED_SENDER,
                    isAccepted = true,
                    debugDetails = "Direct unsaved sender in notification title: ${titleVal.carrierHint}"
                )
            }
        }

        // 3. Priority 2: Check subText or conversationTitle (Often used in WhatsApp Business for the customer phone)
        val subCandidate = extractCandidateFromTitle(cleanSubText) ?: extractCandidateFromTitle(cleanConversationTitle)
        if (subCandidate != null) {
            val subVal = validateCandidate(subCandidate, defaultCountryCode, isExplicitTitle = true)
            if (subVal.isValid) {
                return NotificationParseResult(
                    isGroup = false,
                    sourceLabel = sourceLabel,
                    candidateFound = true,
                    candidate = subCandidate,
                    normalizedNumber = subVal.normalizedNumber,
                    confidence = ConfidenceLevel.HIGH,
                    sourceType = CandidateSourceType.NOTIFICATION_CONVERSATION_METADATA,
                    isAccepted = true,
                    debugDetails = "Sender phone located in notification metadata: ${subVal.carrierHint}"
                )
            }
        }

        // 4. Priority 3: Check messagingPerson URI / name
        val personUriDigits = messagingPersonUri.removePrefix("tel:").filter { it.isDigit() }
        if (personUriDigits.length in 7..15) {
            val personVal = validateCandidate(messagingPersonUri.removePrefix("tel:"), defaultCountryCode, isExplicitTitle = true)
            if (personVal.isValid) {
                return NotificationParseResult(
                    isGroup = false,
                    sourceLabel = sourceLabel,
                    candidateFound = true,
                    candidate = messagingPersonUri,
                    normalizedNumber = personVal.normalizedNumber,
                    confidence = ConfidenceLevel.HIGH,
                    sourceType = CandidateSourceType.NOTIFICATION_CONVERSATION_METADATA,
                    isAccepted = true,
                    debugDetails = "Sender phone found in messagingPerson metadata"
                )
            }
        }

        // 5. Priority 4: Check SBN Tag for WhatsApp JID (e.g. 967730232807@s.whatsapp.net)
        val jidCandidate = extractCandidateFromJid(tag)
        if (jidCandidate != null) {
            val jidVal = validateCandidate(jidCandidate, defaultCountryCode, isExplicitTitle = true)
            if (jidVal.isValid) {
                return NotificationParseResult(
                    isGroup = false,
                    sourceLabel = sourceLabel,
                    candidateFound = true,
                    candidate = jidCandidate,
                    normalizedNumber = jidVal.normalizedNumber,
                    confidence = ConfidenceLevel.HIGH,
                    sourceType = CandidateSourceType.NOTIFICATION_CONVERSATION_METADATA,
                    isAccepted = true,
                    debugDetails = "Sender phone resolved from WhatsApp JID tag"
                )
            }
        }

        // Check if message is a pure system notification (e.g. backup, web active)
        val lowerFull = "$cleanTitle $cleanText $cleanSubText".lowercase()
        if (lowerFull.contains("backup") ||
            lowerFull.contains("نسخ احتياطي") ||
            lowerFull.contains("checking for new messages") ||
            lowerFull.contains("web is currently active")
        ) {
            return NotificationParseResult(
                isGroup = false,
                sourceLabel = sourceLabel,
                candidateFound = false,
                confidence = ConfidenceLevel.LOW,
                sourceType = CandidateSourceType.NOTIFICATION_BODY_UNVERIFIED,
                isAccepted = false,
                rejectionReason = "WhatsApp system status notification",
                debugDetails = "System notification ignored"
            )
        }

        // 6. Priority 5: Message Body with EXPLICIT contact cues ONLY
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
                        isAccepted = true,
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

        // 7. No reliable phone candidate exposed
        return NotificationParseResult(
            isGroup = false,
            sourceLabel = sourceLabel,
            candidateFound = false,
            confidence = ConfidenceLevel.LOW,
            sourceType = CandidateSourceType.NOTIFICATION_BODY_UNVERIFIED,
            isAccepted = false,
            rejectionReason = "Phone number unavailable (Title is not a phone number and message lacks explicit contact share)",
            debugDetails = "Sender name: '$cleanTitle'. No reliable phone number exposed."
        )
    }

    /**
     * Extracts a phone number sequence from text specifically when preceded or followed by contact cues.
     */
    private fun extractExplicitCandidateFromText(text: String): String? {
        val converted = cleanInvisibleAndBidiChars(text)
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
