package com.example.meditrack.report

import android.content.Context
import android.graphics.Bitmap
import com.example.meditrack.Record
import com.example.meditrack.Reminder
import com.example.meditrack.data.VitalEvent
import com.example.meditrack.data.VitalType
import com.example.meditrack.screens.formatVital
import com.example.meditrack.screens.toTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds the medical-grade vitals PDF end to end:
 *   gather data (IO) → render chart/image bitmaps (Main) → draw the document → save.
 *
 * The document is laid out twice so footers can show "Page X of Y": once to count
 * pages, once to render. Bitmaps are rendered only once and reused across both passes.
 */
object VitalsReportGenerator {

    private val hourMinute = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val fileStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())

    suspend fun generate(context: Context): File {
        val content = ReportDataSource.gather(context, windowDays = 30)

        // Pre-render bitmaps once (charts need the main thread).
        val charts = LinkedHashMap<VitalType, Bitmap>()
        for ((type, pts) in content.points) {
            ChartRenderer.renderVital(context, type, pts)?.let { charts[type] = it }
        }
        val thumbs = HashMap<Int, Bitmap>()
        for (rec in content.records) {
            if (rec.type == "CONSULTATION REPORT") {
                rec.data?.let { path -> ChartRenderer.decodeThumbnail(path)?.let { thumbs[rec.id] = it } }
            }
        }

        val genLabel = "Generated ${content.generatedAt.toTime()}"

        return withContext(Dispatchers.Default) {
            // Pass 1 — count pages (discarded).
            val counter = PdfCanvasWriter(totalPages = null, patientName = content.patient.name, generatedLabel = genLabel)
            counter.begin()
            drawReport(counter, content, charts, thumbs)
            val total = counter.finish()
            counter.document.close()

            // Pass 2 — render with the real total.
            val writer = PdfCanvasWriter(totalPages = total, patientName = content.patient.name, generatedLabel = genLabel)
            writer.begin()
            drawReport(writer, content, charts, thumbs)
            writer.finish()

            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, "MediTrack_Report_${fileStamp.format(Date())}.pdf")
            FileOutputStream(file).use { writer.document.writeTo(it) }
            writer.document.close()
            file
        }
    }

    private fun drawReport(
        w: PdfCanvasWriter,
        content: ReportContent,
        charts: Map<VitalType, Bitmap>,
        thumbs: Map<Int, Bitmap>,
    ) {
        val p = content.patient

        w.titleBlock(
            "MediTrack",
            "Health & Vitals Report",
            listOf(
                "Patient: ${p.name}",
                "Report period: ${content.periodStart.toTime()}  to  ${content.periodEnd.toTime()}",
                "Generated: ${content.generatedAt.toTime()}",
            ),
        )

        // 1. Patient information
        w.sectionTitle("Patient Information")
        w.keyValue("Name", p.name)
        w.keyValue("Blood group", p.bloodGroup)
        w.keyValue("Allergies", p.allergies)
        w.keyValue("Chronic illnesses", p.chronic)
        w.keyValue("Emergency contact", p.emergency)
        w.spacer(8f)

        // 2. Vitals summary table
        w.sectionTitle("Vitals Summary (last 30 days)")
        val sw = listOf(108f, 84f, 70f, 95f, 45f, 113f)
        w.tableRow(listOf("Vital", "Latest", "Average", "Min–Max", "Reads", "Normal range"), sw, header = true)
        for (s in content.summaries) {
            val latestCell = s.latest?.let { latestText(s) } ?: "—"
            val avgCell = if (s.count > 0) avgText(s) else "—"
            val rangeCell = if (s.count > 0) "${num(s.min1)}–${num(s.max1)}" else "—"
            val colors = s.latestStatus?.let { mapOf(1 to PdfCanvasWriter.statusColorInt(it)) } ?: emptyMap()
            w.tableRow(
                listOf(s.type.displayName, latestCell, avgCell, rangeCell, s.count.toString(), s.normalRange),
                sw, colors = colors,
            )
        }
        w.spacer(8f)

        // 3. Per-vital trends (charts)
        w.sectionTitle("Vital Trends")
        if (content.summaries.none { it.count > 0 }) {
            w.paragraph("No vital readings recorded in this period.")
        } else {
            for (s in content.summaries) {
                if (s.count == 0) continue
                w.subHeading("${s.type.displayName} (${s.type.defaultUnit})")
                charts[s.type]?.let { w.image(it) }
                w.note(
                    "Normal range ${s.normalRange}   ·   ${s.count} readings   ·   " +
                        "${s.percentInRange ?: 0}% in range   ·   ${s.episodes} abnormal episode(s)",
                )
                for (e in content.events.filter { it.type == s.type }.take(3)) {
                    w.bullet(eventLine(e))
                }
                w.spacer(6f)
            }
        }

        // 4. Clinical alerts
        w.sectionTitle("Clinical Alerts")
        if (content.events.isEmpty()) {
            w.paragraph("No abnormal episodes recorded in this period.")
        } else {
            val ew = listOf(120f, 80f, 95f, 150f, 70f)
            w.tableRow(listOf("Vital", "Severity", "Peak", "Started", "Duration"), ew, header = true)
            for (e in content.events) {
                w.tableRow(
                    listOf(
                        e.type.displayName,
                        e.status.name,
                        "${formatVital(e.type, e.extremeValue, e.extremeValue2)} ${e.type.defaultUnit}",
                        e.startTimestamp.toTime(),
                        "${e.durationMillis / 1000}s",
                    ),
                    ew, colors = mapOf(1 to PdfCanvasWriter.statusColorInt(e.status)),
                )
            }
        }
        w.spacer(8f)

        // 5. Current medications
        w.sectionTitle("Current Medication Reminders")
        if (content.reminders.isEmpty()) {
            w.paragraph("No medication reminders set.")
        } else {
            for (r in content.reminders) {
                w.keyValue(r.med, "Dose ${r.dose}   ·   ${reminderTimes(r)}")
            }
        }
        w.spacer(8f)

        // 6. Medical records
        w.sectionTitle("Medical Records")
        if (content.records.isEmpty()) {
            w.paragraph("No medical records logged.")
        } else {
            for (rec in content.records) drawRecord(w, rec, thumbs[rec.id])
        }
    }

    private fun drawRecord(w: PdfCanvasWriter, rec: Record, thumb: Bitmap?) {
        w.subHeading("> ${rec.type.lowercase()}   ·   ${rec.timestamp.toTime()}")
        when (rec.type) {
            "NEW SYMPTOM" -> {
                val data = rec.data ?: ""
                val severity = data.lastOrNull()?.toString() ?: "?"
                val text = if (data.isNotEmpty()) data.dropLast(1) else ""
                w.paragraph("$text   (severity $severity/10)")
            }
            "FOLLOW UP" -> {
                val date = rec.data?.toLongOrNull()?.toTime() ?: "Date not set"
                w.paragraph("Scheduled follow-up: $date")
            }
            "CONSULTATION REPORT" -> {
                if (thumb != null) w.image(thumb, maxHeight = 170f) else w.paragraph("[Consultation image]")
            }
            else -> w.paragraph(rec.data ?: "")
        }
        w.spacer(4f)
    }

    private fun latestText(s: VitalSummary): String {
        val l = s.latest ?: return "—"
        val v2 = if (s.type.hasSecondValue) l.val2 else null
        return formatVital(s.type, l.val1, v2)
    }

    private fun avgText(s: VitalSummary): String =
        if (s.type.hasSecondValue) "${num(s.avg1)}/${num(s.avg2)}" else num(s.avg1)

    private fun eventLine(e: VitalEvent): String =
        "${e.status} — peak ${formatVital(e.type, e.extremeValue, e.extremeValue2)} ${e.type.defaultUnit}, " +
            "${e.startTimestamp.toTime()} → ${e.endTimestamp.toTime()} (${e.durationMillis / 1000}s)"

    private fun reminderTimes(r: Reminder): String {
        val t1 = "%02d:%02d".format(r.hour1, r.minute1)

        return if (r.hour2 != 0 || r.minute2 != 0) {
            val t2 = "%02d:%02d".format(r.hour2, r.minute2)
            "$t1, $t2"
        } else {
            t1
        }
    }
}
