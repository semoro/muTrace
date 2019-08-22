package org.jetbrains.mutrace.optimize

import org.jetbrains.mutrace.Interner
import org.jetbrains.mutrace.positionString
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.analysis.Analyzer
import kotlin.IllegalArgumentException
import kotlin.IllegalStateException

class InternerTransformer(api: Int, classVisitor: ClassVisitor?) : ClassVisitor(api, classVisitor) {

    var transformed = false
    lateinit var className: String

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    var sourceName: String? = null

    override fun visitSource(source: String?, debug: String?) {
        sourceName = source
        super.visitSource(source, debug)
    }

    override fun visitMethod(
        access: Int,
        methodName: String,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val parentVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions)

        return object : MethodNode(api, access, methodName, descriptor, signature, exceptions) {

            var shouldAnalyze = false
            override fun visitMethodInsn(
                opcodeAndSource: Int,
                owner: String?,
                name: String?,
                descriptor: String?,
                isInterface: Boolean
            ) {
                if (owner == INTRINSICS_ROOT) shouldAnalyze = true
                super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
            }


            var line = 0

            fun findLdc(value: ConstValue): LdcInsnNode {
                val valueFlow = generateSequence(value) { it.prev }
                return valueFlow.map { it.insn }
                    .filterIsInstance<LdcInsnNode>()
                    .singleOrNull() ?: reportNonConstArgumentError()
            }

            fun reportNonConstArgumentError(): Nothing {
                val ex = IllegalArgumentException("μTrace: Unexpected non-const argument")
                ex.stackTrace = arrayOf(StackTraceElement(Type.getObjectType(className).className, methodName, sourceName, line))
                throw IllegalStateException(ex)
            }

            override fun visitEnd() {
                super.visitEnd()
                try {
                if (shouldAnalyze) {

                    this.instructions.asSequence()
                        .onEach {
                            if (it is LineNumberNode) {
                                line = it.line
                            }
                        }.filter { it is MethodInsnNode && it.owner == INTRINSICS_ROOT && it.name == POSITION_INTRINSIC_NAME }
                        .forEach { methodInsn ->
                            methodInsn as MethodInsnNode
                            val position =
                                positionString(
                                    Type.getObjectType(className).className,
                                    methodName,
                                    sourceName,
                                    line
                                )
                            instructions.set(methodInsn, LdcInsnNode(position))
                        }

                    val frames = Analyzer(ConstTrackInterpreter(api)).analyze(className, this)

                    val intrinsicCalls =
                        this.instructions.asSequence()
                            .onEach {
                                if (it is LineNumberNode) {
                                    line = it.line
                                }
                            }
                            .filter { it is MethodInsnNode && it.owner == INTRINSICS_ROOT }

                    intrinsicCalls.mapNotNull { methodInsn ->
                        methodInsn as MethodInsnNode
                        when (methodInsn.name) {

                            NAME_INTERN_NAME -> {
                                val frame = frames[instructions.indexOf(methodInsn)]
                                val value = frame.getStack(frame.stackSize - 1)

                                val ldc = findLdc(value)
                                val id = Interner.intern(ldc.cst as String)

                                fun() {
                                    instructions.set(methodInsn, LdcInsnNode(id))
                                    instructions.remove(value.insn)
                                }
                            }
                            ARGS_INTERN_NAME -> {
                                val frame = frames[instructions.indexOf(methodInsn)]

                                val list = List(frame.stackSize) {
                                    val value = frame.getStack(it)

                                    val ldc = findLdc(value)
                                    ldc.cst as String
                                }

                                val id = Interner.intern(list)

                                fun() {
                                    instructions.set(methodInsn, LdcInsnNode(id))
                                    repeat(frame.stackSize) { index ->
                                        frame.getStack(index).insn?.let { instructions.remove(it) }
                                    }
                                }
                            }
                            POSITION_INTRINSIC_NAME -> null
                            else -> null
                        }
                    }
                        .toList()
                        .forEach { it() }
                    transformed = true
                }
                } catch(t: Throwable) {
                    t.printStackTrace()
                    throw t
                }
                this.accept(parentVisitor)

            }

        }
    }



}


class InsnSequence(val from: AbstractInsnNode, val to: AbstractInsnNode?) : Sequence<AbstractInsnNode> {
    constructor(insnList: InsnList) : this(insnList.first, null)

    override fun iterator(): Iterator<AbstractInsnNode> {
        return object : Iterator<AbstractInsnNode> {
            var current: AbstractInsnNode? = from
            override fun next(): AbstractInsnNode {
                val result = current
                current = current!!.next
                return result!!
            }

            override fun hasNext() = current != to
        }
    }
}

fun InsnList.asSequence() = InsnSequence(this)

const val INTRINSICS_ROOT = "org/jetbrains/mutrace/MuTraceIntrinsics"
const val NAME_INTERN_NAME = "muTraceInternStr"
const val ARGS_INTERN_NAME = "muTraceInternArgs"
const val POSITION_INTRINSIC_NAME = "muTraceCurrentPosition"