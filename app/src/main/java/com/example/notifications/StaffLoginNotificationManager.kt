package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StaffLoginNotificationItem(
    val eventId: String,
    val staffId: Long,
    val staffName: String,
    val vehicleName: String,
    val loginTimestampMs: Long
)

/**
 * Posts Android system notifications whenever a Staff member signs in to C&A Gas,
 * both locally and when real-time Firestore login events arrive on Admin/Owner devices.
 */
object StaffLoginNotificationManager {
    const val CHANNEL_ID = "ca_gas_staff_login_alerts"
    private const val CHANNEL_NAME = "Staff App Login Alerts"
    private const val CHANNEL_DESCRIPTION = "Instant alerts when delivery staff sign in to C&A Gas"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
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

    fun notifyStaffLogin(
        context: Context,
        staffName: String,
        vehicleName: String,
        loginTimestampMs: Long,
        notificationId: Int = ((loginTimestampMs % Int.MAX_VALUE).toInt()).coerceAtLeast(1000)
    ): Boolean {
        return try {
            ensureNotificationChannel(context)
            if (!hasNotificationPermission(context)) return false

            val timeText = SimpleDateFormat("hh:mm a", Locale.US).format(Date(loginTimestampMs))
            val vehicleSegment = vehicleName.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
            val title = "C&A Gas • Staff Logged In"
            val body = "$staffName$vehicleSegment signed in at $timeText"

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
