package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Debt
import com.example.utils.DocumentGenerator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocumentGeneratorTest {

    @Test
    fun testGenerateRegistryCsv_createsValidFileAndFormat() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val debts = listOf(
            Debt(
                id = "1",
                creditorName = "Сбербанк; ПАО",
                contractDate = "10.02.2023",
                factAmount = 100000.0,
                penaltyAmount = 15000.0,
                insuranceAmount = 5000.0,
                planAmount = 80000.0,
                status = "В работе",
                isMortgage = false
            ),
            Debt(
                id = "2",
                creditorName = "Тинькофф",
                contractDate = "15.03.2023",
                factAmount = 50000.0,
                penaltyAmount = 10000.0,
                insuranceAmount = 0.0,
                planAmount = 40000.0,
                status = "Ипотека (Иммунитет)",
                isMortgage = true
            )
        )

        DocumentGenerator.generateRegistryCsv(context, debts)
        val docsDir = File(context.cacheDir, "docs")
        assertTrue(docsDir.exists())

        val csvFiles = docsDir.listFiles { _, name -> name.endsWith(".csv") }
        assertNotNull(csvFiles)
        assertTrue(csvFiles!!.isNotEmpty())

        val latestFile = csvFiles!!.maxByOrNull { it.lastModified() }
        assertNotNull(latestFile)

        val content = latestFile!!.readText(Charsets.UTF_8)
        // Verify CSV header
        assertTrue(content.contains("Кредитор;Договор;Требование (Факт);К отмене (Штрафы);План (Законный долг);Страховка;Ипотека;Статус"))
        // Verify semicolons in creditor name are sanitized to comma
        assertTrue(content.contains("Сбербанк, ПАО"))
        assertTrue(content.contains("Тинькофф"))
        // Verify numbers formatted properly
        assertTrue(content.contains("100000.00"))
        assertTrue(content.contains("80000.00"))
        // Verify mortgage flag
        assertTrue(content.contains("Да"))
        assertTrue(content.contains("Нет"))
    }

    @Test
    fun testGenerateRegistryCsv_emptyList() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val debts = emptyList<Debt>()

        DocumentGenerator.generateRegistryCsv(context, debts)
        val docsDir = File(context.cacheDir, "docs")
        assertTrue(docsDir.exists())

        val csvFiles = docsDir.listFiles { _, name -> name.endsWith(".csv") }
        assertNotNull(csvFiles)
        val latestFile = csvFiles!!.maxByOrNull { it.lastModified() }
        assertNotNull(latestFile)

        val content = latestFile!!.readText(Charsets.UTF_8)
        assertTrue(content.contains("Кредитор;Договор;Требование (Факт)"))
    }
}
