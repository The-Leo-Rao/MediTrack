package com.example.meditrack.graph

import com.example.meditrack.DBHelper
import com.example.meditrack.Vital
import com.example.meditrack.graph.GraphPoint

object GraphDownsampler {

    fun minMaxDecimate(points: List<Vital>, buckets: Int): List<GraphPoint> {
        if (buckets < 1 || points.size <= buckets * 2) {
            return points.map { GraphPoint(it.timestamp, it.val1, it.val2) }
        }

        val first = points.first().timestamp
        val last = points.last().timestamp
        val span = (last - first).coerceAtLeast(1)
        val bucketMillis = span.toDouble() / buckets

        val out = ArrayList<GraphPoint>(buckets * 2)
        var bucketStart = 0
        for (b in 0 until buckets) {
            val edge = first + ((b + 1) * bucketMillis).toLong()
            // Collect the index range that falls in this bucket.
            var i = bucketStart
            var minIdx = -1
            var maxIdx = -1
            while (i < points.size && (points[i].timestamp <= edge || b == buckets - 1)) {
                val v = points[i].val1
                if (minIdx == -1 || v < points[minIdx].val1) minIdx = i
                if (maxIdx == -1 || v > points[maxIdx].val1) maxIdx = i
                i++
            }
            if (minIdx != -1) {
                // Emit the two extremes in chronological order (skip the duplicate
                // when min and max are the same point — a flat bucket).
                val lo = minOf(minIdx, maxIdx)
                val hi = maxOf(minIdx, maxIdx)
                out += points[lo].let { GraphPoint(it.timestamp, it.val1, it.val2) }
                if (hi != lo) out += points[hi].let { GraphPoint(it.timestamp, it.val1, it.val2) }
            }
            bucketStart = i
        }
        return out
    }
}
