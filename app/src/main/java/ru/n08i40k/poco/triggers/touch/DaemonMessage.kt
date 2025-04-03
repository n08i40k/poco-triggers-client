package ru.n08i40k.poco.triggers.touch

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class TriggerSetting(
    val index: Int,
    val enabled: Boolean,
    val x: Int,
    val y: Int,
) {
    fun put(buffer: ByteBuffer) {
        buffer.putShort(index.toShort())
        buffer.putShort(if (enabled) 1 else 0)
        buffer.putInt(x)
        buffer.putInt(y)
    }
}

internal data class DaemonMessage(
    val upper: TriggerSetting,
    val lower: TriggerSetting,
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(24)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        upper.put(buffer)
        lower.put(buffer)

        return buffer.array()
    }
}


