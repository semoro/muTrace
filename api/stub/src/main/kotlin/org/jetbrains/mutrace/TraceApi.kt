@file:Suppress("NOTHING_TO_INLINE", "UNUSED", "UNUSED_PARAMETER")
package org.jetbrains.mutrace

inline fun <T> recordTime(name: String = __POS, block: () -> T): T {
    return block()
}

inline fun instant(name: String) {}

inline fun <T> endRecordTime(block: () -> T): T {
    return block()
}

@Suppress("ObjectPropertyName")
inline val __POS get() = ""