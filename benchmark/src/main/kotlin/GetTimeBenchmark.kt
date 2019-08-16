package org.jetbrains.utrace.bench

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.*
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(time = 1)
@Measurement(time = 1)
@State(Scope.Benchmark)
open class GetTimeBenchmark {

    @Benchmark
    fun nanoTime(bh: Blackhole) {
        bh.consume(System.nanoTime())
    }

    @Benchmark
    fun millisTime(bh: Blackhole) {
        bh.consume(System.currentTimeMillis())
    }

}

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(time = 1)
@Measurement(time = 1)
open class ThreadLocalBenchmark {
    lateinit var threadLocal: ThreadLocal<String>

    @Setup
    fun setupThreadLocal() {
        threadLocal = object : ThreadLocal<String>() {
            override fun initialValue(): String {
                return Math.random().toString()
            }
        }
    }

    @Benchmark
    fun threadLocal(bh: Blackhole) {
        bh.consume(threadLocal.get())
    }
}