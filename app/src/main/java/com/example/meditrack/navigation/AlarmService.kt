package com.example.meditrack.navigation

import android.app.Service
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager

class AlarmService : Service() {

    private var ringtone: Ringtone? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val uri =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            )

        ringtone =
            RingtoneManager.getRingtone(this, uri)

        ringtone?.play()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ringtone?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}