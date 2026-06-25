package com.example.meditrack.report

import android.content.Context
import android.graphics.Bitmap
import com.example.meditrack.Record
import com.example.meditrack.Reminder
import com.example.meditrack.data.VitalEvent
import com.example.meditrack.data.VitalType
import com.example.meditrack.screens.formatVital
import com.example.meditrack.screens.toTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

