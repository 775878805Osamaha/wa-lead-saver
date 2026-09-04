package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.database.entity.HistoryEntity
import com.example.data.database.entity.LeadEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VcfExportHelper {

    /**
     * Builds standard vCard 3.0 format for a list of contacts (name + phone number + note).
     */
    fun buildVCardContent(contacts: List<VCardContact>): String {
        val sb = StringBuilder()
        for (contact in contacts) {
            val cleanPhone = PhoneNumberHelper.normalize(contact.phoneNumber)
            val cleanName = contact.name.trim().ifEmpty { cleanPhone }
            sb.append("BEGIN:VCARD\r\n")
            sb.append("VERSION:3.0\r\n")
            sb.append("FN:$cleanName\r\n")
            sb.append("N:;$cleanName;;;\r\n")
            sb.append("TEL;TYPE=CELL,VOICE,PREF:$cleanPhone\r\n")
            if (contact.note.isNotBlank()) {
                sb.append("NOTE:${escapeVCard(contact.note)}\r\n")
            }
            sb.append("END:VCARD\r\n")
        }
        return sb.toString()
    }

    /**
     * Writes vCard string into cache and launches system chooser with FileProvider URI.
     */
    fun exportContactsToVcf(context: Context, contacts: List<VCardContact>, fileNamePrefix: String = "wa_contacts") {
        if (contacts.isEmpty()) {
            Toast.makeText(context, "No contacts available to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val vCardContent = buildVCardContent(contacts)
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "${fileNamePrefix}_$timeStamp.vcf")

            FileOutputStream(file).use { fos ->
                fos.write(vCardContent.toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "WA Leads - vCard Contacts (${contacts.size})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Export VCF Contacts (${contacts.size})").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun fromHistory(historyList: List<HistoryEntity>): List<VCardContact> {
        return historyList
            .filter { it.phoneNumber.isNotBlank() }
            .distinctBy { PhoneNumberHelper.normalize(it.phoneNumber) }
            .map {
                VCardContact(
                    name = it.contactName,
                    phoneNumber = it.phoneNumber,
                    note = "Source: ${it.source}, Captured: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.timestamp))}"
                )
            }
    }

    fun fromLeads(leads: List<LeadEntity>): List<VCardContact> {
        return leads
            .filter { it.phoneNumber.isNotBlank() }
            .distinctBy { it.normalizedNumber }
            .map {
                VCardContact(
                    name = it.contactName,
                    phoneNumber = it.phoneNumber,
                    note = "Source: ${it.source}, Status: ${it.status}"
                )
            }
    }

    private fun escapeVCard(value: String): String {
        return value.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }
}

data class VCardContact(
    val name: String,
    val phoneNumber: String,
    val note: String = ""
)
