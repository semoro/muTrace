import com.squareup.kotlinpoet.FileSpec
import java.io.File
import java.lang.StringBuilder




fun main(args: Array<String>) {
    val outputDir = File(args[0])
    GenerateRecordTime.generate().writeTo(outputDir.resolve("api"))
    GenerateRecordTime.generateStub().writeTo(outputDir.resolve("stub"))
    GenerateIntrinsics.generate().writeTo(outputDir.resolve("runtime"))
}

interface Generator {
    fun generate(): FileSpec
}