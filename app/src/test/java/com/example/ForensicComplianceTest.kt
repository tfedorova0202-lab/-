package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Debt
import com.example.data.MessageEntity
import com.example.ui.SecureUiHelper
import com.example.utils.DocumentGenerator
import com.example.utils.UserPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers
import java.io.File
import java.io.OutputStream

@Implements(PdfDocument::class)
class ForensicShadowPdfDocument {
    companion object {
        var totalOpened = 0
        var totalClosed = 0
        val pageCountsByDoc = mutableListOf<Int>()

        fun reset() {
            totalOpened = 0
            totalClosed = 0
            pageCountsByDoc.clear()
        }
    }

    private var closed = false
    private var currentPage: PdfDocument.Page? = null
    private var finishedPages = 0

    @Implementation
    fun __constructor__() {
        totalOpened++
    }

    @Implementation
    fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
        check(!closed) { "document is closed!" }
        check(currentPage == null) { "current page not finished!" }
        val bitmap = Bitmap.createBitmap(
            pageInfo.pageWidth.coerceAtLeast(1),
            pageInfo.pageHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val page = ReflectionHelpers.callConstructor(
            PdfDocument.Page::class.java,
            ReflectionHelpers.ClassParameter.from(Canvas::class.java, canvas),
            ReflectionHelpers.ClassParameter.from(PdfDocument.PageInfo::class.java, pageInfo)
        )
        currentPage = page
        return page
    }

    @Implementation
    fun finishPage(page: PdfDocument.Page) {
        check(!closed) { "document is closed!" }
        check(currentPage === page) { "invalid page finished!" }
        currentPage = null
        finishedPages++
    }

    @Implementation
    fun writeTo(out: OutputStream) {
        check(!closed) { "document is closed!" }
        check(currentPage == null) { "cannot write while page is open!" }
        val payload = "%PDF-1.4\n% pages=$finishedPages\n1 0 obj<<>>endobj\n%%EOF\n".repeat(12)
        out.write(payload.toByteArray(Charsets.US_ASCII))
        pageCountsByDoc.add(finishedPages)
    }

    @Implementation
    fun close() {
        if (!closed) {
            closed = true
            totalClosed++
        }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [ForensicShadowPdfDocument::class])
class ForensicComplianceTest {

    @Test
    fun testSecureWipeAllTables_erasesCanaryBytesFromSqliteAndWal(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "forensic_wipe_test_db"
        context.deleteDatabase(dbName)

        val canary = "CANARY_DEBTOR_9941827_IVANOV_SECRET"
        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()

        try {
            db.debtDao().insertDebt(
                Debt(
                    id = "canary-1",
                    creditorName = canary,
                    contractDate = "12.01.2026",
                    factAmount = 150000.0,
                    penaltyAmount = 30000.0,
                    planAmount = 120000.0
                )
            )
            db.messageDao().insertMessage(
                MessageEntity(id = "msg-1", text = "Мой долг $canary", isUser = true, timestamp = 1L)
            )

            // Confirm canary is persisted on disk before wipe
            val dbFiles = listOf(
                context.getDatabasePath(dbName),
                File(context.getDatabasePath(dbName).path + "-wal"),
                File(context.getDatabasePath(dbName).path + "-shm")
            )
            val preWipePresent = dbFiles.any { f ->
                f.exists() && f.readBytes().toString(Charsets.ISO_8859_1).contains(canary)
            }
            assertTrue("Canary string must exist in SQLite/WAL before wipe", preWipePresent)

            // Run forensic wipe (PRAGMA secure_delete + DELETE + VACUUM + WAL TRUNCATE)
            db.secureWipeAllTables()
        } finally {
            db.close()
        }

        val postWipeFiles = listOf(
            context.getDatabasePath(dbName),
            File(context.getDatabasePath(dbName).path + "-wal"),
            File(context.getDatabasePath(dbName).path + "-shm")
        )
        for (file in postWipeFiles) {
            if (file.exists()) {
                val raw = file.readBytes().toString(Charsets.ISO_8859_1)
                assertFalse(
                    "Canary string MUST NOT survive in ${file.name} after secureWipeAllTables()",
                    raw.contains(canary)
                )
            }
        }
        context.deleteDatabase(dbName)
        Unit
    }

