package com.example.sellerhelperebay

import android.app.Application
import com.example.sellerhelperebay.data.AppContainer

class SellerHelperApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
