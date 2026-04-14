package com.example.d_linkmobilymanagement

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.d_linkmobilymanagement.ui.navigation.AppNavGraph
import com.example.d_linkmobilymanagement.ui.theme.DlinkMobilyManagementTheme
import com.example.d_linkmobilymanagement.viewmodel.MainViewModel
import com.example.d_linkmobilymanagement.viewmodel.MainViewModelFactory

class MainActivity : AppCompatActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            // Permission granted, will refresh devices automatically via UI or manual refresh
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkPermissions()

        val container = (application as DlinkApp).container

        setContent {
            val vm: MainViewModel = viewModel(
                factory = MainViewModelFactory(
                    container.routerRepository,
                    container.updateRepository,
                    container.networkMonitor,
                    container.selfDeviceStore,
                    container.currentDeviceNetworkInfoProvider
                )
            )
            val uiState by vm.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.appLanguageSettings) {
                val tag = if (!uiState.appLanguageSettings.hasUserChosenLanguage) {
                    "" // Let the system decide
                } else {
                    uiState.appLanguageSettings.selectedLanguageTag ?: ""
                }
                
                val currentAppLocales = AppCompatDelegate.getApplicationLocales()
                val targetLocales = if (tag.isEmpty()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(tag)
                }

                if (currentAppLocales != targetLocales) {
                    AppCompatDelegate.setApplicationLocales(targetLocales)
                }
            }

            DlinkMobilyManagementTheme {
                AppNavGraph(vm)
            }
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}
