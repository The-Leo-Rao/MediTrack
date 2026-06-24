package com.example.meditrack.Notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.meditrack.R

class ReminderReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val med =
            intent.getStringExtra("med") ?: ""
        val dose =
            intent.getStringExtra("dose") ?: ""
        context.startService(
            Intent(
                context,
                AlarmService::class.java
            )
        )
        Log.d("ALARM", "ReminderReceiver fired")

        val stopIntent =
            Intent(
                context,
                StopAlarmReceiver::class.java
            )

        val stopPendingIntent =
            PendingIntent.getBroadcast(
                context,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )


        val notification =
            NotificationCompat.Builder(context, "medicine_alarm")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Medicine Reminder")
                .setContentText("Time to take your medication of ${med} of dosage ${dose}")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .addAction(
                    0,
                    "STOP",
                    stopPendingIntent
                )
                .build()
        NotificationManagerCompat
            .from(context)
            .notify(1001, notification)
    }
}