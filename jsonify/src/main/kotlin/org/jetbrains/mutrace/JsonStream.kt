package org.jetbrains.mutrace

import kotlinx.serialization.*
import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.modules.EmptyModule
import kotlinx.serialization.modules.getContextualOrDefault
import java.io.Writer

class JsonStream(val encodeDefaults: Boolean = false, val prettyPrint: Boolean = false)
    : AbstractSerialFormat(EmptyModule) {
    fun <T> serializeTo(outputWriter: Writer, strategy: SerializationStrategy<T>, value: T) {
        JsonStreamingEncoder(outputWriter, this, WriteMode.OBJ).encode(strategy, value)
    }
}

internal enum class WriteMode(val begin: Char, val end: Char) {
    LIST('[', ']'), MAP('{', '}'), OBJ('{', '}')
}

internal class JsonStreamingEncoder(
    val writer: Writer, val format: JsonStream,
    private var mode: WriteMode
): ElementValueEncoder() {

    private val modeStack = ArrayList<WriteMode>()

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        val newMode = when(desc.kind) {
            UnionKind.POLYMORPHIC -> error("Unsupported")
            StructureKind.LIST -> WriteMode.LIST
            StructureKind.MAP -> {
                val keyKind = typeParams[0].descriptor.kind
                if (keyKind is PrimitiveKind || keyKind == UnionKind.ENUM_KIND)
                    WriteMode.MAP
                else WriteMode.LIST
            }
            else -> WriteMode.OBJ
        }
        modeStack.add(mode)
        mode = newMode
        writer.write(newMode.begin.toInt())
        identLevel++

        return this
    }


    private val prettyPrint get() = format.prettyPrint
    private var forceQuoted = false
    override fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean {
        return format.encodeDefaults
    }

    override fun endStructure(desc: SerialDescriptor) {
        identLevel--
        writer.write(mode.end.toInt())
        mode = modeStack.removeAt(modeStack.lastIndex)
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        require(serializer !is PolymorphicSerializer<*>)
        serializer.serialize(this, value)
    }



    private fun comma() {
        writer.write(",")
    }

    private fun colon() {
        writer.write(":")
    }

    private fun space() {
        if (prettyPrint) writer.write(" ")
    }


    private var first = false
    private fun resetFirst() { first = true }
    private fun ackFirst() = first.also { first = false }


    private fun eol() = writer.write("\n")
    private fun nextItem() {
        first = false
        if (prettyPrint) {
            eol()
            ident()
        }
    }

    private val identStr = "    "
    private var identLevel = 0
        set(value) {
            if (value > field) resetFirst()
            field = value
        }
    private fun ident() {
        if (prettyPrint) writer.write(identStr.repeat(identLevel))
    }



    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {

        when(mode) {
            WriteMode.LIST -> {
                if (!ackFirst()) comma()
                nextItem()
            }
            WriteMode.MAP -> {
                if (index % 2 == 0) {
                    if (!ackFirst()) comma()
                    nextItem()
                    forceQuoted = true
                } else {
                    colon()
                    space()
                    forceQuoted = false
                }
            }
            WriteMode.OBJ -> {
                if (!ackFirst()) comma()
                nextItem()
                encodeString(desc.getElementName(index))
                colon()
                space()
            }
        }
        return true
    }

    override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) {
        encodeString(enumDescription.getElementName(ordinal))
    }

    override fun encodeBoolean(value: Boolean) {
        maybeQuoted(value.toString())
    }

    override fun encodeByte(value: Byte) {
        maybeQuoted(value.toString())
    }

    override fun encodeChar(value: Char) {
        encodeString(value.toString())
    }

    override fun encodeDouble(value: Double) {
        maybeQuoted(value.toString())
    }

    override fun encodeFloat(value: Float) {
        maybeQuoted(value.toString())
    }

    override fun encodeInt(value: Int) {
        maybeQuoted(value.toString())
    }

    override fun encodeLong(value: Long) {
        maybeQuoted(value.toString())
    }

    override fun encodeShort(value: Short) {
        maybeQuoted(value.toString())
    }

    override fun encodeNull() {
        writer.write("null")
    }



    private fun maybeQuoted(value: String) {
        if (forceQuoted) quoted { writer.write(value) }
        else writer.write(value)
    }

    private inline fun quoted(body: () -> Unit) {
        writer.write("\"")
        body()
        writer.write("\"")
    }

    override fun encodeString(value: String) {
        quoted {
            writer.write(value)
        }
    }


    override fun encodeValue(value: Any) {
        encodeString(value.toString())
    }
}

@ImplicitReflectionSerializer
inline fun <reified T : Any> JsonStream.serializeTo(writer: Writer, obj: T) = serializeTo(writer, context.getContextualOrDefault(T::class), obj)
