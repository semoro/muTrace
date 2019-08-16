package org.jetbrains.mutrace

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.isSubclassOf


/*
If provided displayTimeUnit is a string that specifies in which unit timestamps should be displayed. This supports values of “ms” or “ns”. By default this is value is “ms”.

If provided systemTraceEvents is a string of Linux ftrace data or Windows ETW trace data. This data must start with # tracer: and adhere to the Linux ftrace format or adhere to Windows ETW format.

If provided, powerTraceAsString is a string of BattOr power data.

If provided, the stackFrames field is a dictionary of stack frames, their ids, and their parents that allows compact representation of stack traces throughout the rest of the trace file. It is optional but sometimes very useful in shrinking file sizes.

The samples array is used to store sampling profiler data from a OS level profiler. It stores samples that are different from trace event samples, and is meant to augment the traceEvent data with lower level information. It is OK to have a trace event file with just sample data, but in that case  traceEvents must still be provided and set to []. For more information on sample data, refer to the global samples section.

If provided, controllerTraceDataKey is a string that specifies which trace data comes from tracing controller. Its value should be the key for that specific trace data. For example, {..., "controllerTraceDataKey": "traceEvents"} means the data for traceEvents comes from the tracing controller. This is mainly for the purpose of clock synchronization.

Any other properties seen in the object, in this case otherData a

 */

@Serializable
data class StackFrame(val name: String, val category: String, val parent: Int? = null)

/*

Duration Events
B (begin), E (end)
Complete Events
X
Instant Events
i, I (deprecated)
Counter Events
C
Async Events
b (nestable start), n (nestable instant), e (nestable end)

Deprecated
S (start), T (step into), p (step past), F (end)
Flow Events
s (start), t (step), f (end)
Sample Events
P
Object Events
N (created), O (snapshot), D (destroyed)
Metadata Events
M
Memory Dump Events
V (global), v (process)
Mark Events
R
Clock Sync Events
c
Context Events
(, )

 */

@Serializable(with = PhaseSerializer::class)
sealed class Phase(val ph: String) {
    object DurationStart : Phase("B")
    object DurationEnd : Phase("E")
    object Complete : Phase("X")
    object Instant : Phase("i")
    object Counter : Phase("C")
    object Metadata : Phase("M")
}


@Serializable
data class TraceEvent(
    val ph: Phase,
    val name: String? = null,
    val cat: String? = null,
    val ts: Double,
    val pid: Long,
    val tid: Long,
    val dur: Double? = null,

    val args: (Map<String, String>?)  = null,
    val cname: String? = null,
    val tts: Double? = null,
    val sf: String? = null,
    val stack: List<String>? = null
)

//name: The name of the event, as displayed in Trace Viewer
//cat: The event categories. This is a comma separated list of categories for the event. The categories can be used to hide events in the Trace Viewer UI.
//ph: The event type. This is a single character which changes depending on the type of event being output. The valid values are listed in the table below. We will discuss each phase type below.
//ts: The tracing clock timestamp of the event. The timestamps are provided at microsecond granularity.
//pid: The process ID for the process that output this event.
//tid: The thread ID for the thread that output this event.
//Optional
//tts: Optional. The thread clock timestamp of the event. The timestamps are provided at microsecond granularity.
//args: Any arguments provided for the event. Some of the event types have required argument fields, otherwise, you can put any information you wish in here. The arguments are displayed in Trace Viewer when you view an event in the analysis section.
//cname: A fixed color name to associate with the event. If provided, cname must be one of the names listed in trace-viewer's base color scheme's reserved color names list



@Serializable
data class TraceRoot(
    val traceEvents: List<TraceEvent>,

    @Serializable(with = TimeUnitSerializer::class)
    val displayTimeUnit: TimeUnit,
    val samples: List<TraceEvent>? = null,
    val stackFrames: Map<Int, StackFrame>? = null,
    val powerTraceAsString: String? = null,
    val systemTraceEvents: String? = null,
    val controllerTraceDataKey: String? = null
)