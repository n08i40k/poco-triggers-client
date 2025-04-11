package ru.n08i40k.poco.triggers.ui.overlay

import androidx.compose.ui.geometry.Offset
import ru.n08i40k.poco.triggers.ClickPos
import ru.n08i40k.poco.triggers.Trigger

data class OverlayTriggerData(
    val enabled: Boolean,
    val pos: Offset
) {
    companion object {
        fun fromDataStore(data: Trigger): OverlayTriggerData {
            return OverlayTriggerData(
                data.enabled,
                if (data.pos.x == 0 && data.pos.y == 0)
                    Offset(100f, 100f)
                else
                    Offset(data.pos.x.toFloat(), data.pos.y.toFloat())
            )
        }
    }

    fun toDataStore(builder: Trigger.Builder): Trigger {
        return builder
            .setEnabled(enabled)
            .setPos(ClickPos.newBuilder().setX(pos.x.toInt()).setY(pos.y.toInt()))
            .build()
    }
}