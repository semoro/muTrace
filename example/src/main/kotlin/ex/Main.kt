package ex

import org.jetbrains.mutrace.*
import java.io.File
import java.nio.file.Files
import kotlin.concurrent.thread


fun fib(n: Int): Int = traceTime(__POS, "n", n) {
    return if (n <= 2) 1
    else fib(n - 2) + fib(n - 1)
}

fun main() {


    val threads = mutableListOf<Thread>()
    for(i in 16..20) {
        threads += thread {
            val r = fib(i)
            traceTime("println") {
                println(r)
            }
        }
    }

    threads.forEach { it.join() }


    TraceCollector.drainAll() // complete all measurements
    val tempFile = Files.createTempFile("trace", "dump.bin").toFile()
    TraceDataExporter.exportTraceData(tempFile) // export trace data in binary format to provided temp file
    jsonify(tempFile, File("output.trace")) // load binary trace data and convert to Chrome Tracing format

}