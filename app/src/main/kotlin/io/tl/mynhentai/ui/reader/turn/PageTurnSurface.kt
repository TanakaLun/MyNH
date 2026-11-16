package io.tl.mynhentai.ui.reader.turn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import io.tl.mynhentai.ui.reader.turn.PageAnimation
import io.tl.mynhentai.ui.reader.turn.legado.components.ioscurl.IosCurlTurn
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.ReaderTurnDirection
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.ioscurl.gesture.CurlTouchKind
import io.tl.mynhentai.ui.reader.turn.PageTurnPolicy
import kotlinx.coroutines.launch
import kotlin.math.abs

/** A page is committed only after settling; canceled drags never change the anchor. */
@Composable
internal fun PageTurnSurface(
    index: Int,
    count: Int,
    hasPreviousPreview: Boolean = false,
    hasNextPreview: Boolean = false,
    mode: PageAnimation,
    durationMillis: Int,
    background: Color,
    width: Int,
    height: Int,
    onCommit: (Int) -> Unit,
    onBoundary: (Boolean) -> Unit,
    onToggle: () -> Unit,
    onActive: () -> Unit,
    content: @Composable (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val progress = remember { Animatable(0f) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var target by remember { mutableStateOf<Int?>(null) }
    var settling by remember { mutableStateOf(false) }
    var animationStarted by remember { mutableStateOf(false) }
    var renderer by remember { mutableStateOf<CurlBitmapRenderer?>(null) }
    var ready by remember { mutableStateOf(false) }
    val ios = remember { IosCurlTurn() }
    val speed = io.tl.mynhentai.ui.reader.turn.legado.core.transition.ReaderPageTurnSpeed.entries.first { it.baseDurationMillis == durationMillis }
    val first = rememberGraphicsLayer()
    val second = rememberGraphicsLayer()
    val curl = mode == PageAnimation.Simulation || mode == PageAnimation.GoogleCurl || mode == PageAnimation.IosCurl
    val next = (target ?: index) > index
    val direction = if (next) ReaderTurnDirection.NEXT else ReaderTurnDirection.PREVIOUS
    val currentCommit by rememberUpdatedState(onCommit)
    val currentBoundary by rememberUpdatedState(onBoundary)
    DisposableEffect(Unit) {
        onDispose {
            renderer?.close()
            ios.reset()
        }
    }

    fun begin(forward: Boolean, automatic: Boolean): Boolean {
        if (target != null || settling) return false
        val candidate = index + if (forward) 1 else -1
        if (candidate !in 0 until count && !(candidate == -1 && hasPreviousPreview) && !(candidate == count && hasNextPreview)) {
            if (automatic) currentBoundary(forward)
            return false
        }
        target = candidate
        ready = !curl
        dragProgress = 0f
        if (mode == PageAnimation.IosCurl) {
            ios.begin(
                if (forward) ReaderTurnDirection.NEXT else ReaderTurnDirection.PREVIOUS,
                width,
                height,
                density,
                scope,
                automatic,
                speed,
            )
        }
        return true
    }

    fun settle(commit: Boolean, iosVelocity: Double = 0.0) {
        if (settling || target == null) return
        settling = true
        animationStarted = false
        scope.launch {
            try {
                if (curl) repeat(30) { if (!ready) withFrameNanos { } }
                progress.snapTo(dragProgress)
                animationStarted = true
                if (mode == PageAnimation.IosCurl) {
                    ios.settle(commit, iosVelocity, speed)
                } else {
                    progress.animateTo(
                        if (commit) 1f else 0f,
                        tween(if (mode == PageAnimation.None) 0 else (durationMillis * abs((if (commit) 1f else 0f) - dragProgress)).toInt().coerceAtLeast(1)),
                    )
                }
                val destination = target
                if (commit && destination != null) {
                    if (destination in 0 until count) currentCommit(destination) else currentBoundary(destination >= count)
                }
            } finally {
                target = null
                settling = false
                dragProgress = 0f
                ios.reset()
                renderer?.close()
                renderer = null
            }
        }
    }

    LaunchedEffect(target) {
        if (target == null || !curl) return@LaunchedEffect
        try {
            withFrameNanos { }
            withFrameNanos { }
            fun ImageBitmap.readable() = asAndroidBitmap().let {
                if (it.config == android.graphics.Bitmap.Config.HARDWARE) requireNotNull(it.copy(android.graphics.Bitmap.Config.ARGB_8888, false)) else it
            }
            renderer = CurlBitmapRenderer(first.toImageBitmap().readable(), second.toImageBitmap().readable(), mode)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            android.util.Log.w("PageTurn", "Snapshot unavailable", e)
        } catch (e: OutOfMemoryError) {
            android.util.Log.w("PageTurn", "Snapshot allocation failed", e)
        } finally {
            ready = true
            ios.rendererReady()
        }
    }

    Box(Modifier.fillMaxSize()) {
        val destination = target
        val p = if (settling && animationStarted) progress.value else dragProgress
        if (destination == null) {
            content(index)
        } else if (curl) {
            Box(Modifier.fillMaxSize().drawWithContent { first.record { this@drawWithContent.drawContent() } }) {
                Box(Modifier.fillMaxSize().background(background)) { content(index) }
            }
            Box(Modifier.fillMaxSize().drawWithContent { second.record { this@drawWithContent.drawContent() } }) {
                Box(Modifier.fillMaxSize().background(background)) { content(destination) }
            }
            Canvas(Modifier.fillMaxSize()) {
                val active = renderer
                if (active == null) {
                    drawLayer(first)
                } else {
                    try {
                        active.draw(drawContext.canvas.nativeCanvas, direction, p, density, ios.config, ios.values, background.toArgb())
                    } catch (e: RuntimeException) {
                        drawLayer(if (p > .5f) second else first)
                    }
                }
            }
        } else {
            @Composable fun page(page: Int, x: Float, alpha: Float = 1f) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        translationX = x
                        this.alpha = alpha
                    }.background(background),
                ) { content(page) }
            }
            val w = width.toFloat()
            when (mode) {
                PageAnimation.Fade -> {
                    page(index, 0f)
                    page(destination, 0f, p)
                }

                PageAnimation.None -> page(index, 0f)

                PageAnimation.Cover, PageAnimation.Parallax -> {
                    val ratio = if (mode == PageAnimation.Parallax) .22f else 0f
                    if (next) {
                        page(destination, w * (1f - p) * ratio)
                        page(index, -w * p)
                    } else {
                        page(index, w * p * ratio)
                        page(destination, w * (p - 1f))
                    }
                    Canvas(Modifier.fillMaxSize()) {
                        val edge = if (next) w * (1f - p) else w * p
                        val shadowWidth = (if (mode == PageAnimation.Cover) 36 else 18) * density
                        if (p > 0f && p < 1f) {
                            drawRect(
                                Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .19f), Color.Transparent), edge, edge + shadowWidth),
                                topLeft = androidx.compose.ui.geometry.Offset(edge, 0f),
                                size = androidx.compose.ui.geometry.Size(shadowWidth, size.height),
                            )
                        }
                    }
                }

                else -> {
                    val sign = if (next) 1f else -1f
                    page(index, -sign * w * p)
                    page(destination, sign * w * (1f - p))
                }
            }
        }
        Box(
            Modifier.fillMaxSize().pointerInput(index, count, mode, width, height, durationMillis, hasPreviousPreview, hasNextPreview) {
                try {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        if (settling) return@awaitEachGesture
                        val velocity = VelocityTracker()
                        velocity.addPosition(down.uptimeMillis, down.position)
                        ios.down(width, height, density, down.position.x, down.position.y, down.uptimeMillis, index > 0 || hasPreviousPreview, index < count - 1 || hasNextPreview, viewConfiguration.touchSlop)
                        var dx = 0f
                        var dy = 0f
                        var captured = false
                        var forward = true
                        var up = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            dx = change.position.x - down.position.x
                            dy = change.position.y - down.position.y
                            velocity.addPosition(change.uptimeMillis, change.position)
                            ios.event(if (change.pressed) CurlTouchKind.Move else CurlTouchKind.Up, change.position.x, change.position.y, change.uptimeMillis)
                            if (!change.pressed) {
                                up = true
                                break
                            }
                            if (!captured && abs(dx) > viewConfiguration.touchSlop && abs(dx) > abs(dy)) {
                                forward = dx < 0
                                captured = true
                                begin(forward, false)
                            }
                            if (captured) {
                                change.consume()
                                dragProgress = ((if (forward) -dx else dx) / width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                ios.drag()
                            }
                        }
                        onActive()
                        if (captured) {
                            val v = velocity.calculateVelocity().x * if (forward) -1f else 1f
                            if (target != null) {
                                val iosRelease = if (mode == PageAnimation.IosCurl) {
                                    ios.release(
                                        if (forward) ReaderTurnDirection.NEXT else ReaderTurnDirection.PREVIOUS,
                                        !up,
                                    )
                                } else {
                                    null
                                }
                                val decision = iosRelease?.first ?: run {
                                    up && PageTurnPolicy.release(
                                        PageTurnPolicy.drag((if (forward) -1 else 1) * dragProgress * width, width.toFloat(), true, true),
                                        if (forward) -v else v,
                                    ).commit
                                }
                                settle(decision, iosRelease?.second ?: 0.0)
                            } else if (up && abs(dx) > 48 * density) {
                                currentBoundary(forward)
                            }
                        } else if (up && abs(dx) < viewConfiguration.touchSlop && abs(dy) < viewConfiguration.touchSlop) {
                            if (down.position.x in (width / 3f)..(width * 2f / 3f)) {
                                onToggle()
                            } else if (begin(down.position.x > width / 2f, true)) {
                                settle(true)
                            }
                        }
                    }
                } finally {
                    if (!settling) {
                        target = null
                        renderer?.close()
                        renderer = null
                        ios.reset()
                    }
                }
            },
        )
    }
}
