package com.example.meditrack.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.View
import com.example.meditrack.data.VitalType
import com.example.meditrack.graph.GraphPoint
import com.example.meditrack.ui.VitalChartView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders the same [VitalChartView] used in-app to a Bitmap for embedding in the PDF,
 * so the report graphs match the app exactly (range bands, out-of-range dots, dual BP
 * lines). The view is measured/laid out off-screen — that part must run on the main
 * thread, since it touches an Android View.
 */
object ChartRenderer {

    private const val CHART_W = 1000
    private const val CHART_H = 430

    /** A dark card background so the chart's white axis/legend text stays readable. */
    private val chartBg = Color.rgb(11, 41, 53) // CharcoalBlue

    suspend fun renderVital(context: Context, type: VitalType, points: List<GraphPoint>): Bitmap? {
        if (points.size < 2) return null
        return withContext(Dispatchers.Main) {
            val chart = VitalChartView(context)
            chart.setBackgroundColor(chartBg)
            chart.setData(type, points, follow = false)
            chart.measure(
                View.MeasureSpec.makeMeasureSpec(CHART_W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(CHART_H, View.MeasureSpec.EXACTLY),
            )
            chart.layout(0, 0, CHART_W, CHART_H)
            runCatching { chart.chartBitmap }.getOrNull()
        }
    }

    /** Decode a stored consultation image downsampled to ~[maxPx] so thumbnails stay light. */
    fun decodeThumbnail(path: String, maxPx: Int = 320): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return null
            var sample = 1
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            while (largest / sample > maxPx) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)
        }.getOrNull()
    }
}
