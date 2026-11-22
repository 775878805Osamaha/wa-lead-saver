package com.example.data.bridge

/**
 * Typed, whitelisted command surface for the planned Flutter MethodChannel
 * "wa_lead_saver/native_v1".
 *
 * Flutter embedding is deliberately not imported into the existing Android
 * host until the add-to-app module can be built and tested. This handler can
 * be unit-tested today and later called from MethodChannel.setMethodCallHandler.
 */
class NativeUiCommandRouter(
    private val dataSource: AndroidLeadUiDataSource
) {
    suspend fun execute(
        method: String,
        args: Map<String, Any?> = emptyMap()
    ): Any = when (method) {
        "getOverview" -> dataSource.getOverview()
        "getLeads" -> dataSource.getLeads(
            query = optionalString(args, "query"),
            status = optionalString(args, "status"),
            cursor = cursor(args),
            limit = optionalLimit(args, 40),
            reviewOnly = optionalBoolean(args, "reviewOnly")
        )
        "getHistory" -> dataSource.getHistory(
            filter = optionalString(args, "filter").ifBlank { "ALL" },
            cursor = cursor(args),
            limit = optionalLimit(args, 60)
        )
        "getSettings" -> dataSource.getSettings()
        "getAccountInfo" -> dataSource.getAccountInfo()
        "getCountryCode" -> mapOf("countryCode" to dataSource.getCountryCode())
        "setCountryCode" -> {
            dataSource.setCountryCode(requiredString(args, "countryCode"))
            mapOf("ok" to true)
        }
        "setSetting" -> {
            dataSource.setSetting(
                key = requiredString(args, "key"),
                enabled = requiredBoolean(args, "enabled")
            )
            mapOf("ok" to true)
        }
        "saveLead" -> {
            val id = (args["id"] as? Number)?.toLong()
                ?: throw IllegalArgumentException("Missing lead id")
            dataSource.saveLead(id)
            mapOf("ok" to true)
        }
        "createLead" -> {
            mapOf("id" to dataSource.createLead(
                phone = requiredString(args, "phone"),
                name = optionalString(args, "name")
            ))
        }
        "exportHistory" -> {
            dataSource.exportHistory()
            mapOf("ok" to true)
        }
        "clearHistory" -> {
            require(requiredString(args, "confirmation") == "CLEAR_ALL_AUDIT_HISTORY") {
                "Explicit history deletion confirmation is required"
            }
            dataSource.clearHistory()
            mapOf("ok" to true)
        }
        "openNativeTool" -> {
            dataSource.openNativeTool(requiredString(args, "tool"))
            mapOf("ok" to true)
        }
        "callLead" -> {
            dataSource.callLead(requiredString(args, "number"))
            mapOf("ok" to true)
        }
        "openWhatsAppLead" -> {
            dataSource.openWhatsAppLead(requiredString(args, "number"))
            mapOf("ok" to true)
        }
        else -> throw IllegalArgumentException("Unsupported native method")
    }

    private fun optionalString(args: Map<String, Any?>, name: String): String =
        when (val value = args[name]) {
            null -> ""
            is String -> value
            else -> throw IllegalArgumentException("Invalid $name")
        }

    private fun requiredString(args: Map<String, Any?>, name: String): String {
        val value = args[name] as? String
        require(!value.isNullOrBlank()) { "Missing $name" }
        return value
    }

    private fun cursor(args: Map<String, Any?>): String? =
        when (val value = args["cursor"]) {
            null -> null
            is String -> value
            else -> throw IllegalArgumentException("Invalid cursor")
        }

    private fun optionalBoolean(args: Map<String, Any?>, name: String): Boolean =
        when (val value = args[name]) {
            null -> false
            is Boolean -> value
            else -> throw IllegalArgumentException("Invalid $name")
        }

    private fun requiredBoolean(args: Map<String, Any?>, name: String): Boolean =
        args[name] as? Boolean
            ?: throw IllegalArgumentException("Missing or invalid $name")

    private fun optionalLimit(args: Map<String, Any?>, default: Int): Int =
        when (val value = args["limit"]) {
            null -> default
            is Int -> value.coerceIn(1, 100)
            is Long -> value.coerceIn(1, 100).toInt()
            else -> throw IllegalArgumentException("Invalid page limit")
        }
}
