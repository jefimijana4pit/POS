package com.example.marketpos

import android.app.Application
import com.example.marketpos.di.PosAppContainer

class MarketPosApplication : Application() {
    val appContainer: PosAppContainer by lazy {
        PosAppContainer(this)
    }
}
