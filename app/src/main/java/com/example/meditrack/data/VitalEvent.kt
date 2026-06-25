package com.example.meditrack.data

import com.example.meditrack.clinical.VitalStatus

/**
 * One abnormal excursion = one row in the "vital_events" table.
 *
 * A doctor cares about *episodes*, not every out-of-range sample: "HR was critical
 * (peak 148) from 14:32:10 to 14:32:50" is one event, not 40 rows. This is the
 * closed, summarized episode produced by [com.example.meditrack.sensor.VitalEventDetector]
 * when a vital leaves its normal range and later returns.
 */
data class VitalEvent(
    val id: Long = 0,
    val type: VitalType,
    /** Worst severity reached during the excursion (WARNING or CRITICAL). */
    val status: VitalStatus,
    val startTimestamp: Long,
    val endTimestamp: Long,
    /** The most extreme primary reading during the excursion (the peak/trough). */
    val extremeValue: Double,
    /** The matching second value at the extreme (diastolic for BP), if any. */
    val extremeValue2: Double? = null,
) {
    /** Episode length in millis — handy for "for 40 s" style summaries. */
    val durationMillis: Long get() = endTimestamp - startTimestamp
}
