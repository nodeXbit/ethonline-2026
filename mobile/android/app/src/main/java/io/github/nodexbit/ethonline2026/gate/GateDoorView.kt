package io.github.nodexbit.ethonline2026.gate

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max

/** Lightweight gate visual. Authorization never enters this class. */
class GateDoorView(context: Context, private val profile: String) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val frame = RectF()
    private var progress = 0f
    private var denied = false
    private var animator: ValueAnimator? = null

    fun reset() {
        animator?.cancel()
        progress = 0f
        denied = false
        invalidate()
    }

    fun showAuthoritativeDecision(allowed: Boolean) {
        animator?.cancel()
        denied = !allowed
        if (!allowed) {
            progress = 0f
            invalidate()
            return
        }
        ValueAnimator.ofFloat(progress, 1f).also { animation ->
            animator = animation
            animation.duration = 650
            animation.interpolator = DecelerateInterpolator()
            animation.addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            animation.start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val frameWidth = width * 0.58f
        val frameHeight = height * 0.92f
        frame.set((width - frameWidth) / 2f, height - frameHeight, (width + frameWidth) / 2f, height.toFloat())
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(18, 21, 28)
        canvas.drawRoundRect(frame, 18f, 18f, paint)

        val opening = RectF(frame.left + 18f, frame.top + 18f, frame.right - 18f, frame.bottom)
        drawInterior(canvas, opening)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        paint.color = if (denied) Color.rgb(239, 83, 80) else Color.argb(220, 220, 226, 235)
        canvas.drawRoundRect(frame, 18f, 18f, paint)

        val visibleWidth = max(16f, opening.width() * (1f - progress))
        val door = Path().apply {
            moveTo(opening.left, opening.top)
            lineTo(opening.left + visibleWidth, opening.top + progress * 22f)
            lineTo(opening.left + visibleWidth, opening.bottom - progress * 22f)
            lineTo(opening.left, opening.bottom)
            close()
        }
        paint.style = Paint.Style.FILL
        paint.color = when (profile) {
            "front-door" -> Color.rgb(73, 48, 35)
            "server-room" -> Color.rgb(25, 35, 49)
            else -> Color.rgb(39, 59, 65)
        }
        canvas.drawPath(door, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.argb(150, 235, 240, 245)
        canvas.drawPath(door, paint)
        if (visibleWidth > 70f) {
            paint.style = Paint.Style.FILL
            paint.color = if (denied) Color.rgb(239, 83, 80) else Color.rgb(224, 194, 112)
            canvas.drawCircle(opening.left + visibleWidth - 34f, opening.centerY(), 8f, paint)
        }
    }

    private fun drawInterior(canvas: Canvas, opening: RectF) {
        paint.style = Paint.Style.FILL
        paint.color = when (profile) {
            "front-door" -> Color.rgb(239, 194, 124)
            "server-room" -> Color.rgb(43, 101, 145)
            else -> Color.rgb(102, 202, 190)
        }
        canvas.drawRect(opening, paint)
        when (profile) {
            "front-door" -> {
                paint.color = Color.argb(100, 255, 250, 225)
                canvas.drawCircle(opening.centerX(), opening.top + opening.height() * 0.28f, opening.width() * 0.24f, paint)
                paint.color = Color.rgb(139, 105, 72)
                canvas.drawRect(opening.left, opening.bottom - opening.height() * 0.28f, opening.right, opening.bottom, paint)
            }
            "server-room" -> {
                paint.color = Color.rgb(18, 29, 43)
                val rackWidth = opening.width() * 0.25f
                canvas.drawRect(opening.left + 18f, opening.top + 28f, opening.left + 18f + rackWidth, opening.bottom, paint)
                canvas.drawRect(opening.right - 18f - rackWidth, opening.top + 28f, opening.right - 18f, opening.bottom, paint)
                paint.color = Color.rgb(60, 224, 185)
                repeat(7) { row ->
                    val y = opening.top + 55f + row * opening.height() / 9f
                    canvas.drawCircle(opening.left + 34f, y, 4f, paint)
                    canvas.drawCircle(opening.right - 34f, y, 4f, paint)
                }
            }
            else -> {
                paint.color = Color.argb(115, 235, 255, 252)
                repeat(4) { index ->
                    val y = opening.top + (index + 1) * opening.height() / 5f
                    canvas.drawRect(opening.left + 22f, y, opening.right - 22f, y + 3f, paint)
                }
                paint.color = Color.rgb(35, 91, 94)
                canvas.drawRect(opening.left + 30f, opening.bottom - opening.height() * 0.26f, opening.right - 30f, opening.bottom, paint)
            }
        }
    }
}
