import GenerateIntrinsics.mnInternStr
import com.squareup.kotlinpoet.*
import java.lang.StringBuilder

val packageName = "org.jetbrains.mutrace"

object GenerateRecordTime : Generator {

    val muTraceIntrinsicAnnotationSpec = AnnotationSpec.builder(ClassName.bestGuess("org.jetbrains.mutrace.MuTraceIntrinsic")).build()

    val traceCollectorClassName = ClassName("org.jetbrains.mutrace", "TraceCollector")
    val cnDurationCollector = traceCollectorClassName.nestedClass("Duration")
    val mnStartArgs = MemberName(cnDurationCollector, "startArgs")
    val mnContinueArgs = MemberName(cnDurationCollector, "continueArgs")
    val mnEndRecordTime = MemberName(packageName, "endTraceTime")
    val cnSingleEventCollector = traceCollectorClassName.nestedClass("SingleEvent")
    val mnCounterStart = MemberName(cnSingleEventCollector, "startCounter")
    val mnInstantStart = MemberName(cnSingleEventCollector, "startInstant")
    val mnEndSingle = MemberName(cnSingleEventCollector, "endSingle")

    private fun generateRecordTime(numArgs: Int): FunSpec {
        val typeVariable = TypeVariableName("T")
        val nameParam = ParameterSpec.builder("name", STRING).build()
        val blockParam = ParameterSpec.builder("block", LambdaTypeName.get(returnType = typeVariable)).build()
        val (argNameParameters, argValueParameters) = generateArgParameters(numArgs)

        val argParameters = argNameParameters.zip(argValueParameters)
        val funSpec = FunSpec.builder(DURATION_NAME)
            .addModifiers(KModifier.INLINE)
            .addTypeVariable(typeVariable)
            .addParameter(nameParam)
            .addParameters(argParameters.flatMap { (a, b) -> listOf(a, b) })
            .addParameter(blockParam)
            .returns(typeVariable)
            .addStatement("val nameId = %M(%N)", mnInternStr, nameParam)
            .addStatement("val argsId = %M(${argNameParameters.joinToString { "%N" }})",
                GenerateIntrinsics.internArgsMemberName,
                *argNameParameters.toTypedArray()
            )
            .addCode("""
                |val durationBuffers = 
                |   %M(nameId, argsId, $numArgs)
                |${"        .putStr(%N)\n".repeat(numArgs)}
                |
            """.trimMargin(), mnStartArgs, *argValueParameters.toTypedArray())
        funSpec
            .addStatement("%M(durationBuffers)", mnContinueArgs)
            .addStatement("return %M(%N)", mnEndRecordTime, blockParam)


        return funSpec.build()
    }

    private fun generateArgParameters(numArgs: Int): Pair<List<ParameterSpec>, List<ParameterSpec>> {
        val argNameParameters =
            (0 until numArgs).map {
                ParameterSpec.builder("n$it", STRING).build()
            }
        val argValueParameters =
            (0 until numArgs).map {
                ParameterSpec.builder("v$it", ANY.copy(nullable = true)).build()
            }
        return argNameParameters to argValueParameters
    }

    private fun generateSingle(numArgs: Int, name: String, startCall: MemberName): FunSpec {
        val nameParam = ParameterSpec.builder("name", STRING).build()
        val (argNameParameters, argValueParameters) = generateArgParameters(numArgs)
        val argParameters = argNameParameters.zip(argValueParameters)
        val funSpec = FunSpec.builder(name)
            .addModifiers(KModifier.INLINE)
            .addParameter(nameParam)
            .addParameters(argParameters.flatMap { (a, b) -> listOf(a, b) })
            .returns(UNIT)
            .addStatement("val nameId = %M(%N)", mnInternStr, nameParam)
            .addStatement("val argsId = %M(${argNameParameters.joinToString { "%N" }})",
                GenerateIntrinsics.internArgsMemberName,
                *argNameParameters.toTypedArray()
            )
            .addCode("""
                |val durationBuffers = 
                |   %M(nameId, argsId, $numArgs)
                |${"        .putStr(%N)\n".repeat(numArgs)}
                |
            """.trimMargin(), startCall, *argValueParameters.toTypedArray())
        funSpec
            .addStatement("%M(durationBuffers)", mnEndSingle)

        return funSpec.build()
    }


    private fun suppressAnnotationSpec(vararg suppress: String): AnnotationSpec =
        AnnotationSpec.builder(Suppress::class.asClassName())
            .addMember(suppress.joinToString { "%S" }, *suppress)
            .build()

    override fun generate(): FileSpec {
        val fileSpec = FileSpec.builder(packageName, "generatedTraceWithArgs")

        fileSpec.addAnnotation(suppressAnnotationSpec("NOTHING_TO_INLINE"))

        for (i in 1 until GenerateIntrinsics.argumentCount) {
            fileSpec.addFunction(generateRecordTime(i))
            fileSpec.addFunction(generateSingle(i, INSTANT_NAME, mnInstantStart))
            fileSpec.addFunction(generateSingle(i, COUNTER_NAME, mnCounterStart))
        }
        return fileSpec.build()
    }


    fun generateStubFor(spec: FunSpec, hasBlock: Boolean): FunSpec {
        return FunSpec.builder(spec.name)
            .addAnnotations(spec.annotations)
            .addModifiers(spec.modifiers)
            .apply { spec.returnType?.let { returns(it) } }
            .addTypeVariables(spec.typeVariables)
            .addParameters(spec.parameters)
            .apply { if (hasBlock) addStatement("return %N()", spec.parameters.last()) }
            .build()
    }

    fun generateStub(): FileSpec {
        val fileSpec = FileSpec.builder(packageName, "generatedTraceWithArgs")

        fileSpec.addAnnotation(suppressAnnotationSpec("NOTHING_TO_INLINE"))

        for (i in 1 until GenerateIntrinsics.argumentCount) {
            fileSpec.addFunction(generateStubFor(generateRecordTime(i), hasBlock = true))
            fileSpec.addFunction(generateStubFor(generateSingle(i, INSTANT_NAME, mnInstantStart), hasBlock = false))
            fileSpec.addFunction(generateStubFor(generateSingle(i, COUNTER_NAME, mnCounterStart), hasBlock = false))
        }
        return fileSpec.build()
    }

    val COUNTER_NAME = "traceCounter"
    val INSTANT_NAME = "traceInstant"
    val DURATION_NAME = "traceTime"
}