package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsAppHelper {

    /**
     * Opens WhatsApp chat with international number using wa.me URL.
     * Uses Android Intent to open WhatsApp directly when possible.
     */
    fun openChat(context: Context, phoneNumber: String) {
        val digits = PhoneNumberHelper.toWaMeDigits(phoneNumber)
        if (digits.isEmpty()) {
            Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "https://wa.me/$digits"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Try direct WhatsApp launch first
        try {
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                // Try WhatsApp Business next
                intent.setPackage("com.whatsapp.w4b")
                context.startActivity(intent)
            } catch (_: Exception) {
                // Fallback to general browser or chooser
                intent.setPackage(null)
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
