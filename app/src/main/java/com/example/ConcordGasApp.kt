package com.example

import android.app.Application
import com.example.auth.GmailVerificationManager
import com.example.data.local.ConcordDatabase
import com.example.data.remote.CloudSyncRepository
import com.example.data.repository.InventoryRepository
import com.example.export.BackupManager
import com.example.export.GoogleDriveBackupManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    /**
     * Pre-initializes Room, Firestore, Auth, and managers off the main thread so
     * MainActivity's first Compose frame never stalls SurfaceSyncGroup (>1000ms).
     */
    fun warmUp() {
        runCatching { sessionPreferences }
        runCatching { Firebase.auth }
        runCatching { database }
        runCatching { firestore }
        runCatching { cloudSyncRepository }
        runCatching { repository }
        runCatching { backupManager }
        runCatching { driveBackupManager }
        runCatching { gmailVerificationManager }
    }
}

class ConcordGasApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            // Give Activity.onCreate() + Frame 1 Choreographer vsync priority before initializing SDKs
            delay(150)
            container.warmUp()
        }
    }
}
