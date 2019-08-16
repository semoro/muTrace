

val codegenOutputDir = project(":codegen").buildDir.resolve("generated").resolve("stub")
val compileKotlin by tasks
compileKotlin.dependsOn(":codegen:runCodegen")

val main by kotlin.sourceSets
main.apply {
    this.kotlin.srcDir(codegenOutputDir)
}