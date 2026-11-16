package io.tl.mynhentai.ui.reader.turn

import kotlin.math.abs

enum class PageTurnDirection { Previous, Next }

data class PageTurnState(
    val direction: PageTurnDirection? = null,
    val offsetPx: Float = 0f,
    val widthPx: Float = 0f,
    val dragging: Boolean = false,
) {
    val progress: Float get() = if (widthPx <= 0f) 0f else (abs(offsetPx) / widthPx).coerceIn(0f, 1f)
}

data class PageTurnDecision(val targetOffsetPx: Float, val commit: Boolean)

object PageTurnPolicy {
    fun drag(deltaPx: Float, widthPx: Float, canPrevious: Boolean, canNext: Boolean): PageTurnState {
        if (widthPx <= 0f || deltaPx == 0f) return PageTurnState(widthPx = widthPx)
        val direction = if (deltaPx > 0f) PageTurnDirection.Previous else PageTurnDirection.Next
        val available = if (direction == PageTurnDirection.Previous) canPrevious else canNext
        return PageTurnState(
            direction = direction,
            offsetPx = if (available) deltaPx.coerceIn(-widthPx, widthPx) else 0f,
            widthPx = widthPx,
            dragging = available,
        )
    }

    fun release(state: PageTurnState, velocityPxPerSecond: Float = 0f): PageTurnDecision {
        val direction = state.direction ?: return PageTurnDecision(0f, false)
        val fast = when (direction) {
            PageTurnDirection.Previous -> velocityPxPerSecond >= 900f
            PageTurnDirection.Next -> velocityPxPerSecond <= -900f
        }
        val commit = state.dragging && (state.progress >= 0.35f || fast)
        val target = if (!commit) {
            0f
        } else {
            when (direction) {
                PageTurnDirection.Previous -> state.widthPx
                PageTurnDirection.Next -> -state.widthPx
            }
        }
        return PageTurnDecision(target, commit)
    }
}
