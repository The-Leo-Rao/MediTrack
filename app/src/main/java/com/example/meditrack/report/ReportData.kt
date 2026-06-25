package com.example.meditrack.report

import com.example.meditrack.Record
import com.example.meditrack.Reminder
import com.example.meditrack.Vital
import com.example.meditrack.clinical.ReferenceRanges
import com.example.meditrack.clinical.VitalStatus
import com.example.meditrack.data.VitalEvent
import com.example.meditrack.data.VitalType
import com.example.meditrack.graph.GraphPoint

/** Patient demographics pulled from the Firestore profile (or sensible fallbacks). */
data class PatientInfo(
    val name: String,
    val bloodGroup: String,
    val allergies: String,
    val chronic: String,
    val emergency: String,
)

/** Per-vital aggregates over the report window — the spine of the summary table. */
data class VitalSummary(
    val type: VitalType,
    val count: Int,
    val latest: Vital?,
    val latestStatus: VitalStatus?,
    val min1: Double?,
    val max1: Double?,
    val avg1: Double?,
    val min2: Double?,
    val max2: Double?,
    val avg2: Double?,
    val episodes: Int,
    val percentInRange: Int?,
    val normalRange: String,
)

/** Everything the PDF renderer needs, gathered once. */
data class ReportContent(
    val patient: PatientInfo,
    val generatedAt: Long,
    val periodStart: Long,
    val periodEnd: Long,
    val summaries: List<VitalSummary>,
    val points: Map<VitalType, List<GraphPoint>>,
    val events: List<VitalEvent>,
    val records: List<Record>,
    val reminders: List<Reminder>,
)

/** Human-readable normal reference range for a vital, e.g. "60–100 bpm" or "90–120 / 60–80 mmHg". */
internal fun normalRangeText(type: VitalType): String {
    val t = ReferenceRanges.rangesFor(type) ?: return "—"
    val unit = type.defaultUnit
    if (type.hasSecondValue) {
        return "${num(t.normalLow)}–${num(t.normalHigh)} / ${num(t.normalLow2)}–${num(t.normalHigh2)} $unit"
    }
    val lo = t.normalLow
    val hi = t.normalHigh
    return when {
        lo != null && hi != null -> "${num(lo)}–${num(hi)} $unit"
        lo != null -> "≥ ${num(lo)} $unit"
        hi != null -> "≤ ${num(hi)} $unit"
        else -> "—"
    }
}

/** Format a numeric value without a trailing ".0" for whole numbers. */
internal fun num(value: Double?): String {
    value ?: return "—"
    return if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format("%.1f", value)
}
