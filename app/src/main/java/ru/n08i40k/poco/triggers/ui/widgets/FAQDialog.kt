package ru.n08i40k.poco.triggers.ui.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

data class FAQInfo(
    @StringRes() val titleId: Int,
    @StringRes() val descriptionId: Int,
)

@Composable
fun FAQDialog(info: FAQInfo, onDismiss: () -> Unit) {
    Dialog(onDismiss) {
        val density = LocalDensity.current
        var titleWidth by remember { mutableStateOf(Dp.Unspecified) }

        Card(
            Modifier
                .padding(40.dp)
                .onGloballyPositioned { coords ->
                    with(density) { titleWidth = coords.size.width.toDp() }
                }
        ) {
            val modifier = Modifier.padding(10.dp)

            Card(
                if (titleWidth == Dp.Unspecified) modifier else Modifier.width(titleWidth),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.inverseOnSurface),
                shape = RoundedCornerShape(12.5.dp, 12.5.dp, 0.dp, 0.dp)
            ) {
                Text(
                    stringResource(info.titleId),
                    if (titleWidth == Dp.Unspecified) modifier else modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(stringResource(info.descriptionId), modifier)
        }
    }
}
