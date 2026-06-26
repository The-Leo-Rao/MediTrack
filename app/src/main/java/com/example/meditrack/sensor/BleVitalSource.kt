package com.example.meditrack.sensor

import com.example.meditrack.data.VitalType
import kotlinx.coroutines.flow.Flow

class BleVitalSource : VitalSensorSource {

    override fun connect() =
        throw NotImplementedError("BLE not wired yet — using SimulatedVitalSource for the demo.")

    override fun disconnect() =
        throw NotImplementedError("BLE not wired yet — using SimulatedVitalSource for the demo.")

    override fun stream(type: VitalType): Flow<SensorReading> =
        throw NotImplementedError("BLE not wired yet — using SimulatedVitalSource for the demo.")
}
