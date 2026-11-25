package com.example.permissions

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R

data class AppPermissionsStatus(
    val notificationsGranted: Boolean = true,
    val requiresNotificationPrompt: Boolean = false,
    val networkConnected: Boolean = false,
    val storageFrameworkReady: Boolean = true,
    val driveAuthorized: Boolean = false
) {
    val allCoreGranted: Boolean
        get() = notificationsGranted && driveAuthorized && storageFrameworkReady
}

object AppPermissionsManager {

    private const val DRIVE_BACKUP_CHANNEL_ID = "concord_drive_backup_channel"

    fun evaluateStatus(context: Context, driveAuthorized: Boolean): AppPermissionsStatus {
        val notifGranted = hasNotificationPermission(context)
        val needsPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifGranted
        return AppPermissionsStatus(
            notificationsGranted = notifGranted,
            requiresNotificationPrompt = needsPrompt,
            networkConnected = isNetworkAvailable(context),
            storageFrameworkReady = true,
            driveAuthorized = driveAuthorized
        )
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    fun postDriveBackupNotification(context: Context, title: String, message: String) {
        if (!hasNotificationPermission(context)) return
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    DRIVE_BACKUP_CHANNEL_ID,
                    "Google Drive Cloud Backups",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifies Admin when automatic Google Drive backups finish."
                }
                nm.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(context, DRIVE_BACKUP_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .build()
            nm.notify(2026, notification)
        } catch (_: Exception) {
            // Non-critical notification posting failure should never interrupt backup flow
        }
    }
}
