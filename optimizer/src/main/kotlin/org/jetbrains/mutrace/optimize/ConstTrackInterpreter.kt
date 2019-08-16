package org.jetbrains.mutrace.optimize

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.Value


class ConstValue(val type: Type?, val insn: AbstractInsnNode?, val prev: ConstValue?) : Value {
    override fun getSize(): Int = type?.size ?: 1
}

class ConstTrackInterpreter(api: Int) : Interpreter<ConstValue>(api) {

    companion object {
        val UNINITIALIZED = ConstValue(null, null, null)
    }

    override fun newValue(type: Type?): ConstValue? {
        if (type == Type.VOID_TYPE) return null
        return UNINITIALIZED
    }

    override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out ConstValue>?): ConstValue? {
        return UNINITIALIZED
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode?,
        value1: ConstValue?,
        value2: ConstValue?,
        value3: ConstValue?
    ): ConstValue? {
        return UNINITIALIZED
    }

    override fun merge(value1: ConstValue?, value2: ConstValue?): ConstValue? {
        return UNINITIALIZED
    }

    override fun returnOperation(insn: AbstractInsnNode?, value: ConstValue?, expected: ConstValue?) {}

    override fun unaryOperation(insn: AbstractInsnNode, value: ConstValue?): ConstValue? {
        if (insn.opcode == Opcodes.CHECKCAST && value != null) {
            return ConstValue(value.type, insn, prev = value)
        }
        return UNINITIALIZED
    }

    override fun binaryOperation(insn: AbstractInsnNode?, value1: ConstValue?, value2: ConstValue?): ConstValue? {
        return UNINITIALIZED
    }

    override fun copyOperation(insn: AbstractInsnNode, value: ConstValue?): ConstValue? {
        if (value == null) return null
        return ConstValue(value.type, insn, value)
    }

    override fun newOperation(insn: AbstractInsnNode): ConstValue? {
        if (insn.opcode == Opcodes.LDC) {
            (insn as LdcInsnNode)

            return ConstValue(Type.getType(insn.cst::class.java), insn, null)
        }
        return UNINITIALIZED
    }

}