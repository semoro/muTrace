import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.40-eap-105"
}

subprojects {
    apply(plugin = "kotlin")
    group = "org.jetbrains"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("reflect"))
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}