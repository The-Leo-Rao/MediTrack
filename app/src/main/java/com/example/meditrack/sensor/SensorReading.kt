package com.example.meditrack.sensor

import com.example.meditrack.data.VitalType


data class SensorReading(
    val type: VitalType,
    val value1: Double,
    val value2: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
