package com.example.marketpos.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.marketpos.i18n.EnglishStrings
import com.example.marketpos.i18n.SerbianLatinStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.DefaultLocations
import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.VenueMode
import com.example.marketpos.model.WorkerProductSoldStat
import com.example.marketpos.model.WorkerReportRow
import com.example.marketpos.util.PdfRenderBackend
import com.example.marketpos.util.PosReportPdfGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultiLocationAndPdfTest {

    private lateinit var database: PosDatabase
    private lateinit var catalogDao: CatalogLocationDao
    private lateinit var tableOrderDao: TableOrderDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        catalogDao = database.catalogLocationDao()
        tableOrderDao = database.tableOrderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun locationsAndProducts_areScopedIndependentlyAcrossMultipleMarketsAndBars() = runBlocking {
        val locations = listOf(
            VenueLocationEntity(DefaultLocations.MARKET_CENTRAL_ID, "Market Centar", "Adresa 1", VenueMode.MARKET.name, 0),
            VenueLocationEntity(DefaultLocations.MARKET_NBG_ID, "Market NBG", "Adresa 2", VenueMode.MARKET.name, 1),
            VenueLocationEntity(DefaultLocations.BAR_CENTER_ID, "Bar Korzo", "Adresa 3", VenueMode.BAR_RESTAURANT.name, 2),
            VenueLocationEntity(DefaultLocations.BAR_RIVER_ID, "Restoran Reka", "Adresa 4", VenueMode.BAR_RESTAURANT.name, 3)
        )
        catalogDao.insertLocations(locations)

        catalogDao.insertProducts(
            listOf(
                ProductEntity("pm1", "SKU1", "Hleb", "Bread", ProductCategory.BAKERY.name, 75.0, "kom", "pcs", 10, 3, true, VenueMode.MARKET.name, DefaultLocations.MARKET_CENTRAL_ID),
                ProductEntity("pm2", "SKU2", "Mleko", "Milk", ProductCategory.DAIRY.name, 145.0, "kom", "pcs", 20, 5, true, VenueMode.MARKET.name, DefaultLocations.MARKET_NBG_ID),
                ProductEntity("pb1", "BAR1", "Espreso", "Espresso", ProductCategory.COFFEE_TEA.name, 220.0, "kom", "pcs", 30, 5, true, VenueMode.BAR_RESTAURANT.name, DefaultLocations.BAR_CENTER_ID),
                ProductEntity("pb2", "BAR2", "Pasta", "Pasta", ProductCategory.MAIN_DISHES.name, 890.0, "kom", "pcs", 12, 3, true, VenueMode.BAR_RESTAURANT.name, DefaultLocations.BAR_RIVER_ID)
            )
        )

        tableOrderDao.insertTables(
            listOf(
                BarTableEntity("tb1", "Sto 1", "Sala", 4, sortOrder = 0, locationId = DefaultLocations.BAR_CENTER_ID),
                BarTableEntity("tb2", "Terasa 1", "Terasa", 6, sortOrder = 1, locationId = DefaultLocations.BAR_RIVER_ID)
            )
        )

        assertEquals(4, catalogDao.countLocations())
        assertEquals(1, catalogDao.countProductsByLocation(DefaultLocations.MARKET_CENTRAL_ID))
        assertEquals(1, catalogDao.countProductsByLocation(DefaultLocations.MARKET_NBG_ID))
        assertEquals(1, tableOrderDao.countTablesByLocation(DefaultLocations.BAR_CENTER_ID))
        assertEquals(1, tableOrderDao.countTablesByLocation(DefaultLocations.BAR_RIVER_ID))

        val savedLocations = catalogDao.observeLocations().first()
        assertEquals(listOf("Market Centar", "Market NBG", "Bar Korzo", "Restoran Reka"), savedLocations.map { it.name })
    }

    @Test
    fun pdfGenerator_rendersMultiPageReportAndBuildsCleanFilename() {
        val now = System.currentTimeMillis()
        val loc = VenueLocation(
            id = DefaultLocations.BAR_CENTER_ID,
            name = "Kafe Bar Korzo",
            address = "Knez Mihailova 24, Beograd",
            venueMode = VenueMode.BAR_RESTAURANT,
            sortOrder = 2
        )

        // Create 24 workers to trigger multi-page table pagination
        val rows = (1..24).map { idx ->
            WorkerReportRow(
                workerId = "w_$idx",
                workerName = "Radnik $idx",
                finishedBillsCount = 4 + idx,
                cancelledOrdersCount = idx % 3,
                cancelledItemsCount = idx % 4,
                tablesHandledCount = 3 + (idx % 5),
                totalUnitsSold = 12 + idx,
                revenueRsd = 3500.0 * idx,
                productsSold = listOf(
                    WorkerProductSoldStat("p1", "Espreso kafa", "Espresso Coffee", 5 + idx, 1500.0),
                    WorkerProductSoldStat("p2", "Ceđena pomorandža", "Fresh Orange Juice", 4, 1400.0),
                    WorkerProductSoldStat("p3", "Craft IPA pivo", "Craft IPA Beer", 3, 1200.0)
                )
            )
        }

        val summary = LocationReportSummary(
            location = loc,
            isGlobal = false,
            periodType = ReportPeriodType.DATE_RANGE,
            startMillis = now - 7 * 86_400_000L,
            endMillis = now,
            generatedAtMillis = now,
            totalRevenueRsd = rows.sumOf { it.revenueRsd },
            cashRevenueRsd = rows.sumOf { it.revenueRsd } * 0.6,
            cardRevenueRsd = rows.sumOf { it.revenueRsd } * 0.4,
            finishedBillsCount = rows.sumOf { it.finishedBillsCount },
            cancelledOrdersCount = rows.sumOf { it.cancelledOrdersCount },
            tablesHandledCount = 12,
            totalUnitsSold = rows.sumOf { it.totalUnitsSold },
            workerRows = rows
        )

        val srBackend = RecordingPdfBackend()
        PosReportPdfGenerator.renderReport(
            backend = srBackend,
            summary = summary,
            strings = SerbianLatinStrings,
            settings = AppSettings(language = AppLanguage.SERBIAN_LATIN)
        )
        assertTrue("Expected multi-page PDF rendering for 24 workers", srBackend.startedPages >= 2)
        assertEquals(srBackend.startedPages, srBackend.finishedPages)

        val enBackend = RecordingPdfBackend()
        PosReportPdfGenerator.renderReport(
            backend = enBackend,
            summary = summary.copy(isGlobal = true, location = null, periodType = ReportPeriodType.MONTHLY),
            strings = EnglishStrings,
            settings = AppSettings(language = AppLanguage.ENGLISH)
        )
        assertTrue("Expected multi-page English global report", enBackend.startedPages >= 2)
        assertEquals(enBackend.startedPages, enBackend.finishedPages)

        val fileName = PosReportPdfGenerator.buildFileName(summary)
        assertTrue(fileName.startsWith("MarketPOS_Kafe_Bar_Korzo_"))
        assertTrue(fileName.endsWith(".pdf"))

        val globalFileName = PosReportPdfGenerator.buildFileName(
            summary.copy(isGlobal = true, location = null, periodType = ReportPeriodType.MONTHLY)
        )
        assertTrue(globalFileName.startsWith("MarketPOS_Global_All_Locations_"))
        assertTrue(globalFileName.endsWith(".pdf"))
    }

    private class RecordingPdfBackend : PdfRenderBackend {
        var startedPages = 0
        var finishedPages = 0
        private var currentBitmap: Bitmap? = null

        override fun startPage(width: Int, height: Int, pageNumber: Int): Canvas {
            startedPages++
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            currentBitmap = bmp
            return Canvas(bmp)
        }

        override fun finishPage(pageNumber: Int) {
            finishedPages++
            currentBitmap?.recycle()
            currentBitmap = null
        }

        override fun finishDocument(): ByteArray = byteArrayOf(0x25, 0x50, 0x44, 0x46)
    }
}