    @Test
    fun testAllPdfTemplates_generateValidFilesStrictlyInDocsCacheAndWrapLongCyrillic() {
        ForensicShadowPdfDocument.reset()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferences(context)
        prefs.clearAllUserData()
        prefs.saveProfile(
            name = "Константинопольский Александр Александрович-Длиннофамильный",
            address = "119019, г. Москва, ул. Очень Длинная Улица Для Проверки Переноса Строк, д. 123, корп. 4, стр. 5, кв. 678",
            phone = "+7 (999) 123-45-67"
        )

        val longDebt = Debt(
            id = "debt-long/id?with*chars",
            creditorName = "Общество с ограниченной ответственностью Микрофинансовая компания «Очень Длинное Наименование Кредитора Для Проверки Переноса В PDF»",
            contractDate = "№ 99887766/2025-КД от 15.05.2025",
            factAmount = 250000.0,
            penaltyAmount = 80000.0,
            insuranceAmount = 20000.0,
            planAmount = 150000.0,
            status = "В работе",
            isMortgage = false
        )

        val manyDebts = (1..25).map { idx ->
            longDebt.copy(id = "d-$idx", creditorName = "Кредитор №$idx ${longDebt.creditorName}")
        }

        val generatedUris = listOf(
            DocumentGenerator.generateObjectionPdf(context, longDebt),
            DocumentGenerator.generateAntiCollectorPdf(context, longDebt),
            DocumentGenerator.generateRefusalOfInteractionPdf(context, longDebt),
            DocumentGenerator.generateInsuranceReturnPdf(context, longDebt),
            DocumentGenerator.generateLackOfFundsPdf(context, longDebt),
            DocumentGenerator.generateRestructuringPdf(context, longDebt),
            DocumentGenerator.generateSavingsPdf(context, 100000.0, 180000.0, 100, 200000.0, 80000.0),
            DocumentGenerator.generateLivingWagePreservationPdf(context, longDebt),
            DocumentGenerator.generateInstallmentPlanPdf(context, longDebt),
            DocumentGenerator.generateStatuteOfLimitationsPdf(context, longDebt),
            DocumentGenerator.generateMortgageShieldSettlementPdf(context, longDebt),
            DocumentGenerator.generateLegalStrategiesMemoPdf(context),
            DocumentGenerator.generateAnalysisReportPdf(
                context,
                (1..60).joinToString("\n") { i ->
                    "$i. Правовой анализ договора и расчёт снижения неустойки по ст. 333 ГК РФ и ч. 23 ст. 5 ФЗ № 353-ФЗ."
                }
            ),
            DocumentGenerator.generateRegistryPdf(context, manyDebts)
        )

        // All 14 must return valid FileProvider URIs (matching provider_paths.xml docs/)
        assertEquals(14, generatedUris.size)
        generatedUris.forEachIndexed { idx, uri ->
            assertNotNull("PDF #$idx must return a non-null FileProvider URI", uri)
            assertTrue(
                "URI must point through shared_docs FileProvider path: $uri",
                uri.toString().contains("/shared_docs/")
            )
        }

        // All opened PdfDocuments must be cleanly closed (zero resource leaks)
        assertEquals(14, ForensicShadowPdfDocument.totalOpened)
        assertEquals(14, ForensicShadowPdfDocument.totalClosed)
        // Long analysis report (#12) and 25-row registry (#13) must paginate to >= 2 pages
        assertTrue(
            "Long analysis report must paginate across >= 2 pages, got ${ForensicShadowPdfDocument.pageCountsByDoc[12]}",
            ForensicShadowPdfDocument.pageCountsByDoc[12] >= 2
        )
        assertTrue(
            "25-row registry PDF must paginate across >= 2 pages, got ${ForensicShadowPdfDocument.pageCountsByDoc[13]}",
            ForensicShadowPdfDocument.pageCountsByDoc[13] >= 2
        )

        val docsDir = File(context.cacheDir, "docs")
        assertTrue("docs cache dir must exist", docsDir.exists())
        val pdfFiles = docsDir.listFiles { _, name -> name.endsWith(".pdf") }.orEmpty()
        assertEquals("All 14 PDF generators must write valid files into cache/docs/", 14, pdfFiles.size)

        // Verify zero stray PDFs written to cacheDir root
        val strayRootPdfs = context.cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".pdf") }.orEmpty()
        assertEquals("No PDFs should be written outside cache/docs/", 0, strayRootPdfs.size)

        for (pdf in pdfFiles) {
            val bytes = pdf.readBytes()
            assertTrue("${pdf.name} must be non-empty PDF", bytes.size > 400)
            val header = bytes.copyOfRange(0, 5).toString(Charsets.US_ASCII)
            assertEquals("${pdf.name} must start with %PDF-", "%PDF-", header)
        }

        // Verify clearAllUserData wipes all generated PDFs
        prefs.clearAllUserData()
        assertFalse("cache/docs/ must be removed by clearAllUserData()", docsDir.exists())
    }

    @Test
    fun testSecureUiHelper_enforcesSecurePolicyForIncognitoOrPin() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferences(context)
        prefs.clearAllUserData()

        // Incognito is true by default -> SecureOn
        assertEquals(SecureFlagPolicy.SecureOn, SecureUiHelper.securePolicy(context))

        // Disable incognito, no PIN -> Inherit
        prefs.setIncognitoMode(false)
        assertEquals(SecureFlagPolicy.Inherit, SecureUiHelper.securePolicy(context))

        // Set PIN while incognito is off -> SecureOn
        prefs.savePin("2468")
        assertEquals(SecureFlagPolicy.SecureOn, SecureUiHelper.securePolicy(context))

        prefs.clearAllUserData()
    }
}
