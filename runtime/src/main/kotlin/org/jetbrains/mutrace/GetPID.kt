package org.jetbrains.mutrace

import java.lang.management.ManagementFactory


internal fun getProcessId(): Long? {
    // Note: may fail in some JVM implementations
    // therefore fallback has to be provided

    // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
    val jvmName = ManagementFactory.getRuntimeMXBean().name
    val index = jvmName.indexOf('@')

    if (index < 1) {
        // part before '@' empty (index = 0) / '@' not found (index = -1)
        return null
    }

    try {
        return java.lang.Long.parseLong(jvmName.substring(0, index))
    } catch (e: NumberFormatException) {
        // ignore
    }

    return null
}