package org.jetbrains.utrace.bench

import org.jetbrains.mutrace.TraceCollector
import org.jetbrains.mutrace.traceCounter
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@Warmup(time = 1, iterations = 10)
@Measurement(time = 1, iterations = 10)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class CounterBenchmark {

    var i = 0
    @Benchmark
    fun counterPerf(bh: Blackhole) {
        traceCounter("inc", "v", i++)
    }



    @TearDown(Level.Iteration)
    fun clearBuffers() {
        TraceCollector.drainAll()
        println("Clear, there was ${TraceCollector.storage.size} blocks")
        TraceCollector.clear()
    }
}