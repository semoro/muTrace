import kotlinx.benchmark.gradle.BenchmarkTarget
import kotlinx.benchmark.gradle.JvmBenchmarkExec
import kotlinx.benchmark.gradle.JvmBenchmarkTarget

plugins {
    id("kotlinx.benchmark") version "0.2.0-dev-2"
}

repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }
    mavenLocal()
}

val agent by configurations.creating

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx.benchmark.runtime:0.2.0-dev-2")
    implementation("org.jetbrains.kotlinx:kotlinx.benchmark.runtime-jvm:0.2.0-dev-2")
    compile("org.openjdk.jmh:jmh-core:1.21")
    compile(project(":api"))
    runtime(project(":optimizer"))
    agent(project(":agent"))  { isTransitive = false }
}





benchmark {
    targets {
        register("main") {
            this as JvmBenchmarkTarget
            jmhVersion = "1.21"
        }
    }
    configurations {
        val main by this
        main.apply {
            include("CounterBenchmark")
            include("DurationCounterBench")
        }
    }
}

tasks.withType<JvmBenchmarkExec> {
    dependsOn(agent)
    jvmArgs("-javaagent:${agent.singleFile}")
}