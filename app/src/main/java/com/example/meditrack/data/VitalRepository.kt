package com.example.meditrack.data

import android.util.Log
import com.example.meditrack.DBHelper
import com.example.meditrack.Vital
import com.example.meditrack.clinical.ReferenceRanges
import com.example.meditrack.clinical.VitalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * The clean access layer between the UI/pipeline and the SQLite store ([DBHelper]).
 *
 * MediTrack uses a plain SQLiteOpenHelper rather than Room, so there is no built-in
 * "live query" support. We recreate just enough of it: every write bumps a
 * [revision] counter, and the observe* Flows re-run their (cheap, indexed-by-type)
 * query whenever the revision changes. [distinctUntilChanged] keeps the UI from
 * recomposing when a query result is unchanged. All DB work happens on
 * [Dispatchers.IO].
 *
 * This is the seam the rest of the app talks to — swap the storage behind it
 * without touching the ViewModel or UI.
 */
class VitalRepository(private val db: DBHelper) {

    /** Incremented on every write; the observe* Flows re-query on each change. */
    private val revision = MutableStateFlow(0L)

    private fun bump() { revision.value = revision.value + 1 }

    // ── Writes ─────────────────────────────────────────────────────────────────

    suspend fun addReading(
        type: VitalType,
        value1: Double,
        value2: Double? = null,
        note: String? = null,
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val id = db.setAVital(
            type.name,
            value1,
            if (type.hasSecondValue) value2 ?: 0.0 else 0.0,
            type.defaultUnit,
            now,
            note = note ?: "",
        )

        val status = ReferenceRanges.classify(type, value1, value2)
        if (status != VitalStatus.NORMAL) {
            db.addVitalEvent(type.name, status.name, now, now, value1, value2)
        }

        bump()
        id
    }

    suspend fun addEvent(event: VitalEvent): Long = withContext(Dispatchers.IO) {
        val id = db.addVitalEvent(
            event.type.name,
            event.status.name,
            event.startTimestamp,
            event.endTimestamp,
            event.extremeValue,
            event.extremeValue2,
        )
        bump()
        id
    }

    // ── Live (re-querying) reads ────────────────────────────────────────────────

    fun observeByType(type: VitalType): Flow<List<Vital>> =
        revision.map { withContext(Dispatchers.IO) { db.getAVital(type.name) } }
            .distinctUntilChanged()

    fun observeByTypeInRange(type: VitalType, startMillis: Long, endMillis: Long): Flow<List<Vital>> =
        revision.map { withContext(Dispatchers.IO) { db.getVitalsInRange(type.name, startMillis, endMillis) } }
            .distinctUntilChanged()

    fun observeLatest(type: VitalType): Flow<Vital?> =
        revision.map {
            val result = withContext(Dispatchers.IO) { db.getLatestVital(type.name) }
            Log.d("MEDITRACK", "observeLatest $type → ${result?.val1} ts=${result?.timestamp}")
            result
        }.distinctUntilChanged()

    fun observeCount(): Flow<Int> =
        revision.map { withContext(Dispatchers.IO) { db.getVitalCount() } }
            .distinctUntilChanged()

    fun observeEvents(limit: Int = 100): Flow<List<VitalEvent>> =
        revision.map { withContext(Dispatchers.IO) { db.getRecentEvents(limit) } }
            .distinctUntilChanged()

    suspend fun seedTestData() = withContext(Dispatchers.IO) {
        db.seedDemoVitals()
        Log.d("MEDITRACK", "seed done, bumping")
        bump()
        Log.d("MEDITRACK", "seed bump done, revision=${revision.value}")
    }
}
