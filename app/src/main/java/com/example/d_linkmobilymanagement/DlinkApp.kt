package com.example.d_linkmobilymanagement

import android.app.Application
import com.example.d_linkmobilymanagement.data.AppContainer
import timber.log.Timber

class DlinkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging in debug mode
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize app container
        container = AppContainer(this)
    }
}
