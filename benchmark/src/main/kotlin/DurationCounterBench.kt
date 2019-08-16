package org.jetbrains.utrace.bench

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.jetbrains.mutrace.TraceCollector
import org.jetbrains.mutrace.traceTime
import java.util.concurrent.TimeUnit

@Warmup(time = 1, iterations = 10)
@Measurement(time = 1, iterations = 10)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class DurationCounterBench {
    @Benchmark
    fun measureTime(bh: Blackhole) {
        traceTime("time") {
            bh.consume("")
        }
    }


    var counter = 0

    @Benchmark
    fun measureTimeWithArgs(bh: Blackhole) {
        traceTime("withArgs", "counter", counter, "abc", 15, "cdz", counter, "fup", counter) {
            counter++
            bh.consume("")
        }
    }

    @Benchmark
    fun baseline(bh: Blackhole) {
        bh.consume("")
    }

    @TearDown(Level.Iteration)
    fun clearBuffers() {
        TraceCollector.drainAll()
        println("Clear, there was ${TraceCollector.storage.size} blocks")
        TraceCollector.clear()
    }
}