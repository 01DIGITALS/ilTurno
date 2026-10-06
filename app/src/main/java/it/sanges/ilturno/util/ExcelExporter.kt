package it.sanges.ilturno.util

import it.sanges.ilturno.domain.model.WeekSnapshot
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A small, genuine OOXML workbook: typed dates and inline strings, never formulas. */
object ExcelExporter {
    internal fun dateSerial(date: LocalDate): Long {
        val days = ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 31), date)
        return days + if (date >= LocalDate.of(1900, 3, 1)) 1 else 0
    }
    fun write(snapshot: WeekSnapshot, output: OutputStream) {
        ZipOutputStream(output).use { zip ->
            fun part(path: String, xml: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(xml.trimIndent().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            part("[Content_Types].xml", """
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                </Types>
            """)
            part("_rels/.rels", """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
            """)
            part("xl/workbook.xml", """
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="Turni" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
            """)
            part("xl/_rels/workbook.xml.rels", """
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
            """)
            part("xl/styles.xml", """
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <numFmts count="1"><numFmt numFmtId="164" formatCode="dd/mm/yyyy"/></numFmts>
                  <fonts count="3">
                    <font><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><sz val="16"/><name val="Calibri"/></font>
                  </fonts>
                  <fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFE4EFE8"/><bgColor indexed="64"/></patternFill></fill></fills>
                  <borders count="2"><border/><border><left style="thin"/><right style="thin"/><top style="thin"/><bottom style="thin"/></border></borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="4">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyAlignment="1"><alignment vertical="top" wrapText="1"/></xf>
                    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
                    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyAlignment="1"><alignment vertical="top"/></xf>
                    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0"/>
                  </cellXfs>
                  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
                </styleSheet>
            """)
            fun textCell(address: String, value: String, style: Int = 0): String {
                require(value.length <= 32767) { "Excel cell text is too long" }
                return "<c r=\"$address\" s=\"$style\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xmlText(value)}</t></is></c>"
            }
            val withDepartments = snapshot.rows.any { it.departmentName != null }
            val lastColumn = if (withDepartments) "E" else "D"
            val lastRow = snapshot.rows.size + 2
            val rows = buildString {
                append("<row r=\"1\" ht=\"32\" customHeight=\"1\">${textCell("A1", "TURNI · ${snapshot.label}", 3)}</row>")
                append("<row r=\"2\" ht=\"28\" customHeight=\"1\">")
                (listOf("Giorno", "Data", "Pranzo", "Cena") + if (withDepartments) listOf("Reparto") else emptyList()).forEachIndexed { index, heading -> append(textCell("${('A'.code + index).toChar()}2", heading, 1)) }
                append("</row>")
                snapshot.rows.forEachIndexed { index, row ->
                    val number = index + 3
                    val lunch = row.lunch.joinToString(", ")
                    val dinner = row.dinner.joinToString(", ")
                    val height = maxOf(20, ((maxOf(lunch.length, dinner.length, (row.departmentName?.length ?: 0) * 2) / 34) + 1) * 13 + 6)
                    append("<row r=\"$number\" ht=\"$height\" customHeight=\"1\">")
                    append(textCell("A$number", row.date.format(DateTimeFormatter.ofPattern("EEEE", DateUtils.locale))))
                    val serial = dateSerial(row.date)
                    append("<c r=\"B$number\" s=\"2\"><v>$serial</v></c>")
                    append(textCell("C$number", lunch))
                    append(textCell("D$number", dinner))
                    if (withDepartments) append(textCell("E$number", row.departmentName.orEmpty()))
                    append("</row>")
                }
            }
            part("xl/worksheets/sheet1.xml", """
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetPr><pageSetUpPr fitToPage="1"/></sheetPr><dimension ref="A1:$lastColumn$lastRow"/>
                  <sheetViews><sheetView workbookViewId="0"><pane ySplit="2" topLeftCell="A3" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
                  <sheetFormatPr defaultRowHeight="18"/>
                  <cols><col min="1" max="1" width="16" customWidth="1"/><col min="2" max="2" width="14" customWidth="1"/><col min="3" max="4" width="40" customWidth="1"/>${if (withDepartments) "<col min=\"5\" max=\"5\" width=\"24\" customWidth=\"1\"/>" else ""}</cols>
                  <sheetData>$rows</sheetData><autoFilter ref="A2:$lastColumn$lastRow"/><mergeCells count="1"><mergeCell ref="A1:${lastColumn}1"/></mergeCells>
                  <pageMargins left="0.25" right="0.25" top="0.3" bottom="0.3" header="0.15" footer="0.15"/>
                  <pageSetup paperSize="9" orientation="landscape" fitToWidth="1" fitToHeight="1"/>
                </worksheet>
            """)
        }
    }

    private fun xmlText(value: String): String = buildString {
        value.codePoints().forEach { point ->
            when (point) {
                38 -> append("&amp;")
                60 -> append("&lt;")
                62 -> append("&gt;")
                34 -> append("&quot;")
                39 -> append("&apos;")
                else -> if (point == 9 || point == 10 || point == 13 || point in 0x20..0xD7FF || point in 0xE000..0xFFFD || point in 0x10000..0x10FFFF) appendCodePoint(point)
            }
        }
    }
}
