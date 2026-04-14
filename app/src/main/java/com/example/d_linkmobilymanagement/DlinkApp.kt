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
        try {
            if (java.lang.Boolean.parseBoolean("true")) { // Always plant in debug builds
                Timber.plant(Timber.DebugTree())
            }
        } catch (_: Exception) {
            // Ignore if BuildConfig not available
        }
        
        // Initialize app container
        container = AppContainer(this)
    }
}
