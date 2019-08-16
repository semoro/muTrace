package org.jetbrains.mutrace.format

data class FormatVersion(val high: Int, val low: Int) {
    companion object {
        val CURRENT = FormatVersion(1, 1)
    }
}
data class Header(val version: FormatVersion, val pid: Long)

data class TraceDataHeader(val internerData: InternerData, val eventBuffersHeader: EventBuffersHeader)

data class EventBuffersHeader(val size: Int)

data class InternerData(val names: Map<String, Int>, val arguments: Map<List<String>, Int>)

data class EventBufferContainer(val kind: Int, val next: Int)

sealed class EventBufferHeader

data class StringsHeader(val tid: Long, val size: Int): EventBufferHeader()
data class EventsHeader(val tid: Long, val size: Int): EventBufferHeader()

enum class MeasureKind {
    DURATION,
    DURATION_WITH_ARGS,
    DURATION_END,
    INSTANT,
    INSTANT_WITH_ARGS,
    COUNTER,
    METADATA
}