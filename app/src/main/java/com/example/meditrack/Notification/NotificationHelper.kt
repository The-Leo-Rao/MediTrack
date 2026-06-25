package com.example.meditrack.Notification

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SmsManager
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.location.Priority

class NotificationHelper(private val context: Context){

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun CallSOS(){
        Log.d("SOS", "Started")
        val uid = FirebaseAuth.getInstance().currentUser?.uid?: return
        val client = LocationServices
            .getFusedLocationProviderClient(context)
        val smsManager = SmsManager.getDefault()

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener {
                doc->

                val phone= (("+91" + (doc.getString("Emergency"))) ?: return@addOnSuccessListener)
                Log.d("SOS", "Phone = $phone")

                client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->
                    Log.d("SOS", "Location = $location")
                    if (location != null) {
                        val lat = location.latitude
                        val lon = location.longitude

                        Log.d("SOS", "Sending SMS")
                        smsManager.sendTextMessage(
                            phone,
                            null,
                            "EMERGENCY! My location: https://maps.google.com/?q=$lat,$lon",
                            null,
                            null
                        )
                    }
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel() {

        val channel = NotificationChannel(
            "medicine_alarm",
            "Medicine Alarm",
            NotificationManager.IMPORTANCE_HIGH
        )

        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)
    }
}


@SuppressLint("ScheduleExactAlarm")
fun scheduleAlarm(
    context: Context,
    reminderId: Int,
    triggerTime: Long,
    med: String,
    dose: String
) {
    val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE)
                as AlarmManager

    val intent = Intent(context, ReminderReceiver::class.java)
        .putExtra("id", reminderId)
        .putExtra("med",med)
        .putExtra("dose",dose)

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        reminderId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        triggerTime,
        pendingIntent
    )
}

fun cancelAlarm(
    context: Context,
    reminderId: Int
) {

    val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE)
                as AlarmManager

    for (alarmNo in 1..2) {

        val alarmId = reminderId * 10 + alarmNo

        val intent = Intent(
            context,
            ReminderReceiver::class.java
        )

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        alarmManager.cancel(pendingIntent)
    }
}
