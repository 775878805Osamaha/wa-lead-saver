package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.database.entity.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExportHelper {
    /**
     * Streams audit records in bounded pages instead of creating a giant
     * StringBuilder/Intent.EXTRA_TEXT payload that can exceed Binder limits.
     */
    suspend fun exportHistoryToCsv(
        context: Context,
        loadPage: suspend (beforeTimestamp: Long?, beforeId: Long?) -> List<HistoryEntity>
    ) {
        val file = withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "csv_exports").apply {
                if (!exists() && !mkdirs()) error("Cannot prepare export directory")
            }
            // Retain recent exports long enough for the share recipient to read.
            directory.listFiles()?.forEach { old ->
                if (old.isFile && old.lastModified() < System.currentTimeMillis() - 86_400_000L) {
                    old.delete()
                }
            }
            val output = File(directory, "wa-lead-saver-history-${System.currentTimeMillis()}.csv")
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            output.bufferedWriter(Charsets.UTF_8, 64 * 1024).use { writer ->
                writer.write("\uFEFF") // Excel-friendly UTF-8 with Arabic text.
                writer.write("ID,Phone Number,Contact Name,Source,Timestamp,Status,Details\n")
                var cursorTimestamp: Long? = null
                var cursorId: Long? = null
                while (true) {
                    val page = loadPage(cursorTimestamp, cursorId)
                    if (page.isEmpty()) break
                    for (item in page) {
                        val whenText = dateFormat.format(Date(item.timestamp))
                        val fields = listOf(
                            item.id.toString(),
                            item.phoneNumber,
                            item.contactName,
                            item.source,
                            whenText,
                            item.status,
                            item.details
                        )
                        writer.write(fields.joinToString(",") { csvCell(it) })
                        writer.newLine()
                    }
                    val previous = cursorTimestamp to cursorId
                    cursorTimestamp = page.last().timestamp
                    cursorId = page.last().id
                    check((cursorTimestamp to cursorId) != previous) {
                        "Export paging cursor failed to advance"
                    }
                }
            }
            output
        }

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "WA Lead Saver - History Export")
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "WA Lead Saver CSV", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "تصدير سجل العمليات")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // Escape quotes/newlines, and prevent formula execution in spreadsheet apps.
    internal fun csvCell(value: String): String {
        val risky = value.trimStart().firstOrNull() in listOf('=', '+', '-', '@')
        val safe = if (risky) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
}
