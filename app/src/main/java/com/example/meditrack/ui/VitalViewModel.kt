package com.example.meditrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditrack.DBHelper
import com.example.meditrack.Vital
import com.example.meditrack.clinical.ReferenceRanges
import com.example.meditrack.clinical.VitalStatus
import com.example.meditrack.data.VitalEvent
import com.example.meditrack.data.VitalRepository
import com.example.meditrack.data.VitalType
import com.example.meditrack.graph.GraphDownsampler
import com.example.meditrack.graph.GraphPoint
import com.example.meditrack.sensor.SensorReading
import com.example.meditrack.sensor.SimulatedVitalSource
import com.example.meditrack.sensor.VitalSensorSource
import com.example.meditrack.sensor.VitalsMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The single entry point the UI talks to for everything vitals-related. Screens
 * call these functions and collect these Flows — they never touch the DB or the
 * sensor layer directly.
 *
 * Monitoring starts automatically when the ViewModel is created (see init) and
 * stops when it is cleared, so the live cards have data the moment the user opens
 * the vitals area. Scope it to the Activity so monitoring survives navigation.
 */
class VitalViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = VitalRepository(DBHelper(app))

    // ── THE ONE LINE TO CHANGE FOR REAL HARDWARE ───────────────────────────────
    // Swap SimulatedVitalSource(...) for BleVitalSource() once sensors are wired.
    // timeScale = 1.0 is real time (HR/SpO₂ tick each second). Raise it (e.g. 60.0)
    // to compress an hour into a minute so slow vitals (glucose, BP) visibly move
    // during a short demo.
    private val source: VitalSensorSource = SimulatedVitalSource(timeScale = 1.0)
    // ───────────────────────────────────────────────────────────────────────────

    private val monitor = VitalsMonitor(source, repository, viewModelScope)

    private val _isMonitoring = MutableStateFlow(false)

    val isMonitoringFlow: StateFlow<Boolean> = _isMonitoring.asStateFlow()


    // ── Control ────────────────────────────────────────────────────────────────

    fun startMonitoring() {
        if (!_isMonitoring.value) {
            _isMonitoring.value = true
            monitor.start()
        }
    }

    fun stopMonitoring() {
        if (_isMonitoring.value) {
            _isMonitoring.value = false
            monitor.stop()
        }
    }

    fun toggleMonitoring() {
        if (_isMonitoring.value) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }

    val isMonitoring: Boolean
        get() = _isMonitoring.value

    // ── Live (real-time, in-memory) ────────────────────────────────────────────

    /** Hot live stream for one vital — collect for a real-time number. */
    fun liveStream(type: VitalType): Flow<SensorReading> = monitor.liveStream(type)

    /** Rolling live-graph window for the animated chart (continuous vitals only). */
    fun liveGraph(type: VitalType): StateFlow<List<GraphPoint>> = monitor.liveGraph(type)

    /** Whether [type] streams live (false for WEIGHT — it is logged manually). */
    fun isContinuous(type: VitalType): Boolean = monitor.isContinuous(type)

    // ── History (durable, SQLite-backed) ───────────────────────────────────────

    /** Live count of saved readings. */
    val historyCount: Flow<Int> = repository.observeCount()

    /**
     * Spike-preserving history graph for a fixed window. Reads the full-resolution
     * range and min/max-decimates it to at most [maxPoints] points off the main
     * thread, so wide windows stay accurate AND fast.
     */
    fun historyGraph(
        type: VitalType,
        startMillis: Long,
        endMillis: Long,
        maxPoints: Int = 500,
    ): Flow<List<GraphPoint>> =
        repository.observeByTypeInRange(type, startMillis, endMillis)
            .map { GraphDownsampler.minMaxDecimate(it, maxPoints / 2) }
            .flowOn(Dispatchers.Default)

    /** Convenience: spike-preserving history graph for the last [days] days. */
    fun historyGraph(type: VitalType, days: Int, maxPoints: Int = 500): Flow<List<GraphPoint>> {
        val now = System.currentTimeMillis()
        val start = now - days.toLong() * 24 * 60 * 60 * 1000
        return historyGraph(type, start, now, maxPoints)
    }

    /** Raw history rows for one vital type (chronological). */
    fun graphData(type: VitalType): Flow<List<Vital>> = repository.observeByType(type)

    /** Latest saved reading of a type, for a dashboard card. */
    fun latest(type: VitalType): Flow<Vital?> = repository.observeLatest(type)

    // ── Manual entry ───────────────────────────────────────────────────────────

    /** Log a reading the user typed in (used by the detail screen's "Log" dialog). */
    fun logVital(type: VitalType, value1: Double, value2: Double? = null, note: String? = null) {
        viewModelScope.launch {
            repository.addReading(type, value1, value2, note)
        }
    }

    // ── Clinical ───────────────────────────────────────────────────────────────

    /** Live feed of abnormal excursions (newest first) — wire to an alerts list. */
    fun events(limit: Int = 100): Flow<List<VitalEvent>> = repository.observeEvents(limit)

    /** Classify a reading against its reference range (NORMAL / WARNING / CRITICAL). */
    fun classify(type: VitalType, value1: Double, value2: Double? = null): VitalStatus =
        ReferenceRanges.classify(type, value1, value2)

    override fun onCleared() {
        monitor.stop()
        super.onCleared()
    }

    fun seedTestData() {
        viewModelScope.launch {
            repository.seedTestData()
        }
    }
}
