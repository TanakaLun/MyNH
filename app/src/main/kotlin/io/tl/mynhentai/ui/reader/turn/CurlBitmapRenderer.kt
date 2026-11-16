package io.tl.mynhentai.ui.reader.turn

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import io.tl.mynhentai.ui.reader.turn.PageAnimation
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.CurlPoint
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.PageCurlGeometry
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.ReaderTurnDirection
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.ioscurl.layout.CurlLayout
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.ioscurl.mesh.CurlColor
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.ioscurl.mesh.CurlMaterial
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.ioscurl.mesh.CurlMesh
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.ioscurl.mesh.CurlMeshInput
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.ioscurl.model.CurlConfig
import io.tl.mynhentai.ui.reader.turn.legado.core.transition.ioscurl.model.CurlValues
import io.tl.mynhentai.ui.reader.turn.legado.platform.googlecurl.GoogleCurlRenderer
import io.tl.mynhentai.ui.reader.turn.legado.platform.ioscurl.IosCurlCanvasRenderer
import io.tl.mynhentai.ui.reader.turn.legado.platform.ioscurl.IosCurlRenderer

/** Owns raster resources for one immutable source/target transaction. */
internal class CurlBitmapRenderer(
    private val current: Bitmap,
    private val target: Bitmap,
    private val mode: PageAnimation,
) : AutoCloseable {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF(0f, 0f, current.width.toFloat(), current.height.toFloat())
    private val google = if (mode == PageAnimation.GoogleCurl) GoogleCurlRenderer().also { it.prepare(current, target) } else null
    private var native: IosCurlCanvasRenderer? = null
    private var gl: IosCurlRenderer? = null
    private var output: Bitmap? = null
    private val mesh = CurlMesh()
    private val front = Path()
    private val back = Path()
    private val mirror = Matrix()

    fun draw(
        canvas: Canvas,
        direction: ReaderTurnDirection,
        progress: Float,
        density: Float,
        config: CurlConfig?,
        values: CurlValues,
        background: Int,
    ) {
        if (mode == PageAnimation.GoogleCurl) {
            google?.draw(canvas, direction, progress, rect.width(), rect.height(), density)
        } else if (mode == PageAnimation.IosCurl && config != null) {
            val layout = CurlLayout.build(config)
            val texture = if (layout.passes.single().source.nativeValue < 2) 0 else 1
            val material = CurlMaterial().copy(
                back = CurlColor(
                    android.graphics.Color.red(background) / 255.0,
                    android.graphics.Color.green(background) / 255.0,
                    android.graphics.Color.blue(background) / 255.0,
                    .8,
                ),
                shadow = CurlColor(0.0, 0.0, 0.0, .55),
            )
            val frame = mesh.build(
                CurlMeshInput(
                    values,
                    config.width,
                    config.height,
                    (rect.width() / config.width).coerceIn(.1, 4.0),
                    material = material,
                ),
                coalesceTriangleStrips = true,
            )
            val underlays = layout.underlays.map { (if (it.source.nativeValue < 2) 0 else 1) to it.frame }
            if (Build.VERSION.SDK_INT >= 34 && canvas.isHardwareAccelerated) {
                val renderer = native ?: IosCurlCanvasRenderer(current, target).also { native = it }
                if (renderer.canDraw(config.width, config.height, underlays, true)) {
                    try {
                        renderer.draw(canvas, frame, config.width, config.height, texture, 1 - texture, underlays, true)
                        return
                    } catch (e: RuntimeException) {
                        android.util.Log.w("PageTurn", "Canvas mesh unavailable; using GLES", e)
                        native = null
                    }
                }
            }
            val renderer = gl ?: IosCurlRenderer().also {
                gl = it
                it.upload(current, target)
                output = Bitmap.createBitmap(current.width, current.height, Bitmap.Config.ARGB_8888)
                it.bindOutput(requireNotNull(output))
            }
            renderer.render(frame, config.width.toFloat(), config.height.toFloat(), texture, 1 - texture, underlays)
            val saved = canvas.save()
            canvas.translate(0f, rect.height())
            canvas.scale(1f, -1f)
            canvas.drawBitmap(requireNotNull(output), null, rect, paint)
            canvas.restoreToCount(saved)
        } else {
            simulation(canvas, direction, progress, background)
        }
    }

    private fun simulation(canvas: Canvas, direction: ReaderTurnDirection, progress: Float, background: Int) {
        val forward = direction == ReaderTurnDirection.NEXT
        val p = if (forward) progress else 1f - progress
        val sheet = if (forward) current else target
        canvas.drawBitmap(if (forward) target else current, null, rect, paint)
        if (p >= .999f) return
        if (p <= .001f) {
            canvas.drawBitmap(sheet, null, rect, paint)
            return
        }
        val f = PageCurlGeometry.calculate(
            rect.width(),
            rect.height(),
            rect.width() * (1f - 2f * p),
            rect.height() * (1f - .12f * (1f - p)),
            CurlPoint(rect.width(), rect.height()),
        ) ?: return
        front.rewind()
        front.moveTo(f.start1.x, f.start1.y)
        front.quadTo(f.control1.x, f.control1.y, f.end1.x, f.end1.y)
        front.lineTo(f.touch.x, f.touch.y)
        front.lineTo(f.end2.x, f.end2.y)
        front.quadTo(f.control2.x, f.control2.y, f.start2.x, f.start2.y)
        front.lineTo(f.corner.x, f.corner.y)
        front.close()
        var saved = canvas.save()
        canvas.clipOutPath(front)
        canvas.drawBitmap(sheet, null, rect, paint)
        canvas.restoreToCount(saved)
        back.rewind()
        back.moveTo(f.vertex2.x, f.vertex2.y)
        back.lineTo(f.vertex1.x, f.vertex1.y)
        back.lineTo(f.end1.x, f.end1.y)
        back.lineTo(f.touch.x, f.touch.y)
        back.lineTo(f.end2.x, f.end2.y)
        back.close()
        saved = canvas.save()
        canvas.clipPath(back)
        canvas.drawColor(background)
        val m = f.mirror
        mirror.setValues(floatArrayOf(m.scaleX, m.skewX, m.translateX, m.skewY, m.scaleY, m.translateY, 0f, 0f, 1f))
        paint.alpha = 100
        canvas.drawBitmap(sheet, mirror, paint)
        paint.alpha = 255
        canvas.drawColor(0x22000000)
        canvas.restoreToCount(saved)
        paint.color = 0x44000000
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawPath(back, paint)
        paint.style = Paint.Style.FILL
        val shadowWidth = (f.touchToCornerDistance / 4f).coerceAtLeast(1f)
        saved = canvas.save()
        canvas.rotate(f.degrees, f.start1.x, f.start1.y)
        paint.shader = android.graphics.LinearGradient(
            f.start1.x - shadowWidth,
            0f,
            f.start1.x,
            0f,
            android.graphics.Color.TRANSPARENT,
            0x66111111,
            android.graphics.Shader.TileMode.CLAMP,
        )
        canvas.drawRect(
            f.start1.x - shadowWidth,
            f.start1.y,
            f.start1.x,
            f.start1.y + kotlin.math.hypot(rect.width(), rect.height()),
            paint,
        )
        paint.shader = null
        canvas.restoreToCount(saved)
    }

    override fun close() {
        google?.close()
        gl?.close()
        gl = null
        native = null
        output = null
    }
}
