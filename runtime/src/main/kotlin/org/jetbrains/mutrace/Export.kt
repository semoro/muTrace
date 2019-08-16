package org.jetbrains.mutrace

import org.jetbrains.mutrace.format.FormatVersion
import org.jetbrains.mutrace.format.InternerData
import org.jetbrains.mutrace.format.Serializer
import org.jetbrains.mutrace.format.SerializerOutput
import java.io.DataOutput
import java.io.File
import java.io.RandomAccessFile
import kotlin.reflect.KClass


private class RandomAccessFileSerializerOutput(val file: RandomAccessFile): SerializerOutput(), DataOutput by file {
    override fun ensureLength(len: Long) {
        file.setLength(file.filePointer + len)
    }

    override fun position(): Long {
        return file.filePointer
    }

    override fun seek(to: Long) {
        return file.seek(file.filePointer + to)
    }

    override fun seekAbs(pos: Long) {
        return file.seek(pos)
    }
}

object TraceDataExporter {


    fun exportTraceData(outputFile: File) {
        val storage = TraceCollector.storageForExport()
        val maps = Interner.mapsForExport()

        exportTraceData(storage, maps, outputFile, getProcessId() ?: 0)
    }

    private fun exportTraceData(storage: CollectorStorage, maps: InternerMaps, file: File, pid: Long) {

        val outputRf = RandomAccessFile(file, "rw")

        val serializer = Serializer(RandomAccessFileSerializerOutput(outputRf))
        serializer.serialize(FormatVersion.CURRENT, pid) {
            traceData(InternerData(maps.names, maps.arguments), storage.blocks.size) {
                for (block in storage.blocks) {
                    when (block) {
                        is StorageBlock.BufferBlock -> events(block.buffer, block.threadId)
                        is StorageBlock.StringsBlock -> strings(block.strings.ptr, block.strings.data, block.threadId)
                    }
                }
            }
        }

        outputRf.close()
    }
}