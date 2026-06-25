package com.example.meditrack.sensor

import com.example.meditrack.data.VitalType

/**
 * A single live reading as it comes off a sensor — purely in-memory.
 *
 * Deliberately separate from the persisted reading row: sensors emit many readings
 * per second and we keep the fast live data decoupled from the database shape.
 */
data class SensorReading(
    val type: VitalType,
    val value1: Double,
    val value2: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
