import GenerateRecordTime.muTraceIntrinsicAnnotationSpec
import com.squareup.kotlinpoet.*

object GenerateIntrinsics: Generator {

    val argumentCount = 16
    val internArgsMemberName = MemberName(packageName, "muTraceInternArgs")
    val mnInternStr = MemberName(packageName, "muTraceInternStr")


    fun generateNth(numArgs: Int): FunSpec {
        val argNameParameters =
            (0 until numArgs).map {
                ParameterSpec.builder("n$it", STRING).build()
            }

        return FunSpec.builder(internArgsMemberName.simpleName)
            .addAnnotation(muTraceIntrinsicAnnotationSpec)
            .addParameters(argNameParameters)
            .returns(INT)
            .addStatement("return Interner.intern(listOf(${argNameParameters.joinToString { "%N" }}))", *argNameParameters.toTypedArray())
            .build()
    }
    override fun generate(): FileSpec {
        val fileSpec = FileSpec.builder(packageName, "generatedIntrinsics")
            .addAnnotation(
                AnnotationSpec
                    .builder(ClassName("kotlin.jvm", "JvmMultifileClass"))
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .build()
            ).addAnnotation(
                AnnotationSpec
                    .builder(ClassName("kotlin.jvm", "JvmName"))
                    .addMember("%S", "MuTraceIntrinsics")
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .build()
            )
        for (i in 1 until GenerateIntrinsics.argumentCount) {
            fileSpec.addFunction(generateNth(i))
        }
        return fileSpec.build()
    }

}