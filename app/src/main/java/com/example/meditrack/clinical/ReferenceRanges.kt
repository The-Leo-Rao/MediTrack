package com.example.meditrack.clinical

import com.example.meditrack.data.VitalType

object ReferenceRanges {

    data class Thresholds(
        val normalLow: Double?, val normalHigh: Double?,
        val criticalLow: Double? = null, val criticalHigh: Double? = null,
        val normalLow2: Double? = null, val normalHigh2: Double? = null,
        val criticalLow2: Double? = null, val criticalHigh2: Double? = null,
    )

    /** Thresholds for a vital, or null if it has no clinical range (e.g. weight). */
    fun rangesFor(type: VitalType): Thresholds? = when (type) {
        VitalType.HEART_RATE ->
            Thresholds(normalLow = 60.0, normalHigh = 100.0, criticalLow = 40.0, criticalHigh = 130.0)
        VitalType.SPO2 ->
            Thresholds(normalLow = 95.0, normalHigh = null, criticalLow = 90.0, criticalHigh = null)
        VitalType.TEMPERATURE ->
            Thresholds(normalLow = 36.1, normalHigh = 37.5, criticalLow = 35.0, criticalHigh = 39.5)
        VitalType.BLOOD_SUGAR ->
            Thresholds(normalLow = 70.0, normalHigh = 140.0, criticalLow = 54.0, criticalHigh = 250.0)
        VitalType.BLOOD_PRESSURE ->
            Thresholds(
                normalLow = 90.0, normalHigh = 120.0, criticalLow = 90.0, criticalHigh = 180.0,
                normalLow2 = 60.0, normalHigh2 = 80.0, criticalLow2 = 50.0, criticalHigh2 = 120.0,
            )
        VitalType.WEIGHT -> null
    }

    /**
     * Classify a reading. For blood pressure, returns the worse of systolic and
     * diastolic. Vitals with no range (weight) are always [VitalStatus.NORMAL].
     */
    fun classify(type: VitalType, value1: Double, value2: Double? = null): VitalStatus {
        val t = rangesFor(type) ?: return VitalStatus.NORMAL
        val s1 = classifyOne(value1, t.normalLow, t.normalHigh, t.criticalLow, t.criticalHigh)
        val s2 = if (value2 != null)
            classifyOne(value2, t.normalLow2, t.normalHigh2, t.criticalLow2, t.criticalHigh2)
        else VitalStatus.NORMAL
        return maxOf(s1, s2)
    }

    private fun classifyOne(
        value: Double,
        normalLow: Double?, normalHigh: Double?,
        criticalLow: Double?, criticalHigh: Double?,
    ): VitalStatus {
        if (criticalLow != null && value < criticalLow) return VitalStatus.CRITICAL
        if (criticalHigh != null && value > criticalHigh) return VitalStatus.CRITICAL
        if (normalLow != null && value < normalLow) return VitalStatus.WARNING
        if (normalHigh != null && value > normalHigh) return VitalStatus.WARNING
        return VitalStatus.NORMAL
    }
}
