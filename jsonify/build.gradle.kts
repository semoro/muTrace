plugins {
    id("kotlinx-serialization") version("1.3.40")
}

repositories {
    jcenter()
}

dependencies {
    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.11.1")
    compile(project(":binaryFormat"))
}