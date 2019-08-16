package org.jetbrains.mutrace



inline fun <T> traceTime(name: String = __POS, block: () -> T): T {
    val id = muTraceInternStr(name)
    TraceCollector.Duration.start(id)
    return endTraceTime(block)
}

@Suppress("NOTHING_TO_INLINE")
inline fun traceInstant(name: String) {
    val id = muTraceInternStr(name)
    TraceCollector.SingleEvent.instant(id)
}

inline fun <T> endTraceTime(block: () -> T): T {
    return try {
        block()
    } finally {
        TraceCollector.Duration.end()
    }
}

@Suppress("ObjectPropertyName")
inline val __POS get() = muTraceCurrentPosition()


