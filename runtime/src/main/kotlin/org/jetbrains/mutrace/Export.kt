package org.jetbrains.mutrace

import org.jetbrains.mutrace.format.*
import java.io.DataOutput
import java.io.File
import java.io.RandomAccessFile


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

    private data class RawTraceData(val maps: InternerMaps, val storage: CollectorStorage)
    private fun gatherData(): RawTraceData {
        val storage = TraceCollector.storageForExport()
        val maps = Interner.mapsForExport()
        return RawTraceData(maps, storage)
    }
    fun exportTraceData(outputFile: File) {
        exportTraceData(gatherData(), outputFile, getProcessId() ?: 0)
    }

    fun appendTraceData(outputFile: File) {
        if (!outputFile.exists()) TraceDataExporter.exportTraceData(outputFile)
        val data = gatherData()
        RandomAccessFile(outputFile, "rw").use { outputRf ->
            outputRf.seek(outputRf.length()) // move to end
            val serializer = Serializer(RandomAccessFileSerializerOutput(outputRf))
            serializer.appending {
                appendTraceData(data)
            }
        }
    }

    private fun SerializerTopLevel.appendTraceData(rawTraceData: RawTraceData) {
        val (maps, storage) = rawTraceData
        traceData(InternerData(maps.names, maps.arguments), storage.blocks.size) {
            for (block in storage.blocks) {
                when (block) {
                    is StorageBlock.BufferBlock -> events(block.buffer, block.threadId)
                    is StorageBlock.StringsBlock -> strings(block.strings.ptr, block.strings.data, block.threadId)
                }
            }
        }
    }

    private fun exportTraceData(rawTraceData: RawTraceData, file: File, pid: Long) {

        val outputRf = RandomAccessFile(file, "rw")

        val serializer = Serializer(RandomAccessFileSerializerOutput(outputRf))
        serializer.withHeader(FormatVersion.CURRENT, pid) {
            appendTraceData(rawTraceData)
        }

        outputRf.close()
    }
}