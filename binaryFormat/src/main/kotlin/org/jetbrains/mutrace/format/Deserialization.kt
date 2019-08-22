package org.jetbrains.mutrace.format

import java.io.DataInput


abstract class DeserializerInput : DataInput {
    abstract fun remaining(): Long
    abstract fun position(): Long
    abstract fun seek(to: Long)
}



abstract class InflatedMessageVisitor {
    abstract fun visitMessage(kind: MeasureKind, tid: Long, time: Long)
    abstract fun visitMessageWithName(kind: MeasureKind, tid: Long, name: String, time: Long)
    abstract fun visitMessageWithArguments(
        kind: MeasureKind,
        tid: Long,
        name: String,
        argNames: List<String>,
        argValues: List<String>,
        time: Long
    )
}

class Deserializer(val input: DeserializerInput) {
    fun readHeader(): Header {
        return Header(readVersion(), input.readLong())
    }

    private fun readVersion(): FormatVersion {
        return FormatVersion(input.readInt(), input.readInt())
    }

    private fun readTraceDataHeader(): TraceDataHeader? {
        if (input.remaining() == 0L) return null
        return TraceDataHeader(readInternerData(), readEventBuffersHeader())
    }

    private fun readEventBuffersHeader(): EventBuffersHeader {
        return EventBuffersHeader(input.readInt())
    }


    private inner class MessageDataReader(
        val stringsPositionIndex: List<Pair<Long, StringsHeader>>,
        internerData: InternerData
    ) {

        val namesBackMap = internerData.names.entries.associate { it.value to it.key }
        val argumentsBackMap = internerData.arguments.entries.associate { it.value to it.key }

        fun readMessages(eventsHeader: EventsHeader, messageVisitor: InflatedMessageVisitor) {
            val endPosition = input.position() + eventsHeader.size
            while (input.position() < endPosition) {
                val kind = MeasureKind.values()[input.readInt()]
                when (kind) {
                    MeasureKind.DURATION_END -> {
                        val time = input.readLong()
                        messageVisitor.visitMessage(kind, eventsHeader.tid, time)
                    }
                    MeasureKind.INSTANT,
                    MeasureKind.DURATION -> {
                        val nameId = input.readInt()
                        val time = input.readLong()
                        messageVisitor.visitMessageWithName(kind, eventsHeader.tid, namesBackMap[nameId]!!, time)
                    }
                    MeasureKind.DURATION_WITH_ARGS,
                    MeasureKind.INSTANT_WITH_ARGS,
                    MeasureKind.COUNTER -> {
                        val nameId = input.readInt()
                        val argsId = input.readInt()
                        val argsSize = input.readInt()
                        val time = input.readLong()
                        messageVisitor.visitMessageWithArguments(
                            kind,
                            eventsHeader.tid,
                            namesBackMap[nameId]!!,
                            argumentsBackMap[argsId]!!,
                            getStrings(argsSize, eventsHeader.tid),
                            time
                        )
                    }
                }


            }
        }


        val stringsReaderPositions = mutableMapOf<Long, StringsReaderPosition>()

        inner class StringsReaderPosition(val header: StringsHeader, val stringsNumber: Int, var offset: Long) {

            var numStrings = 0
            fun remaining() = header.size - numStrings
        }



        fun getStringsReaderPosition(tid: Long): StringsReaderPosition {
            val readerPosition = stringsReaderPositions[tid]
            if (readerPosition == null || readerPosition.remaining() <= 0) {
                val pos = (readerPosition?.stringsNumber ?: -1) + 1
                val nextPos = pos + stringsPositionIndex.drop(pos).indexOfFirst { (_, header) -> header.tid == tid }
                val (offset, header) = stringsPositionIndex[nextPos]
                stringsReaderPositions[tid] = StringsReaderPosition(header, nextPos, offset)
            }
            return stringsReaderPositions[tid]!!
        }

        fun getStrings(size: Int, tid: Long): List<String> {
            val prevPosition = input.position()
            val readerPosition = getStringsReaderPosition(tid)
            input.seekAbs(readerPosition.offset)
            return mutableListOf<String>().also {
                for (n in 0 until size) {
                    it += readStr()
                }
                readerPosition.numStrings += size
                readerPosition.offset = input.position()
                input.seekAbs(prevPosition)
            }
        }
    }



    fun DeserializerInput.seekAbs(position: Long) {
        seek(position - position())
    }

    fun readMessages(messageVisitor: InflatedMessageVisitor) {
        while (true) {
            val traceDataHeader = readTraceDataHeader() ?: return
            val blockPositionIndex = mutableListOf<Pair<Long, EventBufferHeader>>()
            val eventBufferCount = traceDataHeader.eventBuffersHeader.size

            var lastPosition = 0L
            for (n in 0 until eventBufferCount) {
                val startPos = input.position()
                val eventBufferContainer = readEventBufferContainer()
                val header = readEventBufferHeader(eventBufferContainer)
                blockPositionIndex += input.position() to header
                input.seekAbs(startPos + eventBufferContainer.next)
                lastPosition = input.position()
            }
            @Suppress("UNCHECKED_CAST")
            val stringsPositionIndex =
                blockPositionIndex.filter { it.second is StringsHeader } as List<Pair<Long, StringsHeader>>
            with(MessageDataReader(stringsPositionIndex, traceDataHeader.internerData)) {
                for ((offset, block) in blockPositionIndex) {
                    input.seek(offset - input.position())
                    when (block) {
                        is EventsHeader -> readMessages(block, messageVisitor)
                        is StringsHeader -> {
                        }
                    }
                }
            }
            input.seekAbs(lastPosition)

        }
    }

    private fun readEventBufferHeader(eventBufferContainer: EventBufferContainer): EventBufferHeader {
        return when (val kind = eventBufferContainer.kind) {
            0 -> EventsHeader(input.readLong(), input.readInt())
            1 -> StringsHeader(input.readLong(), input.readInt())
            else -> error("Unknown kind: $kind")
        }
    }

    private fun readEventBufferContainer(): EventBufferContainer {
        return EventBufferContainer(input.readByte().toInt(), input.readInt())
    }

    fun readStr(): String {
        return input.readUTF()
    }

    private inline fun <K> readKIdMap(readKey: () -> K): Map<K, Int> {
        return mutableMapOf<K, Int>().also {
            for (i in 0 until input.readInt()) {
                it[readKey()] = input.readInt()
            }
        }
    }

    private inline fun <V> readList(readValue: () -> V): List<V> {
        return mutableListOf<V>().also {
            for (i in 0 until input.readInt()) {
                it += readValue()
            }
        }
    }

    private fun readInternerData(): InternerData {
        return InternerData(
            readKIdMap(this::readStr),
            readKIdMap { readList(this::readStr) }
        )
    }
}