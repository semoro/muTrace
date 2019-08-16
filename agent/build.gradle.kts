dependencies {
    compile(project(":optimizer"))
}
tasks.withType<Jar> {
    manifest {
        attributes(mapOf("PreMain-Class" to "org.jetbrains.mutrace.Agent"))
    }
}