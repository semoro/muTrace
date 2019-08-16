package org.jetbrains.mutrace

import org.junit.Test
import java.io.File
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
            for (i in 0 until 100_000)
                counter += computeRandom()
            println(counter)
        }

        println(recursive(10))
        TraceCollector.drainAll()
        TraceDataExporter.exportTraceData(File("output.mutrace.bin"))
    }
}