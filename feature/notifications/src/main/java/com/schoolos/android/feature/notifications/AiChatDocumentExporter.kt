package com.schoolos.android.feature.notifications

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportedDocumentResult(
    val uri: Uri?,
    val fileName: String,
    val mimeType: String,
)

fun normalizeAiIndonesianText(markdown: String): String {
    return markdown
        .replace("Produkturnya", "Produktivitasnya")
        .replace("produkturnya", "produktivitasnya")
        .replace("Produktifitasnya", "Produktivitasnya")
        .replace("produktifitasnya", "produktivitasnya")
        .replace("Produktifitas", "Produktivitas")
        .replace("produktifitas", "produktivitas")
        .replace("Komarasi", "Kolaborasi")
        .replace("komarasi", "kolaborasi")
        .replace("Aktifitas", "Aktivitas")
        .replace("aktifitas", "aktivitas")
        .replace("Efektifitas", "Efektivitas")
        .replace("efektifitas", "efektivitas")
        .replace("Kwalitas", "Kualitas")
        .replace("kwalitas", "kualitas")
        .replace("Sistim", "Sistem")
        .replace("sistim", "sistem")
}

fun suggestAiDocumentTitle(markdown: String, fallback: String = "Jawaban Akselerasi Edu AI"): String {
    val lines = normalizeAiIndonesianText(markdown).replace("\r\n", "\n").split("\n")
    val heading = lines.firstOrNull { it.trim().startsWith("#") }
        ?.replace(Regex("^#{1,6}\\s+"), "")
        ?.replace("**", "")
        ?.trim()
    if (!heading.isNullOrBlank() && heading.length >= 4) {
        return heading.take(72)
    }
    val plainFirst = lines.map {
        it.trim()
            .replace(Regex("^[-*•\\d.)#\\s]+"), "")
            .replace("**", "")
            .replace("`", "")
            .trim()
    }.firstOrNull { it.length >= 6 }
    return plainFirst?.take(64) ?: fallback
}

fun markdownToReadablePlainText(markdown: String): String {
    return normalizeAiIndonesianText(markdown)
        .replace("\r\n", "\n")
        .split("\n")
        .joinToString("\n") { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("#") -> trimmed.replace(Regex("^#{1,6}\\s+"), "").replace("**", "").uppercase()
                else -> trimmed
                    .replace(Regex("\\*\\*([^*\\n]+)\\*\\*"), "$1")
                    .replace(Regex("__([^_\\n]+)__"), "$1")
                    .replace(Regex("`([^`\\n]+)`"), "$1")
                    .replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)"), "$1 ($2)")
            }
        }
        .trim()
}

private fun sanitizeFileSlug(title: String): String {
    val slug = title.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .take(44)
    val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.ROOT).format(Date())
    return "${slug.ifBlank { "dokumen-akselerasi-ai" }}-$stamp"
}

private fun escapeHtml(raw: String): String = raw
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

private fun markdownToWordHtml(title: String, authorLabel: String, markdown: String): String {
    val dateLabel = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())
    val lines = normalizeAiIndonesianText(markdown).replace("\r\n", "\n").split("\n")
    val bodyBuilder = StringBuilder()
    var inList = false

    fun closeList() {
        if (inList) {
            bodyBuilder.append("</ul>\n")
            inList = false
        }
    }

    for (raw in lines) {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            closeList()
            continue
        }
        val formattedInline = escapeHtml(trimmed)
            .replace(Regex("\\*\\*([^*\\n]+)\\*\\*"), "<strong>$1</strong>")
            .replace(Regex("`([^`\\n]+)`"), "<code>$1</code>")

        when {
            trimmed.startsWith("### ") -> {
                closeList()
                bodyBuilder.append("<h3>").append(formattedInline.removePrefix("### ")).append("</h3>\n")
            }
            trimmed.startsWith("## ") -> {
                closeList()
                bodyBuilder.append("<h2>").append(formattedInline.removePrefix("## ")).append("</h2>\n")
            }
            trimmed.startsWith("# ") -> {
                closeList()
                bodyBuilder.append("<h1>").append(formattedInline.removePrefix("# ")).append("</h1>\n")
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                if (!inList) {
                    bodyBuilder.append("<ul>\n")
                    inList = true
                }
                val itemText = formattedInline.replaceFirst(Regex("^(-|\\*|•)\\s+"), "")
                bodyBuilder.append("<li>").append(itemText).append("</li>\n")
            }
            else -> {
                closeList()
                bodyBuilder.append("<p>").append(formattedInline).append("</p>\n")
            }
        }
    }
    closeList()

    return """<!DOCTYPE html>
<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word" lang="id">
<head>
<meta charset="utf-8" />
<title>${escapeHtml(title)}</title>
<style>
  body { font-family: Calibri, Arial, sans-serif; font-size: 11pt; line-height: 1.55; color: #0f172a; margin: 20px; }
  .header { border-bottom: 2px solid #0284c7; padding-bottom: 8px; margin-bottom: 14px; }
  .brand { font-size: 9pt; font-weight: bold; color: #0284c7; text-transform: uppercase; }
  h1 { font-size: 15pt; margin: 12px 0 6px; }
  h2 { font-size: 13pt; margin: 10px 0 5px; }
  h3 { font-size: 11.5pt; margin: 8px 0 4px; }
  p { margin: 0 0 8px; }
  ul { margin: 4px 0 10px 20px; }
</style>
</head>
<body>
  <div class="header">
    <div class="brand">${escapeHtml(authorLabel)} • Kurikulum Merdeka</div>
    <h1>${escapeHtml(title)}</h1>
    <div style="font-size:9pt;color:#475569;">Dibuat pada $dateLabel WIB</div>
  </div>
  ${bodyBuilder}
</body>
</html>"""
}

