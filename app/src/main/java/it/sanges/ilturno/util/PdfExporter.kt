package it.sanges.ilturno.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import it.sanges.ilturno.domain.model.WeekSnapshot
import java.io.OutputStream
import androidx.core.graphics.withClip

data class PdfLabels(val title: String, val day: String, val lunch: String, val dinner: String, val none: String, val page: (Int) -> String)

/** Seven day rows, with a lunch/dinner column pair per department, on A4 landscape. */
object PdfExporter {
    private const val LEFT = 24f
    private const val RIGHT = 818f
    private const val TOP = 80f
    private const val BOTTOM = 555f
    private data class Row(val cells: List<StaticLayout>, val height: Float)

    fun write(snapshot: WeekSnapshot, labels: PdfLabels, output: OutputStream) {
        val document = PdfDocument()
        val text = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val heading = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val line = Paint().apply { color = Color.rgb(190, 200, 194); strokeWidth = 0.5f }
        val headerFill = Paint().apply { color = Color.rgb(228, 239, 232) }
        val alternateFill = Paint().apply { color = Color.rgb(246, 248, 247) }
        val departments = snapshot.rows.map { it.departmentName }.distinct().ifEmpty { listOf(null) }
        val grouped = departments.any { it != null }
        val dayRight = if (grouped) 96f else 118f
        val width = (RIGHT - dayRight) / (departments.size * 2)
        val columns = listOf(LEFT) + (0..departments.size * 2).map { dayRight + width * it }
        fun layout(value: String, paint: TextPaint, available: Float): StaticLayout =
            StaticLayout.Builder.obtain(value, 0, value.length, paint, maxOf(1, available.toInt()))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build()
        val departmentHeaders = departments.map { layout(it.orEmpty(), heading, 2 * width - 8f) }
        val departmentHeight = if (grouped) departmentHeaders.maxOf { it.height }.toFloat() + 8f else 0f
        val bodyTop = TOP + departmentHeight + 24f
        val days = DateUtils.weekDays(snapshot.start)
        var padding = 4f
        fun prepare(): List<Row> = days.map { day ->
            val values = listOf(DateUtils.shortDay(day)) + departments.flatMap { department ->
                val row = snapshot.rows.find { it.date == day && it.departmentName == department }
                listOf(row?.lunch.orEmpty().joinToString(", ").ifEmpty { labels.none },
                    row?.dinner.orEmpty().joinToString(", ").ifEmpty { labels.none })
            }
            val cells = values.mapIndexed { index, value -> layout(value, text, columns[index + 1] - columns[index] - 2 * padding) }
            Row(cells, maxOf(24f, cells.maxOf { it.height }.toFloat() + 2 * padding))
        }
        var rows = emptyList<Row>()
        for (size in listOf(11f, 10.5f, 10f, 9.5f, 9f, 8.5f, 8f)) {
            text.textSize = size
            padding = if (size >= 10f) 4f else 3f
            rows = prepare()
            if (rows.sumOf { it.height.toDouble() } <= BOTTOM - bodyTop) break
        }
        var page: PdfDocument.Page? = null
        var pageNumber = 0
        var y = bodyTop
        fun finishPage() {
            page?.let {
                val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.DKGRAY }
                it.canvas.drawText(labels.page(pageNumber), LEFT, 577f, footer)
                document.finishPage(it)
            }
            page = null
        }
        fun startPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(842, 595, pageNumber).create())
            val canvas = page!!.canvas
            canvas.drawText(labels.title, LEFT, 43f, title)
            canvas.drawText(snapshot.label, LEFT, 63f, heading)
            canvas.drawRect(LEFT, TOP, RIGHT, bodyTop, headerFill)
            canvas.drawText(labels.day, LEFT + 4f, bodyTop - 8f, heading)
            departments.forEachIndexed { index, _ ->
                val x = dayRight + index * 2 * width
                if (grouped) { canvas.save(); canvas.translate(x + 4f, TOP + 4f); departmentHeaders[index].draw(canvas); canvas.restore() }
                canvas.drawText(labels.lunch, x + 4f, bodyTop - 8f, heading)
                canvas.drawText(labels.dinner, x + width + 4f, bodyTop - 8f, heading)
                canvas.drawLine(x, TOP, x, bodyTop, line)
            }
            y = bodyTop
        }
        try {
            startPage()
            val total = rows.sumOf { it.height.toDouble() }.toFloat()
            val spare = if (total <= BOTTOM - bodyTop) minOf(20f, maxOf(0f, BOTTOM - bodyTop - total - 1f) / rows.size) else 0f
            DebugDiagnostics.event("export.pdf.layout font=${text.textSize} rowsHeight=$total available=${BOTTOM - bodyTop}")
            rows.forEachIndexed { dayIndex, row ->
                val height = row.height + spare
                if (height <= BOTTOM - bodyTop && y + height > BOTTOM) { finishPage(); startPage() }
                var offset = 0
                val contentHeight = row.cells.maxOf { it.height }
                while (offset < contentHeight) {
                    val lineHeight = row.cells.maxOf { it.getLineBottom(0) }
                    if (BOTTOM - y < lineHeight + 2 * padding) { finishPage(); startPage() }
                    val available = BOTTOM - y - 2 * padding
                    val remaining = contentHeight - offset
                    val chunk = if (remaining <= available) remaining else row.cells.flatMap { cell ->
                        (0 until cell.lineCount).map { cell.getLineBottom(it) }
                    }.filter { it > offset && it <= offset + available }.maxOrNull()?.minus(offset) ?: lineHeight
                    val segmentHeight = if (offset == 0 && remaining <= available) maxOf(height, chunk + 2 * padding) else chunk + 2 * padding
                    val canvas = page!!.canvas
                    if (dayIndex % 2 == 1) canvas.drawRect(LEFT, y, RIGHT, y + segmentHeight, alternateFill)
                    row.cells.forEachIndexed { index, cell ->
                        canvas.withClip(columns[index] + padding, y + padding, columns[index + 1] - padding, y + segmentHeight - padding) {
                            translate(columns[index] + padding, y + padding - if (index == 0) 0f else offset.toFloat())
                            cell.draw(this)
                        }
                    }
                    canvas.drawLine(LEFT, y + segmentHeight, RIGHT, y + segmentHeight, line)
                    columns.forEach { x -> canvas.drawLine(x, y, x, y + segmentHeight, line) }
                    y += segmentHeight
                    offset += chunk
                    if (offset < contentHeight) { finishPage(); startPage() }
                }
            }
            finishPage()
            document.writeTo(output)
        } finally { page?.let { document.finishPage(it) }; document.close() }
    }
}
