package com.example.d_linkmobilymanagement.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.d_linkmobilymanagement.ui.model.LogUiModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogExportHelper {

    fun exportLogs(context: Context, logs: List<LogUiModel>) {
        if (logs.isEmpty()) return

        val exportText = buildLogsExportText(context, logs)
        val fileName = "router_logs_${SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())}.txt"
        
        try {
            val file = File(context.cacheDir, "logs").apply {
                if (!exists()) mkdirs()
            }.let { File(it, fileName) }

            file.writeText(exportText)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "تصدير السجل"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildLogsExportText(context: Context, logs: List<LogUiModel>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return buildString {
            appendLine("D-Link Mobily Management - Logs Export")
            appendLine("Exported at: ${dateFormat.format(Date())}")
            appendLine("-------------------------------------------")
            appendLine()

            logs.forEach { log ->
                val status = if (log.isSuccess) "SUCCESS" else "FAILURE"
                appendLine("[${dateFormat.format(log.timestamp)}] [${log.type.name}] [$status]")
                
                val message = if (log.messageArgs != null) {
                    context.getString(log.messageRes, *log.messageArgs.toTypedArray())
                } else {
                    context.getString(log.messageRes)
                }
                appendLine(message)
                appendLine()
            }
        }
    }
}
