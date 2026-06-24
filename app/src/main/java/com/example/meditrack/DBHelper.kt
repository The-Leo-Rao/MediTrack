package com.example.meditrack

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.meditrack.Notification.scheduleAlarm

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
    val time1: Long,
    val time2: Long
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
                time INTEGER,
                timetwo INTEGER
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
    fun getAll(): List<Record>{
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


    fun AddReminder(med: String,dose: String,t1: Long,t2:Long){

        val db=writableDatabase

        val entry= ContentValues().apply{
            put("medicine",med)
            put("time",t1)
            put("timetwo",t2)
            put("dose",dose)
        }

        val id=db.insert("reminders", null, entry)

        scheduleAlarm(
            context,
            id.toInt()* 10 + 1,
            t1,
            med,
            dose
        )

        if(t2.toInt()!=0){
            scheduleAlarm(
                context,
                id.toInt()* 10 + 2,
                t2,
                med,
                dose
            )
        }
    }
    fun getAllRem(): List<Reminder>{
        val reminders = mutableListOf<Reminder>()
        val db=readableDatabase

        val cursor=db.rawQuery("select * from reminders",null)

        with(cursor){
            while(moveToNext()){
                reminders.add(
                    Reminder(
                        id=getInt(getColumnIndexOrThrow("id")),
                        med=getString(getColumnIndexOrThrow("medicine")),
                        time1=getLong(getColumnIndexOrThrow("time")),
                        time2=getLong(getColumnIndexOrThrow("timetwo")),
                        dose=getString(getColumnIndexOrThrow("dose"))
                    )
                )
            }
            close()
        }
        return reminders
    }
    fun delRem(id: Int): Int{
        val db=writableDatabase

        return db.delete(
            "reminders",
            "id=?",
            arrayOf(id.toString())
        )
    }


    fun getAVital(specific: String): List<Vital>{
        val data=mutableListOf<Vital>()
        val db=readableDatabase

        val cursor=db.rawQuery(
            "SELECT * FROM vitalsData WHERE type=?",
            arrayOf(specific)
        )

        while (cursor.moveToNext()) {
            data.add(
                Vital(
                    id = cursor.getInt(0),
                    type = cursor.getString(1),
                    val1 = cursor.getDouble(2),
                    val2=cursor.getDouble(3),
                    unit=cursor.getString(4),
                    timestamp = cursor.getLong(5),
                    note=cursor.getString(6)
                )
            )
        }
        cursor.close()
        return data
    }

    fun setAVital(type: String,val1: Double,val2: Double,unit: String,timestamp: Long,note: String){

        val db=readableDatabase

        val entry= ContentValues().apply{
            put("type",type)
            put("val1",val1)
            put("val2",val2)
            put("unit",unit)
            put("timestamp",timestamp)
            put("note",note)
        }

        db.insert("vitalsData",null,entry)
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