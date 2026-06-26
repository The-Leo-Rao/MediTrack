package com.example.meditrack.sensor

import com.example.meditrack.clinical.ReferenceRanges
import com.example.meditrack.clinical.VitalStatus
import com.example.meditrack.data.VitalEvent
import com.example.meditrack.data.VitalType


class VitalEventDetector(
    private val persist: suspend (VitalEvent) -> Unit,
) {
    private class Episode(
        val type: VitalType,
        val startTimestamp: Long,
        var worstStatus: VitalStatus,
        var extremeValue: Double,
        var extremeValue2: Double?,
        var extremeDeviation: Double,
    )

    private val open = mutableMapOf<VitalType, Episode>()

    suspend fun onReading(reading: SensorReading) {
        val type = reading.type
        val status = ReferenceRanges.classify(type, reading.value1, reading.value2)

        if (status == VitalStatus.NORMAL) {
            // Back in range — close any open episode for this vital.
            open.remove(type)?.let { ep ->
                persist(
                    VitalEvent(
                        type = ep.type,
                        status = ep.worstStatus,
                        startTimestamp = ep.startTimestamp,
                        endTimestamp = reading.timestamp,
                        extremeValue = ep.extremeValue,
                        extremeValue2 = ep.extremeValue2,
                    )
                )
            }
            return
        }

        // Out of range — open or extend the episode, tracking worst severity + extreme.
        val deviation = deviation(type, reading.value1, reading.value2)
        val ep = open[type]
        if (ep == null) {
            open[type] = Episode(
                type = type,
                startTimestamp = reading.timestamp,
                worstStatus = status,
                extremeValue = reading.value1,
                extremeValue2 = reading.value2,
                extremeDeviation = deviation,
            )
        } else {
            ep.worstStatus = maxOf(ep.worstStatus, status)
            if (deviation > ep.extremeDeviation) {
                ep.extremeDeviation = deviation
                ep.extremeValue = reading.value1
                ep.extremeValue2 = reading.value2
            }
        }
    }


    private fun deviation(type: VitalType, value1: Double, value2: Double?): Double {
        val t = ReferenceRanges.rangesFor(type) ?: return 0.0
        val d1 = beyond(value1, t.normalLow, t.normalHigh)
        val d2 = if (value2 != null) beyond(value2, t.normalLow2, t.normalHigh2) else 0.0
        return maxOf(d1, d2)
    }

    private fun beyond(value: Double, low: Double?, high: Double?): Double {
        val belowLow = if (low != null) low - value else 0.0
        val aboveHigh = if (high != null) value - high else 0.0
        return maxOf(0.0, belowLow, aboveHigh)
    }
}
