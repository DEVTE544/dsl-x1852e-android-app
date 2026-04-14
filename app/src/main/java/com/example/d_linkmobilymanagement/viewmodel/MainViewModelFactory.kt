package com.example.d_linkmobilymanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.d_linkmobilymanagement.data.local.SelfDeviceStore
import com.example.d_linkmobilymanagement.data.network.CurrentDeviceNetworkInfoProvider
import com.example.d_linkmobilymanagement.data.network.NetworkMonitor
import com.example.d_linkmobilymanagement.data.repository.RouterRepository
import com.example.d_linkmobilymanagement.data.repository.update.UpdateRepository

class MainViewModelFactory(
    private val repository: RouterRepository,
    private val updateRepository: UpdateRepository,
    private val networkMonitor: NetworkMonitor,
    private val selfDeviceStore: SelfDeviceStore,
    private val networkInfoProvider: CurrentDeviceNetworkInfoProvider
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, updateRepository, networkMonitor, selfDeviceStore, networkInfoProvider) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, updateRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
