import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.40-eap-105"
}

repositories {
    mavenCentral()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
    maven { setUrl("https://dl.bintray.com/kotlin/kotlinx") }

    mavenLocal()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val agent by configurations.creating
dependencies {
    compile(kotlin("stdlib-jdk8"))

    val version = "1.0-SNAPSHOT"

    compile("org.jetbrains.muTrace:api:$version")

    compile("org.jetbrains.muTrace:jsonify:$version")

    runtime("org.jetbrains.muTrace:optimizer:$version")
    agent("org.jetbrains.muTrace:agent:$version") { isTransitive = false }
}

val exec by tasks.creating(JavaExec::class) {
    classpath = sourceSets["main"].runtimeClasspath
    dependsOn(agent)
    jvmArgs("-javaagent:${agent.singleFile}")

    main = "ex.MainKt"
}
