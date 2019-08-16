@file:JvmName("MuTraceIntrinsics")
@file:JvmMultifileClass
package org.jetbrains.mutrace


annotation class MuTraceIntrinsic


/**
 * If using muTrace agent, id will be inlined
 * @param constStr must be constant in terms of bytecode
 * @return unique id of this string in interner pool
 */
@MuTraceIntrinsic
fun muTraceInternStr(constStr: String): Int = Interner.intern(constStr)

@MuTraceIntrinsic
fun muTraceCurrentPosition(): String =
    with(Thread.currentThread().stackTrace[1]) {
        positionString(className, methodName, fileName, lineNumber)
    }









