package com.suresh.finance.notify

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
import com.suresh.finance.MainActivity
import com.suresh.finance.R

object Notifications {
    const val CHANNEL_DUES = "dues"
    const val CHANNEL_BUDGET = "budget"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DUES, "Bills, EMIs and rent", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders before bills, loan payments and rent collection are due"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_BUDGET, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Alerts when spending gets close to or over a budget"
            },
        )
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show(context: Context, channel: String, key: String, title: String, text: String) {
        if (!canNotify(context)) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the call.
        }
    }
}
