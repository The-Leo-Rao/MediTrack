package com.example.meditrack.sensor

import com.example.meditrack.data.VitalType
import kotlinx.coroutines.flow.Flow


interface VitalSensorSource {


    fun connect()


    fun disconnect()


    fun stream(type: VitalType): Flow<SensorReading>
}
