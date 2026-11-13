package pk.qist.customer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pk.qist.customer.ui.theme.*

/** A continuous card whose individual rows remain lazy, rather than one eagerly composed list item. */
internal fun <T> LazyListScope.panelSection(
    key: String,
    rows: List<T>,
    rowKey: (T) -> String,
    title: @Composable () -> Unit,
    footer: (@Composable () -> Unit)? = null,
    row: @Composable (T) -> Unit,
) {
    item(key = "$key:heading", contentType = "section-heading") {
        PanelSlice(first = true, last = rows.isEmpty() && footer == null) { title() }
    }
    itemsIndexed(rows, key = { _, value -> "$key:${rowKey(value)}" }, contentType = { _, _ -> key }) { index, value ->
        PanelSlice(last = index == rows.lastIndex && footer == null) {
            if (index > 0) HorizontalDivider(color = Divider)
            row(value)
        }
    }
    if (footer != null) item(key = "$key:footer", contentType = "section-footer") { PanelSlice(last = true) { footer() } }
}

@Composable
private fun PanelSlice(first: Boolean = false, last: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val square = CornerSize(0.dp)
    val corners = QistShapes.Card
    Surface(Modifier.fillMaxWidth(), color = Color.White, shape = corners.copy(
        topStart = if (first) corners.topStart else square, topEnd = if (first) corners.topEnd else square,
        bottomStart = if (last) corners.bottomStart else square, bottomEnd = if (last) corners.bottomEnd else square,
    )) {
        Column(Modifier.padding(horizontal = QistSpacing.Screen).padding(top = if (first) QistSpacing.Screen else 0.dp,
            bottom = if (last) QistSpacing.Screen else 0.dp), content = content)
    }
}

@Composable
internal fun AccountMessage(message: String) {
    Surface(color = DueBackground, shape = QistShapes.Input, modifier = Modifier.fillMaxWidth()) {
        Text(message, Modifier.padding(12.dp), color = Navy)
    }
}
