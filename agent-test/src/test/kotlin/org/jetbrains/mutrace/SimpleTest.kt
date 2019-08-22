package org.jetbrains.mutrace

import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.*

class SimpleTest {

    fun recursive(n: Int): Int {
        traceTime(__POS, "n", n) {
            return if (n < 0) 10
            else recursive(n - 1)
        }
    }

    val random = Random()

    fun computeRandom(): Int = traceTime {
        val int = random.nextInt(128)
        return if (random.nextFloat() > 0.5) {
            int + computeRandom()
        } else {
            int - 1
        }
    }

    @Test fun simpleMeasureTime() {
        traceTime("simple") {
            Thread.sleep(1)
        }
        traceTime("args", "a", 1, "b", 2) {
            Thread.sleep(1)
        }

        traceTime("random") {
            var counter = 0
            for (i in 0 until 100)
                counter += computeRandom()
            println(counter)
        }

        println(recursive(10))

        exportJson(File("output.2.trace"))
    }

    @Test fun simpleCountUp() {

        for (i in 0..100_000) {
            traceTime(__POS, "v", i, "g", i, "c", i, "w", i, "q", i, "a", i) {
                random.nextInt(128)
            }
        }
        exportJson(File("output.trace.gz"))
    }
}

fun exportJson(outputFile: File) {
    val tempFile = Files.createTempFile("trace", "dump.bin").toFile()
    TraceCollector.drainAll()

    TraceDataExporter.appendTraceData(tempFile) // export trace data in binary format to provided temp file
    println(TraceCollector.storage.toList().groupBy { it::class }.mapValues { it.value.size })
    jsonify(tempFile, outputFile) // load binary trace data and convert to Chrome Tracing format
}