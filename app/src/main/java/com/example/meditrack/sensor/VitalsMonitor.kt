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

/**
 * Orchestrates the live-vitals pipeline. It collects each vital type's sensor
 * stream EXACTLY ONCE and fans it out to three consumers:
 *
 *   1. [liveStream]  — raw readings for the real-time number display.
 *   2. [liveGraph]   — a rolling in-memory window for the animated chart.
 *   3. persistence   — every reading written to history via [VitalRepository], and
 *                      fed to [VitalEventDetector] for abnormal-excursion flagging.
 *
 * Why fan out from one shared stream: [VitalSensorSource.stream] is a *cold* flow,
 * so collecting it twice would start two independent random walks — the number
 * shown live would not be the number recorded. [shareIn] makes one hot stream per
 * type, guaranteeing the spike recorded is the same one shown live.
 *
 * @param liveWindowMillis how much recent history the live graph buffer keeps.
 * @param liveMinPoints keep at least this many recent points even if older than the
 *        window, so slow vitals still render a line.
 */
class VitalsMonitor(
    private val source: VitalSensorSource,
    private val repository: VitalRepository,
    private val scope: CoroutineScope,
    private val liveWindowMillis: Long = 60_000L,
    private val liveMinPoints: Int = 60,
) {
    /** Which vital types we stream + persist while monitoring (WEIGHT is manual). */
    private val monitoredTypes = VitalType.entries.filter { it.continuous }

    /** One hot, shared stream per type — the single source the three consumers read. */
    private val shared = mutableMapOf<VitalType, SharedFlow<SensorReading>>()

    /** Rolling live-graph window per type, backing [liveGraph]. */
    private val liveBuffers = monitoredTypes.associateWith {
        MutableStateFlow<List<GraphPoint>>(emptyList())
    }

    /** Flags out-of-range excursions as clinical events, persisted via the repository. */
    private val eventDetector = VitalEventDetector(persist = { repository.addEvent(it) })

    private val jobs = mutableListOf<Job>()

    /** Open the source, start sharing each stream, and begin the live + history paths. */
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

    /** Stop every consumer and close the source. */
    fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        shared.clear()
        source.disconnect()
    }

    /** Hot, unthrottled raw stream for the live monitor numbers. Call after [start]. */
    fun liveStream(type: VitalType): Flow<SensorReading> =
        shared[type] ?: source.stream(type) // pre-start fallback: cold stream

    /**
     * Rolling live-graph window for [type] — the last [liveWindowMillis] of points,
     * updated on every reading. Only valid for continuous vitals.
     */
    fun liveGraph(type: VitalType): StateFlow<List<GraphPoint>> =
        liveBuffers.getValue(type).asStateFlow()

    /** True if this type streams live (so callers can avoid [liveGraph] on WEIGHT). */
    fun isContinuous(type: VitalType): Boolean = liveBuffers.containsKey(type)

    /**
     * Append each reading to the rolling buffer. Drop points older than the window,
     * but always keep at least [liveMinPoints] so slow vitals still render a line.
     */
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

    /**
     * Persist every reading so history keeps full fidelity (no spike dropped), and
     * feed the same reading to the event detector for abnormal-excursion flagging.
     */
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
