package com.example.meditrack.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.example.meditrack.clinical.ReferenceRanges
import com.example.meditrack.clinical.VitalStatus
import com.example.meditrack.data.VitalType
import com.example.meditrack.graph.GraphPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VitalChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LineChart(context, attrs, defStyle) {

    private var baseT: Long = 0L

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        description.isEnabled = false
        legend.isEnabled = true
        setNoDataText("Waiting for readings…")
        axisRight.isEnabled = false
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                timeFmt.format(Date(baseT + (value * 1000).toLong()))
        }
    }

    fun setData(
        type: VitalType,
        points: List<GraphPoint>
    ) {
        render(type, points, false)
    }

    private fun render(type: VitalType, points: List<GraphPoint>, follow: Boolean) {
        if (points.isEmpty()) {
            clear()
            return
        }
        baseT = points.first().t
        configureRangeBand(type)

        val primary = points.map { Entry((it.t - baseT) / 1000f, it.value.toFloat()) }
        val circleColors = points.map { circleColorFor(ReferenceRanges.classify(type, it.value, it.value2)) }
        val sets = ArrayList<ILineDataSet>()

        sets += styledSet(primary, primaryLabel(type), Color.parseColor("#D32F2F"), circleColors)

        // Blood pressure carries diastolic in value2 — draw it as a second line.
        if (type.hasSecondValue && points.any { it.value2 != null }) {
            val secondary = points
                .filter { it.value2 != null }
                .map { Entry((it.t - baseT) / 1000f, it.value2!!.toFloat()) }
            sets += styledSet(secondary, "Diastolic", Color.parseColor("#1976D2"), circleColors = null)
        }

        data = LineData(sets)
        notifyDataSetChanged()
        if (follow) {
            setVisibleXRangeMaximum(60f) // ~60 s window
            moveViewToX(primary.last().x)  // sweep to newest
        } else {
            fitScreen()
        }
        invalidate()
    }

    private fun styledSet(
        entries: List<Entry>,
        label: String,
        color: Int,
        circleColors: List<Int>?,
    ) =
        LineDataSet(entries, label).apply {
            mode = LineDataSet.Mode.LINEAR        // straight segments — preserve spikes
            setColor(color)
            lineWidth = 1.6f
            setDrawValues(false)
            setDrawFilled(false)
            if (circleColors != null) {
                // Only abnormal points get a visible dot (normal = transparent).
                setDrawCircles(true)
                circleRadius = 3f
                setDrawCircleHole(false)
                setCircleColors(circleColors)
            } else {
                setDrawCircles(false)             // dense data: dots would be noise
            }
        }

    /** Amber for warning, red for critical, invisible for normal. */
    private fun circleColorFor(status: VitalStatus): Int = when (status) {
        VitalStatus.NORMAL -> Color.TRANSPARENT
        VitalStatus.WARNING -> Color.parseColor("#FB8C00")
        VitalStatus.CRITICAL -> Color.parseColor("#D32F2F")
    }

    /** Draw the vital's normal reference range as shaded limit lines on the Y axis. */
    private fun configureRangeBand(type: VitalType) {
        axisLeft.removeAllLimitLines()
        val t = ReferenceRanges.rangesFor(type) ?: return
        val green = Color.parseColor("#43A047")
        t.normalLow?.let { axisLeft.addLimitLine(rangeLine(it.toFloat(), "Normal low", green)) }
        t.normalHigh?.let { axisLeft.addLimitLine(rangeLine(it.toFloat(), "Normal high", green)) }
        axisLeft.setDrawLimitLinesBehindData(true)
    }

    private fun rangeLine(value: Float, label: String, color: Int) =
        LimitLine(value, label).apply {
            lineColor = color
            lineWidth = 1f
            enableDashedLine(8f, 6f, 0f)
            textColor = color
        }

    private fun primaryLabel(type: VitalType): String =
        if (type.hasSecondValue) "Systolic" else "${type.displayName} (${type.defaultUnit})"
}
