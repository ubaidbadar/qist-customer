package pk.qist.customer.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import pk.qist.customer.ui.theme.Blue

/** Pull progress belongs to the current gesture, never to restored Activity or account state. */
@Composable
internal fun CustomerRefreshBox(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // Material's rememberPullToRefreshState saves the offset, including interrupted pulls.
    val pull = remember { PullToRefreshState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val currentRefreshing by rememberUpdatedState(refreshing)
    var indicatorAllowed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle, pull) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> indicatorAllowed = false
                Lifecycle.Event.ON_RESUME -> scope.launch {
                    // Reset before revealing; otherwise a stale offset can flash for one frame.
                    if (!currentRefreshing) pull.snapTo(0f)
                    indicatorAllowed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { if (indicatorAllowed) onRefresh() },
        state = pull,
        modifier = modifier,
        indicator = {
            if (indicatorAllowed && (refreshing || pull.distanceFraction > 0f)) {
                PullToRefreshDefaults.Indicator(
                    state = pull,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter).testTag("customer-refresh-indicator"),
                    containerColor = Color.White,
                    color = Blue,
                )
            }
        },
        content = content,
    )
}
