package it.sanges.ilturno

import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.Meal
import it.sanges.ilturno.domain.model.WeekSnapshot
import it.sanges.ilturno.util.ExcelExporter
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class ExportTest {
    private val date = LocalDate.of(2026, 10, 6)
    @Test fun snapshotUsesSevenRealDatesPreservesInactiveNamesAndExcludesOtherWeeks() {
        val employees = listOf(Employee(1, "Marco", false, 0), Employee(2, "Anna", true, 1))
        val assignments = listOf(
            ShiftAssignment(date = date, employeeId = 1, meal = Meal.LUNCH),
            ShiftAssignment(date = date, employeeId = 2, meal = Meal.LUNCH),
            ShiftAssignment(date = date.plusWeeks(1), employeeId = 1, meal = Meal.DINNER))
        val snapshot = WeekSnapshot.create(date, employees, assignments)
        assertEquals("Turni_2026-10-05_2026-10-11.pdf", snapshot.fileName("pdf"))
        assertEquals(7, snapshot.rows.size)
        assertEquals(listOf("Anna", "Marco"), snapshot.rows[1].lunch)
        assertTrue(snapshot.rows.all { it.dinner.isEmpty() })
    }
    @Test fun xlsxIsAValidXmlPackageWithTypedDatesAndSafeText() {
        val name = "=SUM(1,2) & Élodie <Rossi>"
        val snapshot = WeekSnapshot.create(date, listOf(Employee(1, name)), listOf(ShiftAssignment(date = date, employeeId = 1, meal = Meal.LUNCH)))
        val output = ByteArrayOutputStream()
        ExcelExporter.write(snapshot, output)
        val parts = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) { parts[entry.name] = zip.readBytes(); entry = zip.nextEntry }
        }
        assertEquals(6, parts.size)
        val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        parts.values.forEach { parser.parse(ByteArrayInputStream(it)) }
        val worksheet = parser.parse(ByteArrayInputStream(parts.getValue("xl/worksheets/sheet1.xml")))
        assertEquals(9, worksheet.getElementsByTagName("row").length)
        assertEquals(0, worksheet.getElementsByTagName("f").length)
        assertTrue(worksheet.documentElement.textContent.contains(name))
        val cells = worksheet.getElementsByTagName("c")
        val dateCell = (0 until cells.length).map { cells.item(it) as org.w3c.dom.Element }.single { it.getAttribute("r") == "B4" }
        assertEquals("2", dateCell.getAttribute("s"))
        assertEquals(ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), date).toString(), dateCell.textContent)
    }
    @Test fun emptyYearBoundaryWeekStillExportsEveryDate() {
        val snapshot = WeekSnapshot.create(LocalDate.of(2027, 1, 1), emptyList(), emptyList())
        assertEquals("28 dicembre 2026 – 3 gennaio 2027", snapshot.label)
        assertEquals("Turni_2026-12-28_2027-01-03.xlsx", snapshot.fileName("xlsx"))
        assertTrue(snapshot.rows.all { it.lunch.isEmpty() && it.dinner.isEmpty() })
    }
    @Test fun excelDatesAccountForItsFictitiousLeapDayIn1900() {
        assertEquals(1L, ExcelExporter.dateSerial(LocalDate.of(1900, 1, 1)))
        assertEquals(59L, ExcelExporter.dateSerial(LocalDate.of(1900, 2, 28)))
        assertEquals(61L, ExcelExporter.dateSerial(LocalDate.of(1900, 3, 1)))
        assertEquals(ChronoUnit.DAYS.between(LocalDate.of(1899, 12, 30), date), ExcelExporter.dateSerial(date))
    }
}
