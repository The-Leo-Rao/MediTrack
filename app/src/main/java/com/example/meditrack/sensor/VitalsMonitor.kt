package com.example.meditrack.sensor

import com.example.meditrack.data.VitalRepository
import com.example.meditrack.data.VitalType
import com.example.meditrack.graph.GraphPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch


class VitalsMonitor(
    private val source: VitalSensorSource,
    private val repository: VitalRepository,
    private val scope: CoroutineScope,
    private val liveWindowMillis: Long = 60_000L,
    private val liveMinPoints: Int = 60,
) {

    private val monitoredTypes = VitalType.entries.filter { it.continuous }


    private val shared = mutableMapOf<VitalType, SharedFlow<SensorReading>>()


    private val liveBuffers = monitoredTypes.associateWith {
        MutableStateFlow<List<GraphPoint>>(emptyList())
    }


    private val eventDetector = VitalEventDetector(persist = { repository.addEvent(it) })

    private val jobs = mutableListOf<Job>()


    fun start() {
        source.connect()
        monitoredTypes.forEach { type ->
            val hot = source.stream(type)
                .shareIn(scope, SharingStarted.Eagerly, replay = 1)
            shared[type] = hot
            startLiveBuffer(type, hot)
            startPersisting(type, hot)
        }
    }


    fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        shared.clear()
        source.disconnect()
    }


    fun liveStream(type: VitalType): Flow<SensorReading> =
        shared[type] ?: source.stream(type) // pre-start fallback: cold stream


    fun liveGraph(type: VitalType): StateFlow<List<GraphPoint>> =
        liveBuffers.getValue(type).asStateFlow()

    /** True if this type streams live (so callers can avoid [liveGraph] on WEIGHT). */
    fun isContinuous(type: VitalType): Boolean = liveBuffers.containsKey(type)


    private fun startLiveBuffer(type: VitalType, hot: SharedFlow<SensorReading>) {
        val buffer = liveBuffers.getValue(type)
        jobs += scope.launch {
            hot.collect { reading ->
                val cutoff = reading.timestamp - liveWindowMillis
                val prev = buffer.value
                val withinWindow = prev.filter { it.t >= cutoff }
                val kept = if (withinWindow.size >= liveMinPoints) withinWindow
                           else prev.takeLast(liveMinPoints)
                val next = ArrayList<GraphPoint>(kept.size + 1)
                next += kept
                next += GraphPoint(reading.timestamp, reading.value1, reading.value2)
                buffer.value = next
            }
        }
    }


    private fun startPersisting(type: VitalType, hot: SharedFlow<SensorReading>) {
        jobs += scope.launch {
            hot.collect { reading ->
                repository.addReading(
                    type = reading.type,
                    value1 = reading.value1,
                    value2 = reading.value2,
                )
                eventDetector.onReading(reading)
            }
        }
    }
}
