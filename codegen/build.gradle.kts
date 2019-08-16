dependencies {
    compile("com.squareup:kotlinpoet:1.3.0")
}


val codegenClasspath by configurations.creating
dependencies {
    codegenClasspath(project)
}

val codegenOutputDir = buildDir.resolve("generated")

val runCodegen by tasks.creating(JavaExec::class) {
    this.classpath = codegenClasspath
    args(codegenOutputDir)
    workingDir(projectDir)
    main = "GeneratorKt"
    this.outputs.dir(codegenOutputDir)
}
