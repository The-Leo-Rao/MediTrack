package com.example.meditrack.sensor

import com.example.meditrack.data.VitalType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class SimulatedVitalSource(
    private val timeScale: Double = 1.0,
) : VitalSensorSource {

    @Volatile
    private var connected = false

    override fun connect() { connected = true }

    override fun disconnect() { connected = false }

    override fun stream(type: VitalType): Flow<SensorReading> = flow {
        val profile = profileFor(type)
        var current = profile.start
        // Blood pressure carries a second value (diastolic) that wanders too.
        var current2 = profile.start2
        val intervalMillis = (type.sampleIntervalMillis / timeScale).toLong().coerceAtLeast(1L)

        while (connected) {
            current = wander(current, profile.step, profile.min, profile.max)
            current2 = current2?.let {
                wander(it, profile.step2, profile.min2, profile.max2)
            }

            emit(
                SensorReading(
                    type = type,
                    value1 = round(current, profile.decimals),
                    value2 = current2?.let { round(it, profile.decimals2) },
                )
            )
            delay(intervalMillis)
        }
    }

    /** Nudge a value by a small random step, then clamp to a physiological range. */
    private fun wander(value: Double, step: Double, min: Double, max: Double): Double {
        val delta = (Random.nextDouble() * 2 - 1) * step // in [-step, +step]
        return (value + delta).coerceIn(min, max)
    }

    private fun round(value: Double, decimals: Int): Double {
        if (decimals <= 0) return value.toLong().toDouble()
        var factor = 1.0
        repeat(decimals) { factor *= 10 }
        return Math.round(value * factor) / factor
    }

    /** Per-vital baseline, drift size, and clamped range so the demo looks real. */
    private data class Profile(
        val start: Double, val min: Double, val max: Double, val step: Double, val decimals: Int,
        val start2: Double? = null, val min2: Double = 0.0, val max2: Double = 0.0,
        val step2: Double = 0.0, val decimals2: Int = 0,
    )

    /**
     * Step sizes are calibrated to each vital's cadence: one emission represents the
     * physiological change over that interval (glucose moves more per 5 min than HR
     * does per second), so trends look alive at any rate instead of frozen or jumpy.
     */
    private fun profileFor(type: VitalType): Profile = when (type) {
        VitalType.HEART_RATE ->            // per 1 s
            Profile(start = 75.0, min = 50.0, max = 110.0, step = 2.0, decimals = 0)
        VitalType.SPO2 ->                  // per 1 s
            Profile(start = 98.0, min = 92.0, max = 100.0, step = 0.4, decimals = 0)
        VitalType.TEMPERATURE ->           // per 30 s
            Profile(start = 36.8, min = 35.8, max = 38.0, step = 0.08, decimals = 1)
        VitalType.BLOOD_SUGAR ->           // per 5 min
            Profile(start = 110.0, min = 60.0, max = 200.0, step = 12.0, decimals = 0)
        VitalType.WEIGHT ->                // per day (not streamed, kept for completeness)
            Profile(start = 70.0, min = 69.0, max = 71.0, step = 0.1, decimals = 1)
        VitalType.BLOOD_PRESSURE ->        // per 15 min
            Profile(
                start = 120.0, min = 95.0, max = 160.0, step = 8.0, decimals = 0,    // systolic
                start2 = 80.0, min2 = 55.0, max2 = 100.0, step2 = 5.0, decimals2 = 0, // diastolic
            )
    }
}
