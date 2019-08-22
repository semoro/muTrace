package org.jetbrains.mutrace

import kotlinx.serialization.*
import kotlinx.serialization.internal.ArrayListClassDesc
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.jetbrains.mutrace.format.*
import java.io.DataInput
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

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


@Serializable(with = DeserializingStreamedEvents.Companion::class)
class DeserializingStreamedEvents(
    @kotlinx.serialization.Transient
    val header: Header,
    @kotlinx.serialization.Transient
    val deserializer: Deserializer
) : StreamedEvents() {

    companion object : KSerializer<DeserializingStreamedEvents> {
        override val descriptor: SerialDescriptor
            get() = ArrayListClassDesc(TraceEvent.serializer().descriptor)

        override fun deserialize(decoder: Decoder): DeserializingStreamedEvents {
            throw UnsupportedOperationException()
        }

        override fun serialize(encoder: Encoder, obj: DeserializingStreamedEvents) {
            val collection = encoder.beginCollection(descriptor, -1, TraceEvent.serializer())
            obj.deserializer.readMessages(object : DeserializeToTraceEvents(obj.header) {
                var count = 0
                override fun consume(event: TraceEvent) {
                    val serializer = TraceEvent.serializer()
                    collection.encodeSerializableElement(serializer.descriptor, count++,  serializer, event)
                }
            })
            collection.endStructure(descriptor)


        }

    }
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

abstract class DeserializeToTraceEvents(val header: Header) : InflatedMessageVisitor() {

    var startTime = -1L

    abstract fun consume(event: TraceEvent)

    private fun timeMics(nanoTime: Long): Double {
        if (startTime == -1L) {
            startTime = nanoTime
        }
        return (nanoTime - startTime) * 1e-3
    }

    override fun visitMessage(kind: MeasureKind, tid: Long, time: Long) {
        val beginEvent = beginEvent
        val ts = timeMics(time)
        if (kind == MeasureKind.DURATION_END && beginEvent != null && beginEvent.tid == tid) {
            consume(beginEvent.copy(ph = Phase.Complete, dur = ts - beginEvent.ts))
            this.beginEvent = null
        } else {
            consume(TraceEvent(kind.toPhase(), ts = ts, pid = header.pid, tid = tid))
        }
    }



    override fun visitMessageWithName(kind: MeasureKind, tid: Long, name: String, time: Long) {
        smartConsume(TraceEvent(kind.toPhase(), name = name, cat = "test", ts = timeMics(time), pid = header.pid, tid = tid))
    }

    private var beginEvent: TraceEvent? = null

    private fun smartConsume(event: TraceEvent) {
        when(event.ph) {
            Phase.DurationStart -> {
                beginEvent?.let { consume(it) }
                beginEvent = event
            }
            else -> consume(event)
        }
    }

    override fun visitMessageWithArguments(
        kind: MeasureKind,
        tid: Long,
        name: String,
        argNames: List<String>,
        argValues: List<String>,
        time: Long
    ) {
        smartConsume(TraceEvent(
            kind.toPhase(), name = name, cat = "test", ts = timeMics(time), pid = header.pid, tid = tid,
            args = argNames.zip(argValues).toMap()
        ))
    }

}


fun convertModel(readForm: File): List<TraceEvent> {

    val deserializer = Deserializer(RandomAccessFileDeserializerInput(RandomAccessFile(readForm, "r")))
    val header = deserializer.readHeader()
    println(header)

    val traceEvents = mutableListOf<TraceEvent>()


    deserializer.readMessages(object : DeserializeToTraceEvents(header) {
        override fun consume(event: TraceEvent) {
            traceEvents += event
        }
    })

    return traceEvents
}


@UseExperimental(ImplicitReflectionSerializer::class)
fun jsonify(input: File, output: File, compress: Boolean = output.extension == "gz") {

    val json = JsonStream()

    val deserializer = Deserializer(RandomAccessFileDeserializerInput(RandomAccessFile(input, "r")))
    val header = deserializer.readHeader()
    println(header)

    val stream = if (compress) GZIPOutputStream(FileOutputStream(output)) else FileOutputStream(output)
    stream.bufferedWriter().use { outputWriter ->
        json.serializeTo(outputWriter,
            StreamedTraceRoot(
                DeserializingStreamedEvents(header, deserializer),
                displayTimeUnit = TimeUnit.NANOSECONDS
            )
        )
    }
}

@ImplicitReflectionSerializer
fun main(args: Array<String>) {

    jsonify(File(args[0]), File((args.getOrNull(1) ?: "output.trace")))
}
