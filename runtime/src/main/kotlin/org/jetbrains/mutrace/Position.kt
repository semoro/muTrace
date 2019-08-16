package org.jetbrains.mutrace

fun positionString(className: String, methodName: String, fileName: String?, lineNumber: Int): String {
    val sourcePos = fileName?.let { "$it:$lineNumber" } ?: "$lineNumber"
    return "$className.$methodName($sourcePos)"
}