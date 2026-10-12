package com.example.dailymoment.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dailymoment.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "daily_moment"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Daily moment", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Your random daily photo + journal reminder" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_CAPTURE, true)
        }
        val pi = PendingIntent.getActivity(
            context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Time to capture your moment")
            .setContentText("Take a photo and jot down what you learned today.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        try {
            nm.notify(1, notification)
        } catch (_: SecurityException) { /* permission revoked */ }
    }
}
