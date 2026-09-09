package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsAppHelper {
    fun openChat(context: Context, normalizedNumber: String) {
        val digits = PhoneNumberHelper.toWaMeDigits(normalizedNumber)
        if (digits.isBlank()) {
            Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse("https://wa.me/$digits")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context) {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
            ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
        }
    }
}
