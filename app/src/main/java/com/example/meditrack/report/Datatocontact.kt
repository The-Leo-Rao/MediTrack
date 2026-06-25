package com.example.meditrack.report

/**
 * dataToContact.kt — SOS dispatcher
 *
 * Mirrors VitalsReportGenerator exactly for the PDF step, then adds:
 *   • GPS location fetched in parallel with PDF generation
 *   • Firebase Storage upload → public download URL
 *   • Multipart SMS to the emergency contact: location link + PDF link
 *
 * Call from any coroutine scope (e.g. lifecycleScope or viewModelScope):
 *
 *     lifecycleScope.launch {
 *         SOSDispatcher.dispatch(
 *             context  = context,
 *             onStep   = { msg -> statusText = msg },       // main thread
 *             onDone   = { ok, msg -> showDialog(ok, msg) } // main thread
 *         )
 *     }
 *
 * Required permissions in AndroidManifest (already declared):
 *   ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, SEND_SMS
 *
 * New Gradle dependency (add once):
 *   implementation("com.google.firebase:firebase-storage")
 */

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.meditrack.data.VitalType
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "SOS"

object SOSDispatcher {

    private val fileStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())

    /**
     * Full SOS flow. Must be called from a coroutine — suspends until the SMS is sent
     * (or a failure is reported). [onStep] and [onDone] are always called on the main thread.
     */
    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.SEND_SMS,
    ])
    suspend fun dispatch(
        context: Context,
        onStep: suspend (String) -> Unit = {},
        onDone: suspend (success: Boolean, message: String) -> Unit,
    ) {
        // ── 0. Auth guard ────────────────────────────────────────────────────
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            withContext(Dispatchers.Main) { onDone(false, "Not logged in.") }
            return
        }

        // ── 1. Fetch raw emergency phone from Firestore ──────────────────────
        // ReportDataSource.loadPatient() masks the number for display;
        // here we need the real digits to dial, so we fetch directly.
        withContext(Dispatchers.Main) { onStep("Fetching emergency contact…") }

        val rawPhone: String? = fetchEmergencyPhone(uid)
        if (rawPhone.isNullOrBlank()) {
            withContext(Dispatchers.Main) {
                onDone(false, "No emergency contact saved. Add one in your profile.")
            }
            return
        }
        val e164Phone = "+91$rawPhone"
        Log.d(TAG, "Contact = $e164Phone")

        // ── 2. Location + PDF in parallel ────────────────────────────────────
        // Location fetch is IO-bound (GPS); PDF generation is CPU-bound.
        // Running both at once saves ~3–5 s on a typical device.
        withContext(Dispatchers.Main) { onStep("Getting location and building report…") }

        val mapsLink: String?
        val pdfFile: File?

        coroutineScope {
            val locationDeferred = async(Dispatchers.IO) { fetchLocation(context) }
            val pdfDeferred      = async { buildSosPdf(context) }

            val (lat, lon) = locationDeferred.await()
            mapsLink = if (lat != null && lon != null)
                "https://maps.google.com/?q=$lat,$lon"
            else
                null

            pdfFile = pdfDeferred.await()
        }

        Log.d(TAG, "Location = $mapsLink")
        Log.d(TAG, "PDF = ${pdfFile?.name}")

        // ── 3. Upload PDF to Firebase Storage ───────────────────────────────
        val downloadUrl: String? = if (pdfFile != null) {
            withContext(Dispatchers.Main) { onStep("Uploading medical summary…") }
            uploadToStorage(uid, pdfFile)
        } else {
            null
        }

        // ── 4. Send SMS ──────────────────────────────────────────────────────
        withContext(Dispatchers.Main) { onStep("Sending SOS…") }
        sendSms(e164Phone, mapsLink, downloadUrl)

        val detail = buildString {
            if (mapsLink == null) append(" (location unavailable)")
            if (downloadUrl == null) append(" (report unavailable)")
        }
        withContext(Dispatchers.Main) {
            onDone(true, "SOS sent to $rawPhone$detail")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Step 1 — Firestore: raw emergency number
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun fetchEmergencyPhone(uid: String): String? =
        suspendCancellableCoroutine { cont ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc -> cont.resume(doc.getString("Emergency")) }
                .addOnFailureListener { cont.resume(null) }
        }

    // ─────────────────────────────────────────────────────────────────────────
    //  Step 2a — GPS location (10 s timeout, degrades gracefully)
    // ─────────────────────────────────────────────────────────────────────────

    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ])
    private suspend fun fetchLocation(context: Context): Pair<Double?, Double?> =
        suspendCancellableCoroutine { cont ->
            val cancel = com.google.android.gms.tasks.CancellationTokenSource()

            // Safety timeout — GPS can hang; we don't want SOS blocked.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (cont.isActive) {
                    cancel.cancel()
                    cont.resume(null to null)
                }
            }, 10_000)

            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancel.token)
                .addOnSuccessListener { loc ->
                    if (cont.isActive)
                        cont.resume(loc?.latitude to loc?.longitude)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null to null)
                }
                .addOnCanceledListener {
                    if (cont.isActive) cont.resume(null to null)
                }
        }

    // ─────────────────────────────────────────────────────────────────────────
    //  Step 2b — PDF: same two-pass approach as VitalsReportGenerator
    //            but with an SOS-specific title block
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun buildSosPdf(context: Context): File? = runCatching {

        // Gather exactly the same content VitalsReportGenerator uses.
        val content = ReportDataSource.gather(context, windowDays = 30)
        val p = content.patient

        // Pre-render chart bitmaps on Main (View rendering requires Main thread).
        val charts = LinkedHashMap<VitalType, Bitmap>()
        for ((type, pts) in content.points) {
            ChartRenderer.renderVital(context, type, pts)?.let { charts[type] = it }
        }
        val thumbs = HashMap<Int, Bitmap>()
        for (rec in content.records) {
            if (rec.type == "CONSULTATION REPORT") {
                rec.data?.let { path ->
                    ChartRenderer.decodeThumbnail(path)?.let { thumbs[rec.id] = it }
                }
            }
        }

        val genLabel = "SOS — Generated ${System.currentTimeMillis().toSosTime()}"

        return@runCatching withContext(Dispatchers.Default) {

            // Pass 1 — count pages.
            val counter = PdfCanvasWriter(
                totalPages    = null,
                patientName   = p.name,
                generatedLabel = genLabel,
            )
            counter.begin()
            drawSosReport(counter, content, charts, thumbs)
            val total = counter.finish()
            counter.document.close()

            // Pass 2 — render with real page total.
            val writer = PdfCanvasWriter(
                totalPages    = total,
                patientName   = p.name,
                generatedLabel = genLabel,
            )
            writer.begin()
            drawSosReport(writer, content, charts, thumbs)
            writer.finish()

            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            dir.mkdirs()
            val file = File(dir, "SOS_Report_${fileStamp.format(Date())}.pdf")
            FileOutputStream(file).use { writer.document.writeTo(it) }
            writer.document.close()
            file
        }
    }.onFailure { Log.e(TAG, "PDF build failed: ${it.message}") }.getOrNull()

    /**
     * Draws an SOS-specific report — identical sections to VitalsReportGenerator
     * but with a red emergency title block so recipients immediately understand
     * this is an emergency document, not a routine export.
     */
    private fun drawSosReport(
        w: PdfCanvasWriter,
        content: ReportContent,
        charts: Map<VitalType, Bitmap>,
        thumbs: Map<Int, Bitmap>,
    ) {
        val p = content.patient

        // ── Emergency title block (replaces the standard teal header) ─────────
        w.titleBlock(
            "🚨 EMERGENCY — MediTrack",
            "Medical Summary for First Responders",
            listOf(
                "Patient: ${p.name}",
                "Blood group: ${p.bloodGroup}",
                "Allergies: ${p.allergies}",
                "Report period: ${content.periodStart.toSosTime()}  to  ${content.periodEnd.toSosTime()}",
                "Generated: ${content.generatedAt.toSosTime()}",
            ),
        )

        // ── 1. Patient information ─────────────────────────────────────────
        w.sectionTitle("Patient Information")
        w.keyValue("Name", p.name)
        w.keyValue("Blood group", p.bloodGroup)
        w.keyValue("Allergies", p.allergies)
        w.keyValue("Chronic illnesses", p.chronic)
        w.keyValue("Emergency contact", p.emergency)   // already masked for privacy
        w.spacer(8f)

        // ── 2. Vitals summary ─────────────────────────────────────────────
        w.sectionTitle("Vitals Summary (last 30 days)")
        val sw = listOf(108f, 84f, 70f, 95f, 45f, 113f)
        w.tableRow(listOf("Vital", "Latest", "Average", "Min–Max", "Reads", "Normal range"), sw, header = true)
        for (s in content.summaries) {
            val latestCell = s.latest?.let { sosLatestText(s) } ?: "—"
            val avgCell    = if (s.count > 0) sosAvgText(s) else "—"
            val rangeCell  = if (s.count > 0) "${num(s.min1)}–${num(s.max1)}" else "—"
            val colors     = s.latestStatus?.let { mapOf(1 to PdfCanvasWriter.statusColorInt(it)) } ?: emptyMap()
            w.tableRow(
                listOf(s.type.displayName, latestCell, avgCell, rangeCell, s.count.toString(), s.normalRange),
                sw, colors = colors,
            )
        }
        w.spacer(8f)

        // ── 3. Vital trends (charts) ──────────────────────────────────────
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
                    w.bullet(sosEventLine(e))
                }
                w.spacer(6f)
            }
        }

        // ── 4. Clinical alerts ─────────────────────────────────────────────
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
                        "${com.example.meditrack.screens.formatVital(e.type, e.extremeValue, e.extremeValue2)} ${e.type.defaultUnit}",
                        e.startTimestamp.toSosTime(),
                        "${e.durationMillis / 1000}s",
                    ),
                    ew, colors = mapOf(1 to PdfCanvasWriter.statusColorInt(e.status)),
                )
            }
        }
        w.spacer(8f)

        // ── 5. Current medications ─────────────────────────────────────────
        w.sectionTitle("Current Medication Reminders")
        if (content.reminders.isEmpty()) {
            w.paragraph("No medication reminders set.")
        } else {
            for (r in content.reminders) {
                val t1 = "%02d:%02d".format(r.hour1, r.minute1)
                val times = if (r.hour2 != 0 || r.minute2 != 0)
                    "$t1, %02d:%02d".format(r.hour2, r.minute2) else t1
                w.keyValue(r.med, "Dose ${r.dose}   ·   $times")
            }
        }

    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Step 3 — Firebase Storage upload → download URL
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun uploadToStorage(uid: String, file: File): String? =
        suspendCancellableCoroutine { cont ->
            val ref = FirebaseStorage.getInstance()
                .reference
                .child("sos_reports/$uid/${file.name}")

            ref.putFile(android.net.Uri.fromFile(file))
                .addOnFailureListener { e ->
                    Log.e(TAG, "Upload failed: ${e.message}")
                    if (cont.isActive) cont.resume(null)
                }
                .addOnSuccessListener {
                    ref.downloadUrl
                        .addOnSuccessListener { uri ->
                            if (cont.isActive) cont.resume(uri.toString())
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
        }

    // ─────────────────────────────────────────────────────────────────────────
    //  Step 4 — SMS
    // ─────────────────────────────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.SEND_SMS)
    private fun sendSms(phone: String, mapsLink: String?, downloadUrl: String?) {
        val body = buildString {
            append("EMERGENCY MEDICAL ALERT\n\n")
            if (mapsLink != null) append("Location: $mapsLink\n\n")
            else append("Location: could not be determined.\n\n")
            if (downloadUrl != null) append("Medical report: $downloadUrl")
        }
        Log.d(TAG, "SMS → $phone\n$body")
        try {
            val parts = SmsManager.getDefault().divideMessage(body)
            SmsManager.getDefault().sendMultipartTextMessage(phone, null, parts, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "SMS failed: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Private text helpers (parallel to VitalsReportGenerator's private fns)
    // ─────────────────────────────────────────────────────────────────────────

    private fun sosLatestText(s: VitalSummary): String {
        val l  = s.latest ?: return "—"
        val v2 = if (s.type.hasSecondValue) l.val2 else null
        return com.example.meditrack.screens.formatVital(s.type, l.val1, v2)
    }

    private fun sosAvgText(s: VitalSummary): String =
        if (s.type.hasSecondValue) "${num(s.avg1)}/${num(s.avg2)}" else num(s.avg1)

    private fun sosEventLine(e: com.example.meditrack.data.VitalEvent): String =
        "${e.status} — peak ${com.example.meditrack.screens.formatVital(e.type, e.extremeValue, e.extremeValue2)} ${e.type.defaultUnit}, " +
                "${e.startTimestamp.toSosTime()} → ${e.endTimestamp.toSosTime()} (${e.durationMillis / 1000}s)"
}

// ─────────────────────────────────────────────────────────────────────────────
//  Timestamp formatter — local to this file, mirrors toTime() in vitalScreen
// ─────────────────────────────────────────────────────────────────────────────
private val sosFmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
private fun Long.toSosTime(): String = sosFmt.format(Date(this))