private fun saveBytesToDownloads(
    context: Context,
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
): ExportedDocumentResult {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Gagal membuat file di folder Downloads.")
        resolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: throw IllegalStateException("Gagal menulis data file ke Downloads.")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return ExportedDocumentResult(uri = uri, fileName = fileName, mimeType = mimeType)
    } else {
        @Suppress("DEPRECATION")
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            out.write(bytes)
            out.flush()
        }
        return ExportedDocumentResult(uri = Uri.fromFile(file), fileName = fileName, mimeType = mimeType)
    }
}

fun exportAiReplyAsPdfToDownloads(
    context: Context,
    markdownContent: String,
    authorLabel: String,
): Result<ExportedDocumentResult> = runCatching {
    val docTitle = suggestAiDocumentTitle(markdownContent)
    val fileName = "${sanitizeFileSlug(docTitle)}.pdf"
    val plainText = markdownToReadablePlainText(markdownContent)
    val dateLabel = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID")).format(Date())

    val pageWidth = 595
    val pageHeight = 842
    val margin = 44f
    val contentWidth = (pageWidth - margin * 2).toInt()

    val headerBrandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0284C7")
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F172A")
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        textSize = 9f
    }
    val dividerPaint = Paint().apply {
        color = Color.parseColor("#0284C7")
        strokeWidth = 1.5f
    }
    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        textSize = 10.5f
    }

    val titleLayout = StaticLayout.Builder.obtain(docTitle, 0, docTitle.length, titlePaint, contentWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(2f, 1.15f)
        .build()

    val bodyLayout = StaticLayout.Builder.obtain(plainText, 0, plainText.length, bodyPaint, contentWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(2f, 1.25f)
        .build()

    val pdf = PdfDocument()
    try {
        var lineIndex = 0
        var pageNum = 1
        while (lineIndex < bodyLayout.lineCount || pageNum == 1) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            var yCursor = margin

            if (pageNum == 1) {
                canvas.drawText("$authorLabel • Kurikulum Merdeka".uppercase(Locale.ROOT), margin, yCursor + 10f, headerBrandPaint)
                yCursor += 18f
                canvas.withTranslation(margin, yCursor) {
                    titleLayout.draw(this)
                }
                yCursor += titleLayout.height + 6f
                canvas.drawText("Dibuat pada $dateLabel", margin, yCursor + 8f, metaPaint)
                yCursor += 16f
                canvas.drawLine(margin, yCursor, pageWidth - margin, yCursor, dividerPaint)
                yCursor += 16f
            } else {
                canvas.drawText("$docTitle (Hal. $pageNum)", margin, yCursor + 8f, metaPaint)
                yCursor += 22f
            }

            val bottomLimit = pageHeight - margin
            while (lineIndex < bodyLayout.lineCount) {
                val lineHeight = (bodyLayout.getLineBottom(lineIndex) - bodyLayout.getLineTop(lineIndex)).toFloat()
                if (yCursor + lineHeight > bottomLimit) {
                    break
                }
                val start = bodyLayout.getLineStart(lineIndex)
                val end = bodyLayout.getLineEnd(lineIndex)
                val lineStr = plainText.substring(start, end).trimEnd('\n')
                val baseline = yCursor - bodyLayout.getLineAscent(lineIndex)
                canvas.drawText(lineStr, margin, baseline, bodyPaint)
                yCursor += lineHeight
                lineIndex++
            }

            pdf.finishPage(page)
            pageNum++
            if (lineIndex >= bodyLayout.lineCount) break
        }

        val outStream = ByteArrayOutputStream()
        pdf.writeTo(outStream)
        saveBytesToDownloads(
            context = context,
            fileName = fileName,
            mimeType = "application/pdf",
            bytes = outStream.toByteArray(),
        )
    } finally {
        pdf.close()
    }
}

fun exportAiReplyAsWordToDownloads(
    context: Context,
    markdownContent: String,
    authorLabel: String,
): Result<ExportedDocumentResult> = runCatching {
    val docTitle = suggestAiDocumentTitle(markdownContent)
    val fileName = "${sanitizeFileSlug(docTitle)}.doc"
    val html = markdownToWordHtml(docTitle, authorLabel, markdownContent)
    val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + html.toByteArray(Charsets.UTF_8)
    saveBytesToDownloads(
        context = context,
        fileName = fileName,
        mimeType = "application/msword",
        bytes = bytes,
    )
}

fun tryOpenExportedDocument(context: Context, exported: ExportedDocumentResult): Boolean {
    val uri = exported.uri ?: return false
    return try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, exported.mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
        true
    } catch (_: Throwable) {
        false
    }
}
