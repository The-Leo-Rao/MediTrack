package com.example.meditrack

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

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
            id.toInt(),
            t1
        )
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

    fun delRec(id: Int): Int{
        val db=writableDatabase

        return db.delete(
            "records",
            "id=?",
            arrayOf(id.toString())
        )
    }

    fun delRem(id: Int): Int{
        val db=writableDatabase

        return db.delete(
            "reminders",
            "id=?",
            arrayOf(id.toString())
        )
    }
}