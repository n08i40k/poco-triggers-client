package ru.n08i40k.poco.triggers.ui.overlay

import android.content.Intent
import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.n08i40k.poco.triggers.Application
import ru.n08i40k.poco.triggers.ClickPos
import ru.n08i40k.poco.triggers.Settings
import ru.n08i40k.poco.triggers.proto.settings
import ru.n08i40k.poco.triggers.service.OverlayService
import ru.n08i40k.poco.triggers.touch.TriggerType

private data class Trigger(
    val enabled: Boolean, val pos: Offset
) {
    companion object {
        fun fromDataStore(data: ru.n08i40k.poco.triggers.Trigger): Trigger {
            return Trigger(
                data.enabled,
                if (data.pos.x == 0 && data.pos.y == 0)
                    Offset(100f, 100f)
                else
                    Offset(data.pos.x.toFloat(), data.pos.y.toFloat())
            )
        }
    }

    fun toDataStore(builder: ru.n08i40k.poco.triggers.Trigger.Builder): ru.n08i40k.poco.triggers.Trigger {
        return builder
            .setEnabled(enabled)
            .setPos(ClickPos.newBuilder().setX(pos.x.toInt()).setY(pos.y.toInt()))
            .build()
    }
}

@Composable
fun AppOverlay() {
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

    val app = context.applicationContext as Application
    var triggers = app.triggers

    var upperTrigger by remember { mutableStateOf(Trigger.fromDataStore(triggers.upper)) }
    var lowerTrigger by remember { mutableStateOf(Trigger.fromDataStore(triggers.lower)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0, 0, 0, 150))
    ) {
        Box(Modifier.fillMaxSize(), Alignment.CenterEnd) {
            IconButton({
                app.triggers = triggers.toBuilder()
                    .setUpper(upperTrigger.toDataStore(triggers.upper.toBuilder()))
                    .setLower(lowerTrigger.toDataStore(triggers.lower.toBuilder())).build()

                coroutineScope.launch {
                    context.settings.updateData {
                        Settings.getDefaultInstance().toBuilder().setTriggers(app.triggers).build()
                    }

                    app.sendSetting()

                    val intent = Intent(context.applicationContext, OverlayService::class.java)
                    context.stopService(intent)
                }

            }) {
                Icon(
                    Icons.Default.Close,
                    "Close",
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.inverseSurface
                )
            }
        }

        DraggableCircle(
            TriggerType.UPPER,
            upperTrigger.enabled,
            upperTrigger.pos,
            { upperTrigger = upperTrigger.copy(pos = upperTrigger.pos + it) }) {
            upperTrigger = upperTrigger.copy(enabled = !upperTrigger.enabled)
        }
        DraggableCircle(
            TriggerType.LOWER,
            lowerTrigger.enabled,
            lowerTrigger.pos,
            { lowerTrigger = lowerTrigger.copy(pos = lowerTrigger.pos + it) }) {
            lowerTrigger = lowerTrigger.copy(enabled = !lowerTrigger.enabled)
        }
    }
}

private fun convertInputOffset(rotation: Int, screenSize: Size, offset: Offset): Offset {
    return when (rotation) {
        Surface.ROTATION_0   -> offset

        Surface.ROTATION_90  -> {
            Offset(
                offset.y, screenSize.width - offset.x - 1f
            )
        }

        Surface.ROTATION_180 -> {
            Offset(
                screenSize.width - offset.x - 1, screenSize.height - offset.y - 1f
            )
        }

        Surface.ROTATION_270 -> {
            Offset(
                screenSize.height - offset.y - 1f, offset.x
            )
        }

        else                 -> throw Exception()
    }
}

private fun convertDrag(rotation: Int, offset: Offset): Offset {
    return when (rotation) {
        Surface.ROTATION_0   -> offset

        Surface.ROTATION_90  -> {
            Offset(
                -offset.y, offset.x
            )
        }

        Surface.ROTATION_180 -> {
            Offset(
                -offset.x, -offset.y
            )
        }

        Surface.ROTATION_270 -> {
            Offset(
                offset.y, -offset.x
            )
        }

        else                 -> throw Exception()
    }
}

@Composable
fun DraggableCircle(
    type: TriggerType,
    enabled: Boolean,
    offset: Offset,
    onDrag: (Offset) -> Unit,
    onSetEnabled: () -> Unit
) {
    val color = when (type) {
        TriggerType.UPPER -> Color.Red
        TriggerType.LOWER -> Color.Blue
    }

    val view = LocalView.current
    val density = LocalDensity.current
    val conf = LocalConfiguration.current

    val screenSize = remember(conf.orientation) {
        var screenSize = with(density) {
            Size(
                view.resources.displayMetrics.widthPixels.toFloat(),
                view.resources.displayMetrics.heightPixels.toFloat()
            )
        }

        if (screenSize.height < screenSize.width) screenSize =
            Size(screenSize.height, screenSize.width)

        screenSize
    }

    val offset = remember(conf.orientation, view.display.rotation, offset) {
        convertInputOffset(
            view.display.rotation, screenSize, offset
        )
    }

    Box(
        Modifier
            .graphicsLayer {
                translationX = offset.x - with(density) { 25.dp.toPx() }
                translationY = offset.y - with(density) { 25.dp.toPx() }
                alpha = if (enabled) 1f else 0.25f
            }
            .size(50.dp)
            .background(color = color, shape = CircleShape)
            .pointerInput(0) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(
                        convertDrag(
                            view.display.rotation, dragAmount
                        )
                    )
                }
            }
            .pointerInput(1) {
                detectTapGestures(onDoubleTap = { onSetEnabled() })
            })
}