package org.jetbrains.mutrace.format

import java.io.DataOutput
import java.nio.ByteBuffer

abstract class SerializerOutput: DataOutput {
    abstract fun ensureLength(len: Long)
    abstract fun position(): Long
    abstract fun seek(to: Long)

    abstract fun seekAbs(pos: Long)
}


inline class SerializerEventBufferLevel(val serializer: Serializer) {
    inline fun events(byteBuffer: ByteBuffer, tid: Long) = serializer.events(byteBuffer, tid)
    inline fun strings(count: Int, data: Array<String?>, tid: Long) = serializer.strings(count, data, tid)
}

inline class SerializerTopLevel(val serializer: Serializer) {

    inline fun traceData(internerData: InternerData, eventCount: Int, eventBuffers: SerializerEventBufferLevel.() -> Unit) {
        serializer.store(TraceDataHeader(internerData, EventBuffersHeader(eventCount)))
        SerializerEventBufferLevel(serializer).eventBuffers()
    }
}

private inline fun <T> Collection<T>.sumByLong(value: (T) -> Long) : Long =
    this.fold(0L) { acc, v -> acc + value(v) }

class Serializer(private val output: SerializerOutput) {

    inline fun serialize(version: FormatVersion, pid: Long, body: SerializerTopLevel.() -> Unit){
        store(Header(version, pid))
        SerializerTopLevel(this).body()
    }

    fun store(header: Header) {
        store(header.version)
        output.writeLong(header.pid)
    }
    fun store(version: FormatVersion) {
        output.writeInt(version.high)
        output.writeInt(version.low)
    }
    fun store(traceDataHeader: TraceDataHeader) {
        store(traceDataHeader.internerData)
        store(traceDataHeader.eventBuffersHeader)
    }

    fun store(eventBufferHeader: EventBuffersHeader) {
        output.writeInt(eventBufferHeader.size)
    }



    private inline fun eventBufferContainer(kind: Int, body: () -> Unit) {
        val pos = output.position()
        output.writeByte(kind) // KIND
        output.writeInt(0) // Next
        body()
        val endPos = output.position()
        output.seekAbs(pos + 1)
        val nextOffset = (endPos - pos)
        output.writeInt(nextOffset.toInt())
        output.seekAbs(endPos)
    }

    fun events(byteBuffer: ByteBuffer, tid: Long) {
        eventBufferContainer(0) {
            output.writeLong(tid)
            output.ensureLength(byteBuffer.position().toLong())
            output.writeInt(byteBuffer.position())
            output.write(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.position())
        }
    }

    fun strings(size: Int, strings: Array<String?>, tid: Long) {
        eventBufferContainer(1) {
            output.writeLong(tid)
            output.ensureLength(strings.take(size).sumByLong { it!!.length.toLong() })
            output.writeInt(size)
            for (i in 0 until size) {
                output.writeUTF(strings[i]!!)
            }
        }
    }


    private fun storeStr(string: String) {
        output.writeUTF(string)
    }

    private inline fun <K> storeKIdMap(map: Map<K, Int>, storeK: (K) -> Unit) {
        output.writeInt(map.size)
        for ((k, v) in map.entries) {
            storeK(k)
            output.writeInt(v)
        }
    }
    private inline fun <V> storeList(list: List<V>, storeV: (V) -> Unit) {
        output.writeInt(list.size)
        for (v in list) {
            storeV(v)
        }
    }

    fun store(internerData: InternerData) {
        storeKIdMap(internerData.names, this::storeStr)
        storeKIdMap(internerData.arguments) { storeList(it, this::storeStr) }
    }
}