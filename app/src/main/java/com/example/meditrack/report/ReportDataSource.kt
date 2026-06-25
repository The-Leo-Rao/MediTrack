package com.example.meditrack.report

import android.content.Context
import com.example.meditrack.DBHelper
import com.example.meditrack.Vital
import com.example.meditrack.clinical.ReferenceRanges
import com.example.meditrack.clinical.VitalStatus
import com.example.meditrack.data.VitalType
import com.example.meditrack.graph.GraphDownsampler
import com.example.meditrack.graph.GraphPoint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Gathers all the data a medical report needs from the app's own stores:
 * SQLite (vitals, events, records, reminders) and the Firestore patient profile.
 * Pure data — no Android UI. Runs off the main thread.
 */
object ReportDataSource {

    suspend fun gather(context: Context, windowDays: Int = 30): ReportContent = withContext(Dispatchers.IO) {
        val db = DBHelper(context)
        val now = System.currentTimeMillis()
        val start = now - windowDays.toLong() * 24 * 60 * 60 * 1000

        val patient = loadPatient()

        val windowEvents = db.getRecentEvents(1000)
            .filter { it.endTimestamp in start..now }

        val summaries = mutableListOf<VitalSummary>()
        val points = LinkedHashMap<VitalType, List<GraphPoint>>()
        for (type in VitalType.entries) {
            val readings = db.getVitalsInRange(type.name, start, now)
            val episodes = windowEvents.count { it.type == type }
            summaries += summarize(type, readings, episodes)
            if (readings.isNotEmpty()) {
                points[type] = GraphDownsampler.minMaxDecimate(readings, 120)
            }
        }

        val records = db.getAllRec().sortedByDescending { it.timestamp }
        val reminders = db.getAllRem()

        ReportContent(
            patient = patient,
            generatedAt = now,
            periodStart = start,
            periodEnd = now,
            summaries = summaries,
            points = points,
            events = windowEvents,
            records = records,
            reminders = reminders,
        )
    }

    private fun summarize(type: VitalType, readings: List<Vital>, episodes: Int): VitalSummary {
        if (readings.isEmpty()) {
            return VitalSummary(
                type = type, count = 0, latest = null, latestStatus = null,
                min1 = null, max1 = null, avg1 = null, min2 = null, max2 = null, avg2 = null,
                episodes = episodes, percentInRange = null, normalRange = normalRangeText(type),
            )
        }
        val latest = readings.maxByOrNull { it.timestamp }
        fun second(v: Vital) = if (type.hasSecondValue) v.val2 else null
        val latestStatus = latest?.let { ReferenceRanges.classify(type, it.val1, second(it)) }
        val normals = readings.count {
            ReferenceRanges.classify(type, it.val1, second(it)) == VitalStatus.NORMAL
        }
        val v1 = readings.map { it.val1 }
        val (min2, max2, avg2) = if (type.hasSecondValue) {
            val v2 = readings.map { it.val2 }
            Triple<Double?, Double?, Double?>(v2.min(), v2.max(), v2.average())
        } else {
            Triple<Double?, Double?, Double?>(null, null, null)
        }
        return VitalSummary(
            type = type,
            count = readings.size,
            latest = latest,
            latestStatus = latestStatus,
            min1 = v1.min(), max1 = v1.max(), avg1 = v1.average(),
            min2 = min2, max2 = max2, avg2 = avg2,
            episodes = episodes,
            percentInRange = normals * 100 / readings.size,
            normalRange = normalRangeText(type),
        )
    }

    /** Reads users/{uid} from Firestore, awaited without the play-services coroutine dep. */
    private suspend fun loadPatient(): PatientInfo {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return PatientInfo("Not provided", "Not provided", "Not provided", "Not provided", "Not provided")

        val doc: DocumentSnapshot? = suspendCancellableCoroutine { cont ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { cont.resumeWith(Result.success(it)) }
                .addOnFailureListener { cont.resumeWith(Result.success(null)) }
        }

        fun field(key: String, default: String): String =
            doc?.getString(key)?.takeIf { it.isNotBlank() } ?: default

        return PatientInfo(
            name = field("name", user.email?.substringBefore("@") ?: "Patient"),
            bloodGroup = field("Blood-Group", "Not provided"),
            allergies = field("Allergies", "None reported"),
            chronic = field("Chronic illnesses", "None reported"),
            emergency = doc?.getString("Emergency")?.takeIf { it.isNotBlank() }?.let { "+91 $it" }
                ?: "Not provided",
        )
    }
}
