package com.example

import android.app.Application
import com.example.auth.GmailVerificationManager
import com.example.data.local.ConcordDatabase
import com.example.data.remote.CloudSyncRepository
import com.example.data.repository.InventoryRepository
import com.example.export.BackupManager
import com.example.export.GoogleDriveBackupManager
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(application: Application) {
    val database: ConcordDatabase by lazy { ConcordDatabase.getInstance(application) }
    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance(application.getString(R.string.firestore_database_id))
    }
    val cloudSyncRepository: CloudSyncRepository by lazy {
        CloudSyncRepository(firestore, database)
    }
    val sessionPreferences: com.example.data.local.SessionPreferences by lazy {
        com.example.data.local.SessionPreferences(application)
    }
    val repository: InventoryRepository by lazy {
        InventoryRepository(database, cloudSyncRepository)
    }
    val backupManager: BackupManager by lazy { BackupManager(database) }
    val driveBackupManager: GoogleDriveBackupManager by lazy { GoogleDriveBackupManager(application) }
    val gmailVerificationManager: GmailVerificationManager by lazy { GmailVerificationManager(application) }
}

class ConcordGasApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
