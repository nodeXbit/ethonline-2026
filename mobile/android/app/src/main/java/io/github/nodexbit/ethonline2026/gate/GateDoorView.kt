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
    private var verifyingPulse = 0f
    private var animator: ValueAnimator? = null

    init {
        contentDescription = "Virtual gate closed — ready"
    }

    fun reset() {
        animator?.cancel()
        progress = 0f
        denied = false
        verifyingPulse = 0f
        contentDescription = "Virtual gate closed — ready"
        invalidate()
    }

    fun showVerifying() {
        animator?.cancel()
        progress = 0f
        denied = false
        contentDescription = "Virtual gate closed — verifying"
        ValueAnimator.ofFloat(0f, 1f).also { animation ->
            animator = animation
            animation.duration = 720
            animation.repeatCount = ValueAnimator.INFINITE
            animation.repeatMode = ValueAnimator.REVERSE
            animation.addUpdateListener {
                verifyingPulse = it.animatedValue as Float
                invalidate()
            }
            animation.start()
        }
    }

    fun showAuthoritativeDecision(allowed: Boolean) {
        animator?.cancel()
        verifyingPulse = 0f
        denied = !allowed
        if (!allowed) {
            progress = 0f
            contentDescription = "Virtual gate closed — denied"
            invalidate()
            return
        }
        contentDescription = "Virtual gate opening — authoritative allow"
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
        val frameWidth = width * 0.62f
        val frameHeight = height * 0.92f
        frame.set((width - frameWidth) / 2f, height - frameHeight, (width + frameWidth) / 2f, height.toFloat())
        drawApproach(canvas)
        paint.style = Paint.Style.FILL
        paint.color = frameColor()
        canvas.drawRoundRect(frame, 18f, 18f, paint)

        val opening = RectF(frame.left + 18f, frame.top + 18f, frame.right - 18f, frame.bottom)
        drawInterior(canvas, opening)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f + verifyingPulse * 3f
        paint.color = when {
            denied -> Color.rgb(239, 83, 80)
            verifyingPulse > 0f -> Color.rgb(92, (190 + verifyingPulse * 55).toInt(), 235)
            else -> frameAccent()
        }
        canvas.drawRoundRect(frame, 18f, 18f, paint)

        val visibleWidth = max(16f, opening.width() * (1f - progress))
        val door = Path().apply {
            moveTo(opening.left, opening.top)
            lineTo(opening.left + visibleWidth, opening.top + progress * 22f)
            lineTo(opening.left + visibleWidth, opening.bottom - progress * 22f)
            lineTo(opening.left, opening.bottom)
            close()
        }
        drawDoorSurface(canvas, door, opening, visibleWidth)
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

    private fun drawApproach(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        when (profile) {
            "front-door" -> {
                paint.color = Color.argb(58, 255, 220, 160)
                canvas.drawCircle(width * 0.18f, height * 0.20f, width * 0.11f, paint)
                canvas.drawCircle(width * 0.82f, height * 0.20f, width * 0.11f, paint)
                paint.color = Color.argb(75, 226, 238, 240)
                canvas.drawRect(width * 0.08f, height * 0.28f, width * 0.16f, height.toFloat(), paint)
                canvas.drawRect(width * 0.84f, height * 0.28f, width * 0.92f, height.toFloat(), paint)
            }
            "server-room" -> {
                paint.color = Color.argb(105, 8, 14, 24)
                canvas.drawRect(0f, height * 0.18f, width.toFloat(), height.toFloat(), paint)
                paint.color = Color.argb(120, 58, 130, 170)
                repeat(5) { index ->
                    val y = height * (0.24f + index * 0.14f)
                    canvas.drawRect(width * 0.05f, y, width * 0.16f, y + 3f, paint)
                    canvas.drawRect(width * 0.84f, y, width * 0.95f, y + 3f, paint)
                }
            }
            else -> {
                paint.color = Color.argb(72, 224, 250, 247)
                canvas.drawRect(width * 0.04f, height * 0.20f, width * 0.18f, height.toFloat(), paint)
                canvas.drawRect(width * 0.82f, height * 0.20f, width * 0.96f, height.toFloat(), paint)
                paint.color = Color.argb(75, 73, 210, 197)
                canvas.drawRect(0f, height * 0.72f, width.toFloat(), height * 0.74f, paint)
            }
        }
    }

    private fun drawDoorSurface(
        canvas: Canvas,
        door: Path,
        opening: RectF,
        visibleWidth: Float,
    ) {
        val right = opening.left + visibleWidth
        canvas.save()
        canvas.clipPath(door)
        paint.style = Paint.Style.FILL
        when (profile) {
            "front-door" -> {
                paint.color = Color.rgb(82, 55, 39)
                canvas.drawRect(opening.left, opening.top, right, opening.bottom, paint)
                paint.color = Color.rgb(34, 56, 62)
                val glassBottom = opening.top + opening.height() * 0.52f
                canvas.drawRect(opening.left + 22f, opening.top + 26f, right - 22f, glassBottom, paint)
                paint.color = Color.argb(105, 204, 238, 241)
                canvas.drawRect(opening.left + 31f, opening.top + 34f, right - 31f, glassBottom - 9f, paint)
                paint.color = Color.rgb(205, 158, 91)
                val upperRail = opening.top + opening.height() * 0.61f
                val lowerRail = opening.top + opening.height() * 0.80f
                canvas.drawRect(opening.left + 18f, upperRail, right - 18f, upperRail + 8f, paint)
                canvas.drawRect(opening.left + 18f, lowerRail, right - 18f, lowerRail + 8f, paint)
            }
            "server-room" -> {
                paint.color = Color.rgb(24, 34, 47)
                canvas.drawRect(opening.left, opening.top, right, opening.bottom, paint)
                paint.color = Color.rgb(42, 56, 72)
                canvas.drawRoundRect(
                    RectF(opening.left + 22f, opening.top + 26f, right - 22f, opening.bottom - 26f),
                    10f,
                    10f,
                    paint,
                )
                paint.color = Color.rgb(218, 165, 48)
                repeat(4) { index ->
                    val y = opening.bottom - 88f + index * 14f
                    canvas.drawRect(opening.left + 26f, y, right - 26f, y + 7f, paint)
                }
                paint.color = Color.rgb(92, 223, 183)
                repeat(5) { index ->
                    canvas.drawCircle(opening.left + 34f, opening.top + 48f + index * 55f, 4f, paint)
                }
            }
            else -> {
                paint.color = Color.rgb(207, 224, 226)
                canvas.drawRect(opening.left, opening.top, right, opening.bottom, paint)
                paint.color = Color.rgb(67, 96, 105)
                canvas.drawRect(opening.left + 28f, opening.top + 40f, right - 28f, opening.top + opening.height() * 0.44f, paint)
                paint.color = Color.argb(150, 145, 225, 220)
                canvas.drawRect(opening.left + 36f, opening.top + 48f, right - 36f, opening.top + opening.height() * 0.44f - 8f, paint)
                paint.color = Color.rgb(84, 173, 166)
                canvas.drawRect(opening.left, opening.top + opening.height() * 0.68f, right, opening.top + opening.height() * 0.70f, paint)
                paint.color = Color.rgb(238, 246, 247)
                canvas.drawRect(opening.left + 28f, opening.bottom - 86f, right - 28f, opening.bottom - 42f, paint)
            }
        }
        canvas.restore()
    }

    private fun frameColor(): Int = when (profile) {
        "front-door" -> Color.rgb(45, 35, 29)
        "server-room" -> Color.rgb(10, 16, 24)
        else -> Color.rgb(224, 236, 237)
    }

    private fun frameAccent(): Int = when (profile) {
        "front-door" -> Color.rgb(224, 183, 116)
        "server-room" -> Color.rgb(82, 151, 188)
        else -> Color.rgb(170, 224, 220)
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
                paint.color = Color.argb(120, 255, 250, 225)
                canvas.drawCircle(opening.centerX(), opening.top + opening.height() * 0.23f, opening.width() * 0.25f, paint)
                paint.color = Color.rgb(139, 105, 72)
                canvas.drawRect(opening.left, opening.bottom - opening.height() * 0.34f, opening.right, opening.bottom, paint)
                paint.color = Color.argb(115, 255, 235, 190)
                repeat(4) { index ->
                    val inset = index * opening.width() * 0.10f
                    canvas.drawLine(opening.left + inset, opening.bottom, opening.centerX(), opening.top + opening.height() * 0.57f, paint)
                    canvas.drawLine(opening.right - inset, opening.bottom, opening.centerX(), opening.top + opening.height() * 0.57f, paint)
                }
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
                paint.color = Color.rgb(221, 242, 239)
                canvas.drawRect(opening.left + 44f, opening.bottom - opening.height() * 0.34f, opening.centerX() - 10f, opening.bottom - opening.height() * 0.27f, paint)
                canvas.drawRect(opening.centerX() + 10f, opening.bottom - opening.height() * 0.34f, opening.right - 44f, opening.bottom - opening.height() * 0.27f, paint)
            }
        }
    }
}
