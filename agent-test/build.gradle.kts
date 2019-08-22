val agent by configurations.creating

repositories {
    jcenter()
}

dependencies {
    testCompile(kotlin("test"))
    testCompile(kotlin("test-junit"))
    testCompile(project(":api"))
    testCompile(project(":jsonify"))
    testRuntime(project(":optimizer"))
    agent(project(":agent")) { isTransitive = false }
}

tasks.withType<Test> {
    dependsOn(agent)
    jvmArgs("-javaagent:${agent.singleFile}")
}