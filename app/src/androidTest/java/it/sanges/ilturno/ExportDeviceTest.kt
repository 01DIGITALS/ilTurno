package it.sanges.ilturno

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import it.sanges.ilturno.data.entity.Employee
import it.sanges.ilturno.data.entity.Department
import it.sanges.ilturno.data.entity.ShiftAssignment
import it.sanges.ilturno.domain.model.Meal
import it.sanges.ilturno.domain.model.WeekSnapshot
import it.sanges.ilturno.util.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.LocalDate

class ExportDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun pdfIsReadableA4AndSharesOnlyItsContentUri() {
        val date = LocalDate.of(2026, 10, 6)
        val people = listOf(Employee(1, "Marco"), Employee(2, "Anna"), Employee(3, "Giulia"))
        val assignments = listOf(ShiftAssignment(date = date, employeeId = 1, meal = Meal.LUNCH),
            ShiftAssignment(date = date, employeeId = 2, meal = Meal.LUNCH), ShiftAssignment(date = date, employeeId = 1, meal = Meal.DINNER))
        val snapshot = WeekSnapshot.create(date, people, assignments)
        val pdf = ShareExporter.generate(context, snapshot, ExportFormat.PDF)
        PdfRenderer(ParcelFileDescriptor.open(pdf.file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertEquals(1, renderer.pageCount)
            renderer.openPage(0).use { page -> assertEquals(842, page.width); assertEquals(595, page.height) }
        }
        val share = ShareExporter.intent(context, pdf)
        assertEquals("application/pdf", share.type)
        assertEquals("content", share.clipData!!.getItemAt(0).uri.scheme)
        assertTrue(share.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val qa = File(context.filesDir, "qa").apply { mkdirs() }
        pdf.file.copyTo(File(qa, snapshot.fileName("pdf")), overwrite = true)
        ShareExporter.generate(context, snapshot, ExportFormat.XLSX).file.copyTo(File(qa, snapshot.fileName("xlsx")), overwrite = true)
    }
    @Test fun longNamesPaginateRatherThanTruncate() {
        val date = LocalDate.of(2026, 10, 6)
        val people = (1L..80L).map { Employee(it, "Persona $it con un nome lungo per la verifica della stampa") }
        val assignments = DateUtils.weekDays(date).flatMap { day -> people.map { ShiftAssignment(date = day, employeeId = it.id, meal = Meal.LUNCH) } }
        val snapshot = WeekSnapshot.create(date, people, assignments)
        val pdf = ShareExporter.generate(context, snapshot, ExportFormat.PDF)
        PdfRenderer(ParcelFileDescriptor.open(pdf.file, ParcelFileDescriptor.MODE_READ_ONLY)).use { assertTrue(it.pageCount > 1) }
        pdf.file.copyTo(File(context.filesDir, "qa/long-names.pdf").apply { parentFile!!.mkdirs() }, overwrite = true)
    }
    @Test fun fourDepartmentsAndTenPeoplePerWeekendServiceFitOneLandscapePage() {
        val monday = LocalDate.of(2026, 10, 5)
        val departments = listOf("Bar", "Cucina", "Pizzeria", "Sala").mapIndexed { index, name -> Department(index + 1L, name) }
        val names = listOf("Anna Rossi", "Bruno Bianchi", "Carla Verdi", "Diego Romano", "Elena Costa", "Fabio Marino", "Giulia Ricci", "Luca Gallo", "Marco Conti", "Sara Greco")
        val people = names.mapIndexed { index, name -> Employee(index + 1L, name) }
        val shifts = (0L..6L).flatMap { offset -> departments.flatMap { department -> Meal.entries.flatMap { meal ->
            people.take(if (offset >= 5) 10 else 3).map { person ->
                ShiftAssignment(date = monday.plusDays(offset), employeeId = person.id, meal = meal, departmentId = department.id)
            }
        } } }
        val snapshot = WeekSnapshot.create(monday, people, shifts, departments)
        val pdf = ShareExporter.generate(context, snapshot, ExportFormat.PDF)
        PdfRenderer(ParcelFileDescriptor.open(pdf.file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertEquals(1, renderer.pageCount)
            renderer.openPage(0).use { assertEquals(842, it.width); assertEquals(595, it.height) }
        }
        val qa = File(context.filesDir, "qa").apply { mkdirs() }
        pdf.file.copyTo(File(qa, "landscape-weekend.pdf"), overwrite = true)
        ShareExporter.generate(context, snapshot, ExportFormat.XLSX).file.copyTo(File(qa, "landscape-weekend.xlsx"), overwrite = true)
    }
}
