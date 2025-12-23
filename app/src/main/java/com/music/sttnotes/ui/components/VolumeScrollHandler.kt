package com.music.sttnotes.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.ScrollState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Interface for handling volume button scroll events
 * E-ink optimized with instant scrolling (no animation)
 */
interface VolumeScrollHandler {
    fun scrollUp()
    fun scrollDown()
}

/**
 * Creates a VolumeScrollHandler for LazyListState
 * Uses instant scrolling (scrollToItem) for e-ink optimization
 *
 * @param state The LazyListState to control
 * @param scope CoroutineScope for launching scroll operations
 * @param scrollDistanceProvider Provider function that returns overlap percentage (0.3-1.0)
 *        The overlap represents how much content stays visible after scrolling.
 *        - 100% (1.0) = 0% overlap, scroll full screen
 *        - 50% (0.5) = 50% overlap, scroll half screen (half stays visible)
 *        - 30% (0.3) = 70% overlap, scroll 30% (70% stays visible)
 *        Default: 0.8 (20% overlap)
 */
fun createLazyListVolumeHandler(
    state: LazyListState,
    scope: CoroutineScope,
    scrollDistanceProvider: () -> Float = { 0.8f }
): VolumeScrollHandler = object : VolumeScrollHandler {
    override fun scrollUp() {
        scope.launch {
            val visibleItems = state.layoutInfo.visibleItemsInfo.size
            // scrollDistanceProvider returns the distance percentage (1 - overlap)
            // E.g., if user sets 50%, they want 50% overlap, so we scroll 50% of screen
            val itemJumpCount = (visibleItems * scrollDistanceProvider()).toInt().coerceAtLeast(1)
            val targetIndex = (state.firstVisibleItemIndex - itemJumpCount).coerceAtLeast(0)
            state.scrollToItem(targetIndex)
        }
    }

    override fun scrollDown() {
        scope.launch {
            val visibleItems = state.layoutInfo.visibleItemsInfo.size
            // scrollDistanceProvider returns the distance percentage (1 - overlap)
            val itemJumpCount = (visibleItems * scrollDistanceProvider()).toInt().coerceAtLeast(1)
            val maxIndex = (state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            val targetIndex = (state.firstVisibleItemIndex + itemJumpCount).coerceAtMost(maxIndex)
            state.scrollToItem(targetIndex)
        }
    }
}

/**
 * Creates a VolumeScrollHandler for LazyGridState
 * Uses instant scrolling (scrollToItem) for e-ink optimization
 *
 * @param state The LazyGridState to control
 * @param scope CoroutineScope for launching scroll operations
 * @param scrollDistanceProvider Provider function that returns overlap percentage (0.3-1.0)
 *        The overlap represents how much content stays visible after scrolling.
 *        - 100% (1.0) = 0% overlap, scroll full screen
 *        - 50% (0.5) = 50% overlap, scroll half screen (half stays visible)
 *        - 30% (0.3) = 70% overlap, scroll 30% (70% stays visible)
 *        Default: 0.8 (20% overlap)
 */
fun createLazyGridVolumeHandler(
    state: LazyGridState,
    scope: CoroutineScope,
    scrollDistanceProvider: () -> Float = { 0.8f }
): VolumeScrollHandler = object : VolumeScrollHandler {
    override fun scrollUp() {
        scope.launch {
            val visibleItems = state.layoutInfo.visibleItemsInfo.size
            // scrollDistanceProvider returns the distance percentage (1 - overlap)
            val itemJumpCount = (visibleItems * scrollDistanceProvider()).toInt().coerceAtLeast(1)
            val targetIndex = (state.firstVisibleItemIndex - itemJumpCount).coerceAtLeast(0)
            state.scrollToItem(targetIndex)
        }
    }

    override fun scrollDown() {
        scope.launch {
            val visibleItems = state.layoutInfo.visibleItemsInfo.size
            // scrollDistanceProvider returns the distance percentage (1 - overlap)
            val itemJumpCount = (visibleItems * scrollDistanceProvider()).toInt().coerceAtLeast(1)
            val maxIndex = (state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            val targetIndex = (state.firstVisibleItemIndex + itemJumpCount).coerceAtMost(maxIndex)
            state.scrollToItem(targetIndex)
        }
    }
}

/**
 * Creates a VolumeScrollHandler for ScrollState
 * Uses instant scrolling (scrollTo) for e-ink optimization
 *
 * @param state The ScrollState to control
 * @param scope CoroutineScope for launching scroll operations
 * @param viewportHeightProvider Provider function that returns current viewport height in pixels
 * @param scrollDistanceProvider Provider function that returns overlap percentage (0.3-1.0)
 *        The overlap represents how much content stays visible after scrolling.
 *        - 100% (1.0) = 0% overlap, scroll full screen
 *        - 50% (0.5) = 50% overlap, scroll half screen (half stays visible)
 *        - 30% (0.3) = 70% overlap, scroll 30% (70% stays visible)
 *        Default: 0.8 (20% overlap)
 */
fun createScrollStateVolumeHandler(
    state: ScrollState,
    scope: CoroutineScope,
    viewportHeightProvider: () -> Int,
    scrollDistanceProvider: () -> Float = { 0.8f }
): VolumeScrollHandler = object : VolumeScrollHandler {
    override fun scrollUp() {
        scope.launch {
            // scrollDistanceProvider returns the distance percentage (1 - overlap)
            val scrollAmount = (viewportHeightProvider() * scrollDistanceProvider()).toInt()
            val targetPosition = (state.value - scrollAmount).coerceAtLeast(0)
            state.scrollTo(targetPosition)
        }
    }

    override fun scrollDown() {
        scope.launch {
            // scrollDistanceProvider returns the distance percentage (1 - overlap)
            val scrollAmount = (viewportHeightProvider() * scrollDistanceProvider()).toInt()
            val targetPosition = (state.value + scrollAmount).coerceAtMost(state.maxValue)
            state.scrollTo(targetPosition)
        }
    }
}

/**
 * Creates a VolumeScrollHandler for LazyListState that scrolls exactly 1 item at a time
 * Perfect for chat conversations where you want to navigate message by message
 * Uses instant scrolling (scrollToItem) for e-ink optimization
 *
 * @param state The LazyListState to control
 * @param scope CoroutineScope for launching scroll operations
 */
fun createSingleItemVolumeHandler(
    state: LazyListState,
    scope: CoroutineScope
): VolumeScrollHandler = object : VolumeScrollHandler {
    override fun scrollUp() {
        scope.launch {
            // Scroll up by exactly 1 item (1 message)
            val targetIndex = (state.firstVisibleItemIndex - 1).coerceAtLeast(0)
            state.scrollToItem(targetIndex)
        }
    }

    override fun scrollDown() {
        scope.launch {
            // Scroll down by exactly 1 item (1 message)
            val maxIndex = (state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            val targetIndex = (state.firstVisibleItemIndex + 1).coerceAtMost(maxIndex)
            state.scrollToItem(targetIndex)
        }
    }
}
