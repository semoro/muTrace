import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.40-eap-105"
    `maven-publish`
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    group = "org.jetbrains.muTrace"
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

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}