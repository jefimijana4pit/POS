package com.fourpit.marketpos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fourpit.marketpos.data.seed.PosDatabaseSeeder
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.model.ReportPeriod
import com.fourpit.marketpos.util.PosReportPdfGenerator
import com.fourpit.marketpos.util.ReportAggregator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultiLocationAndPdfTest {

    private lateinit var context: Context
    private lateinit var db: PosDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun seederAndPdfGenerator_produceNonEmptyPdfAcrossLocations() = runTest {
        PosDatabaseSeeder(db).seedIfEmpty()
        val locations = db.catalogLocationDao().getAllLocationsOnce()
        assertTrue(locations.size >= 2)

        val locNames = locations.associate { it.id to it.name }
        val orders = db.salesDao().observeAllOrders().first().map { it.toModel(locNames[it.order.locationId].orEmpty()) }
        val report = ReportAggregator.aggregate(
            orders = orders,
            period = ReportPeriod.ALL_TIME,
            title = "Global Report",
            scopeLabel = "All Branches",
            locationNames = locNames
        )
        assertTrue(report.paidOrderCount > 0)
        assertTrue(report.totalRevenueRsd > 0.0)

        val pdfFile = PosReportPdfGenerator().generateReportPdf(
            context = context,
            report = report,
            businessName = "MarketPOS Test",
            currency = CurrencyCode.RSD,
            eurRate = 117.2
        )
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 0L)
        pdfFile.delete()
    }
}
