package org.jetbrains.mutrace

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.jetbrains.mutrace.format.Deserializer
import org.jetbrains.mutrace.format.DeserializerInput
import org.jetbrains.mutrace.format.InflatedMessageVisitor
import org.jetbrains.mutrace.format.MeasureKind
import java.io.DataInput
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

class RandomAccessFileDeserializerInput(val file: RandomAccessFile): DeserializerInput(), DataInput by file {
    override fun remaining(): Long {
        return file.length() - file.filePointer
    }

    override fun position(): Long {
        return file.filePointer
    }

    override fun seek(to: Long) {
        return file.seek(file.filePointer + to)
    }

}

fun convertModel(readForm: File): List<TraceEvent> {

    val deserializer = Deserializer(RandomAccessFileDeserializerInput(RandomAccessFile(readForm, "r")))
    val header = deserializer.readHeader()
    println(header)

    val traceEvents = mutableListOf<TraceEvent>()

    var startTime = -1L
    deserializer.readMessages(object : InflatedMessageVisitor() {
        fun timeMics(nanoTime: Long): Double {
            if (startTime == -1L) {
                startTime = nanoTime
            }
            return (nanoTime - startTime) * 1e-3
        }

        override fun visitMessage(kind: MeasureKind, tid: Long, time: Long) {
//            println("$kind tid=$tid time=$time")
            traceEvents += TraceEvent(kind.toPhase(), ts = timeMics(time), pid = header.pid, tid = tid)
        }

        fun MeasureKind.toPhase() = when(this) {
            MeasureKind.DURATION -> Phase.DurationStart
            MeasureKind.DURATION_WITH_ARGS -> Phase.DurationStart
            MeasureKind.DURATION_END -> Phase.DurationEnd
            MeasureKind.INSTANT -> Phase.Instant
            MeasureKind.INSTANT_WITH_ARGS -> Phase.Instant
            MeasureKind.COUNTER -> Phase.Counter
            MeasureKind.METADATA -> Phase.Metadata
        }

        override fun visitMessageWithName(kind: MeasureKind, tid: Long, name: String, time: Long) {
//            println("$kind tid=$tid name=$name time=$time")

            traceEvents += TraceEvent(kind.toPhase(), name = name, cat = "test", ts = timeMics(time), pid = header.pid, tid = tid)

        }

        override fun visitMessageWithArguments(
            kind: MeasureKind,
            tid: Long,
            name: String,
            argNames: List<String>,
            argValues: List<String>,
            time: Long
        ) {
//            println("$kind tid=$tid name=$name args=(${argNames.zip(argValues).joinToString { (a, b) -> "$a = $b" }}) time=$time")

            traceEvents += TraceEvent(kind.toPhase(), name = name, cat = "test", ts = timeMics(time), pid = header.pid, tid = tid,
                args = argNames.zip(argValues).toMap()
            )
        }

    })

    return traceEvents
}


@UseExperimental(ImplicitReflectionSerializer::class)
fun jsonify(input: File, output: File) {

    val json = Json(
        JsonConfiguration.Stable.copy(
            encodeDefaults = false,
            prettyPrint = false,
            useArrayPolymorphism = true
        )
    )

    val traceEvents = convertModel(input)
    output.writeText(json.stringify(
        TraceRoot(
            traceEvents,
            displayTimeUnit = TimeUnit.NANOSECONDS
        )
    ))
}

@ImplicitReflectionSerializer
fun main(args: Array<String>) {

    jsonify(File(args[0]), (args.getOrNull(1) ?: "output.trace").let { File(it) })
}
