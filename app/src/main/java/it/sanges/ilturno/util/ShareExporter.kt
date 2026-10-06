package it.sanges.ilturno.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import it.sanges.ilturno.R
import it.sanges.ilturno.domain.model.WeekSnapshot
import java.io.File
import java.util.UUID

enum class ExportFormat(val extension: String, val mime: String) {
    PDF("pdf", "application/pdf"), XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
}
data class ExportedFile(val file: File, val format: ExportFormat, val weekLabel: String)

object ShareExporter {
    fun generate(context: Context, snapshot: WeekSnapshot, format: ExportFormat): ExportedFile {
        val directory = File(context.cacheDir, "exports/${UUID.randomUUID()}")
        check(directory.mkdirs())
        val file = File(directory, snapshot.fileName(format.extension))
        try {
            file.outputStream().use { output ->
                when (format) {
                    ExportFormat.XLSX -> ExcelExporter.write(snapshot, output)
                    ExportFormat.PDF -> PdfExporter.write(snapshot, PdfLabels(
                        context.getString(R.string.export_title), context.getString(R.string.export_day),
                        context.getString(R.string.lunch), context.getString(R.string.dinner), context.getString(R.string.export_none),
                        { context.getString(R.string.export_page, it) },
                    ), output)
                }
            }
            DebugDiagnostics.event("export.file format=${format.name} bytes=${file.length()}")
            return ExportedFile(file, format, snapshot.label)
        } catch (exception: Exception) { file.delete(); directory.delete(); throw exception }
    }

    fun intent(context: Context, exported: ExportedFile): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", exported.file)
        return Intent(Intent.ACTION_SEND).apply {
            type = exported.format.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${context.getString(R.string.export_title)} · ${exported.weekLabel}")
            clipData = ClipData.newRawUri(exported.file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
