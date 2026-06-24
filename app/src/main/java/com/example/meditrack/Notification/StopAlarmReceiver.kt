package com.example.meditrack.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class StopAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        context.stopService(
            Intent(
                context,
                AlarmService::class.java
            )
        )

        NotificationManagerCompat
            .from(context)
            .cancel(1001)
    }
}