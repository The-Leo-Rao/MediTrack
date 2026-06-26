package com.example.meditrack.data

enum class VitalType(
    val displayName: String,
    val defaultUnit: String,
    val sampleIntervalMillis: Long,
    val hasSecondValue: Boolean = false,
    val continuous: Boolean = true,
) {
    BLOOD_PRESSURE("Blood Pressure", "mmHg", sampleIntervalMillis = 900_000, hasSecondValue = true),
    BLOOD_SUGAR("Blood Sugar", "mg/dL", sampleIntervalMillis = 300_000),
    TEMPERATURE("Temperature", "°C", sampleIntervalMillis = 30_000),
    HEART_RATE("Heart Rate", "bpm", sampleIntervalMillis = 10_000),
    SPO2("Oxygen (SpO₂)", "%", sampleIntervalMillis = 5_000),

    WEIGHT("Weight", "kg", sampleIntervalMillis = 86_400_000, continuous = false),
}
