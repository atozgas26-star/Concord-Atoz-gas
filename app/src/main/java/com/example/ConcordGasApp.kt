package com.example

import android.app.Application
import com.example.data.local.ConcordDatabase
import com.example.data.repository.InventoryRepository
import com.example.export.BackupManager

class AppContainer(application: Application) {
    val database: ConcordDatabase by lazy { ConcordDatabase.getInstance(application) }
    val repository: InventoryRepository by lazy { InventoryRepository(database) }
    val backupManager: BackupManager by lazy { BackupManager(database) }
}

class ConcordGasApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
