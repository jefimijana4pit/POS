package com.fourpit.marketpos

import android.app.Application
import com.fourpit.marketpos.di.PosAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MarketPosApplication : Application() {
    lateinit var container: PosAppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = PosAppContainer(this)
        applicationScope.launch {
            container.seeder.seedIfEmpty()
        }
    }
}
