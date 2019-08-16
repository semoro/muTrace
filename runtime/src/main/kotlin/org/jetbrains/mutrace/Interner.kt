package org.jetbrains.mutrace

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal data class InternerMaps(val names: Map<String, Int>, val arguments: Map<List<String>, Int>)

object Interner {
    private val map = ConcurrentHashMap<String, Int>()
    private val counter = AtomicInteger()
    fun intern(str: String): Int {
        return map.computeIfAbsent(str) {
            counter.getAndIncrement()
        }
    }

    private val argumentMap = ConcurrentHashMap<List<String>, Int>()
    private val argumentCounter = AtomicInteger()
    fun intern(arguments: List<String>): Int {
        return argumentMap.computeIfAbsent(arguments) {
            argumentCounter.getAndIncrement()
        }
    }

    internal fun mapsForExport() = InternerMaps(map.toMap(), argumentMap.toMap())
}
