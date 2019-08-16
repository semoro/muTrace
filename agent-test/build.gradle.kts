val agent by configurations.creating

dependencies {
    testCompile(kotlin("test"))
    testCompile(kotlin("test-junit"))
    testCompile(project(":api"))
    testRuntime(project(":optimizer"))
    agent(project(":agent")) { isTransitive = false }
}

tasks.withType<Test> {
    dependsOn(agent)
    jvmArgs("-javaagent:${agent.singleFile}")
}