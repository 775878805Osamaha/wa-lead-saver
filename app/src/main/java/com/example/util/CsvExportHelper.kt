package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.database.entity.HistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {
    fun exportHistoryToCsv(context: Context, historyList: List<HistoryEntity>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csvBuilder = StringBuilder()
        csvBuilder.append("ID,Phone Number,Contact Name,Source,Timestamp,Status,Details\n")
        
        for (item in historyList) {
            val dateStr = dateFormat.format(Date(item.timestamp))
            val safePhone = escapeCsv(item.phoneNumber)
            val safeName = escapeCsv(item.contactName)
            val safeSource = escapeCsv(item.source)
            val safeStatus = escapeCsv(item.status)
            val safeDetails = escapeCsv(item.details)
            csvBuilder.append("${item.id},\"$safePhone\",\"$safeName\",\"$safeSource\",\"$dateStr\",\"$safeStatus\",\"$safeDetails\"\n")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "WA Lead Saver - History Export")
            putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(shareIntent, "Export History CSV").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        return value.replace("\"", "\"\"")
    }
}
