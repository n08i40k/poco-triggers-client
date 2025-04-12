package ru.n08i40k.poco.triggers.ui.overlay

import android.view.Surface
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import ru.n08i40k.poco.triggers.touch.TriggerType


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
    onToggle: () -> Unit
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
                detectTapGestures(onDoubleTap = { onToggle() })
            })
}