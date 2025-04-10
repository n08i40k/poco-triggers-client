package ru.n08i40k.poco.triggers.daemon

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal data class DaemonMessage(
    val upper: Trigger,
    val lower: Trigger,
) {
    internal data class Trigger(
        val enabled: Boolean,
        val x: Int,
        val y: Int,
    ) {
        fun put(buffer: ByteBuffer) {
            // bool - 1 byte
            buffer.put(if (enabled) 1 else 0)

            // C++ struct alignment to 4 bytes
            buffer.put(0)
            buffer.put(0)
            buffer.put(0)

            // std::int32_t - 4 bytes
            buffer.putInt(x)

            // std::int32_t - 4 bytes
            buffer.putInt(y)
        }
    }

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(24)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        upper.put(buffer)
        lower.put(buffer)

        return buffer.array()
    }
}


