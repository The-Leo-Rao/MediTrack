package com.example.meditrack

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.meditrack.Notification.cancelAlarm
import com.example.meditrack.Notification.scheduleAlarm
import com.example.meditrack.clinical.VitalStatus
import com.example.meditrack.data.VitalEvent
import com.example.meditrack.data.VitalType
import java.util.Calendar

data class Record(
    val id: Int,
    val type: String,
    val data: String?,
    val timestamp: Long
)
data class Reminder(
    val id: Int,
    val med: String,
    val dose: String,
    val hour1: Int,
    val minute1: Int,
    val hour2: Int,
    val minute2: Int
)
data class Vital(
    val id: Int,
    val type: String,
    val val1: Double,
    val val2: Double,
    val unit: String,
    val timestamp: Long,
    val note: String
)

class DBHelper(private val context: Context) :
    SQLiteOpenHelper(
        context,
        "data.db",
        null,
        1
    ){
    override fun onCreate(db: SQLiteDatabase) {
        Log.d("DB", "Creating records")
        db.execSQL(
            """
            CREATE TABLE records(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT,
                data TEXT,
                timestamp INTEGER
            )
            """
        )
        Log.d("DB", "Created records")
        Log.d("DB", "Creating Reminders")
        db.execSQL(
            """CREATE TABLE reminders(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    medicine TEXT,
                    dose TEXT,
                
                    hour1 INTEGER,
                    minute1 INTEGER,
                
                    hour2 INTEGER,
                    minute2 INTEGER
                )"""
        )
        Log.d("DB", "Created Reminders")
        Log.d("DB", "Creating Vitals")
        db.execSQL(
            """CREATE TABLE vitalsData(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT,
                val1 REAL,
                val2 REAL,
                unit TEXT,
                timestamp INTEGER,
                note TEXT
            )"""
        )
        Log.d("DB", "Created Vitals")

        db.execSQL(
            """CREATE TABLE vital_events(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT,
                status TEXT,
                startTimestamp INTEGER,
                endTimestamp INTEGER,
                extremeValue REAL,
                extremeValue2 REAL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldV: Int, newV: Int) {
    }

    fun AddRecord(type: String, data: String?, timestamp: Long){

        val db=writableDatabase
        val entry= ContentValues().apply{
            put("type",type)
            put("data",data)
            put("timestamp",timestamp)
        }

        db.insert("records",null,entry)
    }
    fun getAllRec(): List<Record>{
        val records = mutableListOf<Record>()
        val db=readableDatabase

        val cursor=db.rawQuery("select * from records",null)

        with(cursor){
            while(moveToNext()){
                records.add(
                    Record(
                        id=getInt(getColumnIndexOrThrow("id")),
                        type=getString(getColumnIndexOrThrow("type")),
                        data=getString(getColumnIndexOrThrow("data")),
                        timestamp=getLong(getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
            close()
        }
        return records
    }
    fun delRec(id: Int): Int{
        val db=writableDatabase

        return db.delete(
            "records",
            "id=?",
            arrayOf(id.toString())
        )
    }


    fun AddReminder(
        med: String,
        dose: String,

        hour1: Int,
        minute1: Int,

        hour2: Int,
        minute2: Int
    ){
        fun nextOccurrence(
            hour: Int,
            minute: Int
        ): Long {

            val now = Calendar.getInstance()

            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            return target.timeInMillis
        }

        val db=writableDatabase

        val entry= ContentValues().apply{
            put("medicine",med)
            put("hour1",hour1)
            put("minute1",minute1)
            put("hour2",hour2)
            put("minute2",minute2)
            put("dose",dose)
        }

        val id=db.insert("reminders", null, entry)

        scheduleAlarm(
            context,
            id.toInt()* 10 + 1,
            nextOccurrence(hour1, minute1),
            med,
            dose
        )

        if (hour2 != 0 || minute2 != 0) {

            scheduleAlarm(
                context,
                id.toInt() * 10 + 2,
                nextOccurrence(hour2, minute2),
                med,
                dose
            )
        }
    }
    fun getReminder(id: Int): Reminder? {

        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT * FROM reminders WHERE id=?",
            arrayOf(id.toString())
        )

        var reminder: Reminder? = null

        if (cursor.moveToFirst()) {

            reminder = Reminder(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                med = cursor.getString(cursor.getColumnIndexOrThrow("medicine")),
                dose = cursor.getString(cursor.getColumnIndexOrThrow("dose")),

                hour1 = cursor.getInt(cursor.getColumnIndexOrThrow("hour1")),
                minute1 = cursor.getInt(cursor.getColumnIndexOrThrow("minute1")),

                hour2 = cursor.getInt(cursor.getColumnIndexOrThrow("hour2")),
                minute2 = cursor.getInt(cursor.getColumnIndexOrThrow("minute2"))
            )
        }

        cursor.close()

        return reminder
    }

    fun getAllRem(): List<Reminder> {
        val reminders = mutableListOf<Reminder>()
        val db = readableDatabase

        val cursor = db.rawQuery("SELECT * FROM reminders", null)

        with(cursor) {
            while (moveToNext()) {
                reminders.add(
                    Reminder(
                        id = getInt(getColumnIndexOrThrow("id")),
                        med = getString(getColumnIndexOrThrow("medicine")),
                        dose = getString(getColumnIndexOrThrow("dose")),

                        hour1 = getInt(getColumnIndexOrThrow("hour1")),
                        minute1 = getInt(getColumnIndexOrThrow("minute1")),

                        hour2 = getInt(getColumnIndexOrThrow("hour2")),
                        minute2 = getInt(getColumnIndexOrThrow("minute2"))
                    )
                )
            }
            close()
        }

        return reminders
    }
    fun delRem(id: Int): Int{
        val db=writableDatabase

        cancelAlarm(context, id)
        return db.delete(
            "reminders",
            "id=?",
            arrayOf(id.toString())
        )
    }


    fun getAVital(specific: String): List<Vital>{
        return queryVitals(
            "SELECT * FROM vitalsData WHERE type=? ORDER BY timestamp ASC",
            arrayOf(specific)
        )
    }


    fun getVitalsInRange(type: String, start: Long, end: Long): List<Vital> {
        return queryVitals(
            "SELECT * FROM vitalsData WHERE type=? AND timestamp BETWEEN ? AND ? ORDER BY timestamp ASC",
            arrayOf(type, start.toString(), end.toString())
        )
    }


    fun getLatestVital(type: String): Vital? {
        return queryVitals(
            "SELECT * FROM vitalsData WHERE type=? ORDER BY timestamp DESC LIMIT 1",
            arrayOf(type)
        ).firstOrNull()
    }


    fun getVitalCount(): Int {
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM vitalsData", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count
    }

    private fun queryVitals(sql: String, args: Array<String>): List<Vital> {
        val data = mutableListOf<Vital>()
        val cursor = readableDatabase.rawQuery(sql, args)
        while (cursor.moveToNext()) {
            data.add(
                Vital(
                    id = cursor.getInt(0),
                    type = cursor.getString(1),
                    val1 = cursor.getDouble(2),
                    val2 = cursor.getDouble(3),
                    unit = cursor.getString(4),
                    timestamp = cursor.getLong(5),
                    note = cursor.getString(6) ?: ""
                )
            )
        }
        cursor.close()
        return data
    }

    fun setAVital(type: String,val1: Double,val2: Double,unit: String,timestamp: Long,note: String): Long{

        val db=readableDatabase

        val entry= ContentValues().apply{
            put("type",type)
            put("val1",val1)
            put("val2",val2)
            put("unit",unit)
            put("timestamp",timestamp)
            put("note",note)
        }

        return db.insert("vitalsData",null,entry)
    }

    fun addVitalEvent(
        type: String,
        status: String,
        startTimestamp: Long,
        endTimestamp: Long,
        extremeValue: Double,
        extremeValue2: Double?
    ): Long {
        val db = writableDatabase
        val entry = ContentValues().apply {
            put("type", type)
            put("status", status)
            put("startTimestamp", startTimestamp)
            put("endTimestamp", endTimestamp)
            put("extremeValue", extremeValue)
            put("extremeValue2", extremeValue2)
        }
        return db.insert("vital_events", null, entry)
    }

    fun getRecentEvents(limit: Int): List<VitalEvent> {
        return queryEvents(
            "SELECT * FROM vital_events ORDER BY endTimestamp DESC LIMIT ?",
            arrayOf(limit.toString())
        )
    }

    private fun queryEvents(sql: String, args: Array<String>): List<VitalEvent> {
        val events = mutableListOf<VitalEvent>()
        val cursor = readableDatabase.rawQuery(sql, args)
        while (cursor.moveToNext()) {
            events.add(
                VitalEvent(
                    id = cursor.getLong(0),
                    type = VitalType.valueOf(cursor.getString(1)),
                    status = VitalStatus.valueOf(cursor.getString(2)),
                    startTimestamp = cursor.getLong(3),
                    endTimestamp = cursor.getLong(4),
                    extremeValue = cursor.getDouble(5),
                    extremeValue2 = if (cursor.isNull(6)) null else cursor.getDouble(6),
                )
            )
        }
        cursor.close()
        return events
    }







    fun seedDemoVitals(
        daysBack: Int = 30,
        readingsPerDayPerType: Int = 4
    ) {
        val db = writableDatabase
        writableDatabase.delete("vitalsData", null, null)
        val dayMs = 24L * 60 * 60 * 1000
        val hourMs = 60L * 60 * 1000
        val now = System.currentTimeMillis()

        db.beginTransaction()
        try {
            for (dayOffset in 0 until daysBack) {
                val dayStart = now - (dayOffset * dayMs)

                repeat(readingsPerDayPerType) { slot ->
                    val timestamp =
                        startOfDay(dayStart) +
                                (8L + slot * 4L) * hourMs +
                                (0..59).random() * 60L * 1000L

                    insertVitalRow(
                        type = "HEART RATE",
                        val1 = (60..110).random().toDouble(),
                        val2 = 0.0,
                        unit = "bpm",
                        timestamp = timestamp,
                        note = "Test",
                        db = db
                    )

                    insertVitalRow(
                        type = "WEIGHT",
                        val1 = (68..74).random().toDouble(),
                        val2 = 0.0,
                        unit = "kg",
                        timestamp = timestamp,
                        note = "Test",
                        db = db
                    )

                    insertVitalRow(
                        type = "BLOOD PRESSURE",
                        val1 = (100..150).random().toDouble(),
                        val2 = (60..95).random().toDouble(),
                        unit = "mmHg",
                        timestamp = timestamp + 5 * 60 * 1000L,
                        note = "Test",
                        db = db
                    )

                    insertVitalRow(
                        type = "SPO2",
                        val1 = (92..100).random().toDouble(),
                        val2 = 0.0,
                        unit = "%",
                        timestamp = timestamp + 10 * 60 * 1000L,
                        note = "Test",
                        db = db
                    )

                    insertVitalRow(
                        type = "BODY TEMPERATURE",
                        val1 = ((365..390).random() / 10.0),
                        val2 = 0.0,
                        unit = "°C",
                        timestamp = timestamp + 15 * 60 * 1000L,
                        note = "Test",
                        db = db
                    )

                    insertVitalRow(
                        type = "BLOOD SUGAR",
                        val1 = (70..220).random().toDouble(),
                        val2 = 0.0,
                        unit = "mg/dL",
                        timestamp = timestamp + 20 * 60 * 1000L,
                        note = "Test",
                        db = db
                    )
                }
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insertVitalRow(
        type: String,
        val1: Double,
        val2: Double,
        unit: String,
        timestamp: Long,
        note: String?,
        db: android.database.sqlite.SQLiteDatabase
    ) {
        val values = android.content.ContentValues().apply {
            put("type", type)
            put("val1", val1)
            put("val2", val2)
            put("unit", unit)
            put("timestamp", timestamp)
            put("note", note)
        }

        db.insert("vitalsData", null, values)
    }

    private fun startOfDay(timeInMillis: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            this.timeInMillis = timeInMillis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}