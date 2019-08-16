package org.jetbrains.mutrace

import org.jetbrains.mutrace.TraceCollector.checkStringsOverflow
import org.jetbrains.mutrace.format.MeasureKind
import java.lang.System.nanoTime
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

class StringBuf(val capacity: Int) {
    val data = arrayOfNulls<String?>(capacity)
    var ptr = 0
    fun put(string: String) {
        data[ptr++] = string
    }
    fun putAll(other: Array<String>) {
        for (s in other) {
            put(s)
        }
    }
    fun enoughCapacity(sizeToInsert: Int): Boolean {
        return ptr + sizeToInsert <= capacity
    }
}

class DurationBuffers(val thread: Thread) {
    var buffer = newBlock()
    fun newBlock() = ByteBuffer.allocate(blockSize)
    var strings = newStrings()
    fun newStrings() = StringBuf(stringsSize)

    fun nameId(id: Int): DurationBuffers {
        this.buffer.putInt(id)
        return this
    }



    fun putStr(value: Any?): DurationBuffers {
        if (value is String) this.strings.put(value)
        else if (value == null) this.strings.put("null")
        else this.strings.put(value.toString())

        return this
    }
}

const val blockSize = 1024 * 16 * 32
const val stringsSize = 1024 * 16 * 32

const val MAX_BUFFER_SIZE = 1024 * 1024 * 1024 * 2L // 1GB of buffers

sealed class StorageBlock(val threadId: Long) {
    abstract fun estimateSize(): Int
    class BufferBlock(val buffer: ByteBuffer, threadId: Long = Thread.currentThread().id) : StorageBlock(threadId) {
        override fun estimateSize(): Int = buffer.capacity()
    }
    class StringsBlock(val strings: StringBuf, threadId: Long = Thread.currentThread().id) : StorageBlock(threadId) {
        override fun estimateSize(): Int = strings.capacity * 32
    }
}

internal data class CollectorStorage(val blocks: List<StorageBlock>)

object TraceCollector {
    private val allDurationBuffers = ConcurrentLinkedQueue<DurationBuffers>()


    fun DurationBuffers.argsInfo(id: Int, size: Int): DurationBuffers {
        this.checkStringsOverflow(size)
        this.buffer.putInt(id)
        this.buffer.putInt(size)
        return this
    }

    private val buffers = ThreadLocal.withInitial {
        val currentThread = Thread.currentThread()
        DurationBuffers(currentThread).also {
            allDurationBuffers.add(it)
        }
    }
    val storage = mutableListOf<StorageBlock>()
    private var estimateStorageSize = 0L

    internal fun storageForExport() = synchronized(storage) {
        CollectorStorage(storage.toList())
    }

    fun clear() {
        synchronized(storage) {
            storage.clear()
            estimateStorageSize = 0
        }
    }

    private fun DurationBuffers.retire() = synchronized(this) {
        val old = buffer
        buffer = newBlock()
        retire(StorageBlock.BufferBlock(old))
    }

    private fun DurationBuffers.retireStrings() = synchronized(this) {
        val old = strings
        strings = newStrings()
        retire(StorageBlock.StringsBlock(old))
    }

    private fun DurationBuffers.retireAll() {
        retire()
        retireStrings()
    }

    fun drain() {
        buffers.get().retireAll()
    }

    fun drainAll() {
        allDurationBuffers.forEach { it.retireAll() }
    }

    private fun retire(block: StorageBlock) {
        synchronized(storage) {
            storage.add(block)
            estimateStorageSize += block.estimateSize()

            while (estimateStorageSize > MAX_BUFFER_SIZE) {
                System.err.println("μTrace buffer full! Consider drain data")
                estimateStorageSize -= storage.removeAt(0).estimateSize()
            }
        }
    }

    private fun DurationBuffers.checkOverflow(size: Int) {
        if (buffer.position() + size > blockSize) {
            retire()
        }
    }

    private fun DurationBuffers.checkStringsOverflow(size: Int) {
        if (!strings.enoughCapacity(size)) {
            retireStrings()
        }
    }

    private fun beginHeader(kind: MeasureKind, bSize: Int): DurationBuffers {
        val durationBuffers = buffers.get()
        durationBuffers.checkOverflow(KIND_SIZE + bSize)
        val buffer = durationBuffers.buffer
        buffer.putInt(kind.ordinal)
        return durationBuffers
    }

    private fun DurationBuffers.putTime(time: Long = nanoTime()): DurationBuffers {
        this.buffer.putLong(time)
        return this
    }




    object Duration {
        fun start(id: Int) {
            beginHeader(MeasureKind.DURATION, NAME_SIZE + TIME_SIZE)
                .nameId(id)
                .putTime()
        }

        fun startArgs(id: Int, argsId: Int, argsSize: Int): DurationBuffers {
            return beginHeader(MeasureKind.DURATION_WITH_ARGS, NAME_ARGS_TIME_SIZE)
                .nameId(id)
                .argsInfo(argsId, argsSize)
        }

        fun continueArgs(buffers: DurationBuffers) {
            buffers
                .putTime()
        }


        fun end() {
            val time = nanoTime()
            beginHeader(MeasureKind.DURATION_END, TIME_SIZE)
                .putTime(time)
        }

    }

    object SingleEvent {
        fun instant(id: Int) {
            beginHeader(MeasureKind.INSTANT, NAME_SIZE + TIME_SIZE)
                .nameId(id)
                .putTime()
        }
        fun startInstant(nameId: Int, argsId: Int, argsSize: Int): DurationBuffers {
            return startSingleArgs(MeasureKind.INSTANT_WITH_ARGS, nameId, argsId, argsSize)
        }
        fun startCounter(nameId: Int, argsId: Int, argsSize: Int): DurationBuffers {
            return startSingleArgs(MeasureKind.COUNTER, nameId, argsId, argsSize)
        }

        fun startSingleArgs(kind: MeasureKind, nameId: Int, argsId: Int, argsSize: Int): DurationBuffers {
            return beginHeader(kind, NAME_ARGS_TIME_SIZE)
                .nameId(nameId)
                .argsInfo(argsId, argsSize)
        }

        fun endSingle(buffers: DurationBuffers) {
            buffers
                .putTime()
        }

        fun startMetadata(nameId: Int, argsId: Int, argsSize: Int): DurationBuffers {
            return startSingleArgs(MeasureKind.METADATA, nameId, argsId, argsSize)
        }

    }
}


private const val ARGS_SIZE = 4 + 4
private const val NAME_SIZE = 4
private const val KIND_SIZE = 4
private const val TIME_SIZE = 8

private const val NAME_ARGS_TIME_SIZE = NAME_SIZE + ARGS_SIZE + TIME_SIZE
