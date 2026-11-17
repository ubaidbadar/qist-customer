package pk.qist.customer.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import pk.qist.customer.ui.theme.Navy
import pk.qist.customer.ui.theme.QistShapes

/** One shared sheet style; individual screens own only their content and spacing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomerBottomSheet(
    onDismiss: () -> Unit,
    scrimColor: Color = Navy.copy(alpha = 0.60f),
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = QistShapes.Sheet,
        scrimColor = scrimColor,
        dragHandle = dragHandle,
        content = content,
    )
}
