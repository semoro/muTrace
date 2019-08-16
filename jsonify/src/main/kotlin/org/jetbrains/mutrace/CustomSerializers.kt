package org.jetbrains.mutrace

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.isSubclassOf

@Serializer(forClass = Phase::class)
object PhaseSerializer {
    override val descriptor: SerialDescriptor
        get() = StringDescriptor


    val backMap =
        Phase::class.nestedClasses
            .filter { it.isSubclassOf(Phase::class) }
            .map { it.objectInstance }
            .associate { (it as Phase).ph to it }

    override fun deserialize(decoder: Decoder): Phase {
        val phaseName = decoder.decodeString()
        return backMap[phaseName] ?: error("Not supported ph: $phaseName")
    }

    override fun serialize(encoder: Encoder, obj: Phase) {
        encoder.encodeString(obj.ph)
    }

}


@Serializer(forClass = TimeUnit::class)
object TimeUnitSerializer {
    override val descriptor: SerialDescriptor
        get() = StringDescriptor

    override fun deserialize(decoder: Decoder): TimeUnit {
        return when(val timeUnitName = decoder.decodeString()) {
            "ms" -> TimeUnit.MILLISECONDS
            "ns" -> TimeUnit.NANOSECONDS
            else -> error("Not supported time-unit: $timeUnitName")
        }
    }

    override fun serialize(encoder: Encoder, obj: TimeUnit) {
        when(obj) {
            TimeUnit.MILLISECONDS -> encoder.encodeString("ms")
            TimeUnit.NANOSECONDS -> encoder.encodeString("ns")
            else -> error("Not supported time-unit: $obj")
        }
    }
}

