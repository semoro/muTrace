package org.jetbrains.mutrace.optimize

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.Value


class ConstValue(
    val type: Type?,
    val insn: AbstractInsnNode?,
    val prev: ConstValue?,
    val originKind: String
) : Value {
    override fun toString(): String {
        return "ConstValue(type = $type, insn = $insn, hasPrev = ${prev != null}, origin = $originKind)"
    }
    override fun getSize(): Int = type?.size ?: 1
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConstValue

        if (type != other.type) return false
        if (insn != other.insn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type?.hashCode() ?: 0
        result = 31 * result + (insn?.hashCode() ?: 0)
        return result
    }


}

class ConstTrackInterpreter(api: Int) : Interpreter<ConstValue>(api) {

    companion object {
        val UNINITIALIZED_NEW = ConstValue(null, null, null, "newValue")
        val UNINITIALIZED_NEWOP = ConstValue(null, null, null, "newOp")
        val UNINITIALIZED_NARY = ConstValue(null, null, null, "naryOp")
        val UNINITIALIZED_TERNARY = ConstValue(null, null, null, "ternaryOp")
        val UNINITIALIZED_BINARY = ConstValue(null, null, null, "binaryOp")
        val UNINITIALIZED_UNARY = ConstValue(null, null, null, "unaryOp")
    }

    override fun newValue(type: Type?): ConstValue? {
        if (type == Type.VOID_TYPE) return null
        return UNINITIALIZED_NEW
    }

    override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out ConstValue>?): ConstValue? {
        return UNINITIALIZED_NARY
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode?,
        value1: ConstValue?,
        value2: ConstValue?,
        value3: ConstValue?
    ): ConstValue? {
        return UNINITIALIZED_TERNARY
    }

    override fun merge(value1: ConstValue?, value2: ConstValue?): ConstValue? {
        if (value1 == value2) return value1
        return ConstValue(null, null, null, originKind = "merge: $value1, $value2")
    }

    override fun returnOperation(insn: AbstractInsnNode?, value: ConstValue?, expected: ConstValue?) {}

    override fun unaryOperation(insn: AbstractInsnNode, value: ConstValue?): ConstValue? {
        if (insn.opcode == Opcodes.CHECKCAST && value != null) {
            return ConstValue(value.type, insn, prev = value, originKind = "unary")
        }
        return UNINITIALIZED_UNARY
    }

    override fun binaryOperation(insn: AbstractInsnNode?, value1: ConstValue?, value2: ConstValue?): ConstValue? {
        return UNINITIALIZED_BINARY
    }

    override fun copyOperation(insn: AbstractInsnNode, value: ConstValue?): ConstValue? {
        if (value == null) return null
        return ConstValue(value.type, insn, value, "copy")
    }

    override fun newOperation(insn: AbstractInsnNode): ConstValue? {
        if (insn.opcode == Opcodes.LDC) {
            (insn as LdcInsnNode)

            return ConstValue(Type.getType(insn.cst::class.java), insn, null, "newOp")
        }
        return UNINITIALIZED_NEWOP
    }

}