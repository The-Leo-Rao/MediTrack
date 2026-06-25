package com.example.meditrack.sensor

import com.example.meditrack.data.VitalType
import kotlinx.coroutines.flow.Flow

/**
 * THE SWAP POINT of the whole live-vitals design.
 *
 * Anything that can produce live vitals implements this interface:
 *   - SimulatedVitalSource  → fake data for the demo (used now)
 *   - BleVitalSource        → real Bluetooth sensors (drop in later)
 *
 * Because the rest of the app (VitalsMonitor, ViewModel, UI, graphs) only ever
 * talks to THIS interface, switching from simulated to real hardware is a one-line
 * change — nothing downstream has to be rewritten.
 */
interface VitalSensorSource {

    /** Begin acquiring data (open the BLE connection, start the simulator, etc.). */
    fun connect()

    /** Stop acquiring data and release resources. Safe to call when not connected. */
    fun disconnect()

    /**
     * Hot stream of live readings for one vital type. Emissions stop when [disconnect]
     * is called or the collecting coroutine is cancelled.
     */
    fun stream(type: VitalType): Flow<SensorReading>
}
