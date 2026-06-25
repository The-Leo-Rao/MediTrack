package com.example.meditrack.sensor

import com.example.meditrack.data.VitalType
import kotlinx.coroutines.flow.Flow

/**
 * Placeholder for the REAL Bluetooth Low Energy sensor source. Not implemented yet
 * — it exists so the rest of the app already targets the production shape.
 *
 * When the hardware arrives, implement each method and then change ONE line in
 * [com.example.meditrack.ui.VitalViewModel]:
 *     SimulatedVitalSource(...)  →  BleVitalSource(context, ...)
 * Everything downstream (live monitor, history, graphs, events) keeps working.
 *
 * Implementation sketch for later:
 *   connect():  use BluetoothLeScanner to find the device, connect GATT, then
 *               enable notifications on the sensor's measurement characteristic
 *               (e.g. Heart Rate Service 0x180D, characteristic 0x2A37).
 *   stream():   use callbackFlow { } — in the GATT onCharacteristicChanged callback,
 *               parse the bytes into a SensorReading and trySend() it; clean up in
 *               awaitClose { }.
 *   disconnect(): close the GATT connection.
 */
class BleVitalSource : VitalSensorSource {

    override fun connect() =
        throw NotImplementedError("BLE not wired yet — using SimulatedVitalSource for the demo.")

    override fun disconnect() =
        throw NotImplementedError("BLE not wired yet — using SimulatedVitalSource for the demo.")

    override fun stream(type: VitalType): Flow<SensorReading> =
        throw NotImplementedError("BLE not wired yet — using SimulatedVitalSource for the demo.")
}
