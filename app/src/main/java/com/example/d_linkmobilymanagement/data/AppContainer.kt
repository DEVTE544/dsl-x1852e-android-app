package com.example.d_linkmobilymanagement.data

import android.content.Context
import com.example.d_linkmobilymanagement.data.local.AppLanguageStore
import com.example.d_linkmobilymanagement.data.local.AppSettingsStore
import com.example.d_linkmobilymanagement.data.local.PersistentDeviceMetaStore
import com.example.d_linkmobilymanagement.data.local.RouterPrefsStore
import com.example.d_linkmobilymanagement.data.local.SelfDeviceStore
import com.example.d_linkmobilymanagement.data.network.ActiveNetworkMonitor
import com.example.d_linkmobilymanagement.data.network.CurrentDeviceNetworkInfoProvider
import com.example.d_linkmobilymanagement.data.network.NetworkMonitor
import com.example.d_linkmobilymanagement.data.network.RealCurrentDeviceNetworkInfoProvider
import com.example.d_linkmobilymanagement.data.remote.RouterHttpClient
import com.example.d_linkmobilymanagement.data.remote.update.GitHubUpdateService
import com.example.d_linkmobilymanagement.data.repository.RealRouterRepository
import com.example.d_linkmobilymanagement.data.repository.RouterRepository
import com.example.d_linkmobilymanagement.data.repository.update.UpdateRepository
import com.example.d_linkmobilymanagement.utils.AppConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val routerPrefsStore by lazy { RouterPrefsStore(appContext) }
    private val deviceMetaStore by lazy { PersistentDeviceMetaStore(appContext) }
    val appSettingsStore by lazy { AppSettingsStore(appContext) }
    val appLanguageStore by lazy { AppLanguageStore(appContext) }
    private val routerHttpClient by lazy { RouterHttpClient() }

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    private val updateRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.GITHUB_API_BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val updateService by lazy {
        updateRetrofit.create(GitHubUpdateService::class.java)
    }

    val updateRepository by lazy {
        UpdateRepository(updateService)
    }

    val selfDeviceStore by lazy { SelfDeviceStore(appContext) }
    val currentDeviceNetworkInfoProvider: CurrentDeviceNetworkInfoProvider by lazy {
        RealCurrentDeviceNetworkInfoProvider(appContext)
    }
    
    val networkMonitor: NetworkMonitor by lazy {
        ActiveNetworkMonitor(appContext)
    }

    val routerRepository: RouterRepository by lazy {
        RealRouterRepository(
            prefsStore = routerPrefsStore,
            deviceMetaStore = deviceMetaStore,
            appSettingsStore = appSettingsStore,
            languageStore = appLanguageStore,
            httpClient = routerHttpClient
        )
    }
}
