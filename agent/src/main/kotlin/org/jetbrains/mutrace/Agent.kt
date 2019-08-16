package org.jetbrains.mutrace

import org.jetbrains.mutrace.optimize.InternerTransformer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceClassVisitor
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.lang.RuntimeException
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.IllegalClassFormatException
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain
import kotlin.concurrent.thread

object Agent {
    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {

        instrumentation.addTransformer(object : ClassFileTransformer {
            override fun transform(
                loader: ClassLoader?,
                className: String,
                classBeingRedefined: Class<*>?,
                protectionDomain: ProtectionDomain?,
                classfileBuffer: ByteArray
            ): ByteArray? {



                val classReader = ClassReader(classfileBuffer)
                val node = ClassNode()

                val transformer: InternerTransformer
                    try {
                    transformer = InternerTransformer(Opcodes.ASM7, node)
                    classReader.accept(transformer, SKIP_FRAMES)
                } catch (e: Throwable) {
//                    thread { throw e }
                    throw IllegalClassFormatException().initCause(e)
                }
                if (!transformer.transformed) return null

                val writer = ClassWriter(COMPUTE_FRAMES)
                node.accept(writer)
//                println("transformed: $className")

//                node.accept(TraceClassVisitor(PrintWriter(System.err)))

                return writer.toByteArray()
            }
        })
    }
}