package com.webitel.mobile_demo_app.ui.call

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import androidx.annotation.FloatRange
import kotlin.math.roundToInt


/**
 * @param scalesArg - array of percentage points from 0f to 1f. Associating with range from 0 to
 *                    bounds size of drawable . To indicate points with key frames and associated
 *                    scales on those frames
 * @param alphasArg - array of percentage alphas from 0f to 1f. Associating with range from 0 to
 *                    0xFF of alpha channel. To indicate points with key frames and associated
 *                    alpha channel on those frames
 */
class SonarDrawable(
    scalesArg: Array<Float>,
    alphasArg: Array<Float>,
    tint: Int
) : Drawable() {

    private val ripples: Array<Ripple>
    private val progressStep: Float

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = SONAR_DURATION
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
    }

    fun setColor(color: Int) {
        for (ripple in ripples) {
            ripple.setColor(color)
        }
    }

    fun setStrokeWidth(width: Float) {
        for (ripple in ripples) {
            ripple.setStrokeWidth(width)
        }
    }


    init {
        val scales = initScales(scalesArg)
        val alphas = initAlphas(alphasArg)

        ripples = Array(CIRCLE_COUNT) {
            Ripple(scales, alphas, tint) { bounds }
        }
        progressStep = 1f / ripples.size

        animator.addUpdateListener {
            var progressOffset = it.animatedFraction
            repeat(ripples.size) {
                progressOffset = (progressOffset + progressStep).let { result ->
                    if (result >= 1f) result - 1f else result
                }

                ripples[it].progress = progressOffset
            }

            invalidateSelf()
        }
    }

    override fun draw(canvas: Canvas) {
        ripples.forEach { it.draw(canvas) }
    }

    override fun setAlpha(alpha: Int) {/*ignore*/
    }

    override fun getOpacity(): Int = PixelFormat.OPAQUE

    override fun setColorFilter(colorFilter: ColorFilter?) {/*ignore*/
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        return super.setVisible(visible, restart).also { changed ->
            if (changed) {
                with(animator) { if (visible) start() else cancel() }
            }
        }
    }

    private fun initScales(arg: Array<Float>): FloatArray {
        //include up (1f) and down (0f) bounds
        return FloatArray(arg.size + 2) {
            when (it) {
                0 -> 0f
                arg.size + 1 -> 1f
                else -> arg[it - 1]
            }
        }
    }

    private fun initAlphas(arg: Array<Float>): IntArray {
        //include up (0xFF) and down (0x00) bounds
        return IntArray(arg.size + 2) {
            when (it) {
                0 -> OPAQUE_ALPHA
                arg.size + 1 -> 0
                else -> (OPAQUE_ALPHA * arg[it - 1]).roundToInt()
            }
        }
    }

    class Ripple(
        private val scales: FloatArray,
        private val alphas: IntArray,
        private var tint: Int,
        private val dBounds: () -> Rect
    ) {
        private val rect: RectF = RectF()
        private var width: Float = 3F
        private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tint
        }

        fun setColor(color: Int) {
            tint = color
            paint.color = color
        }

        fun setStrokeWidth(width: Float) {
            this.width = width
        }

        @FloatRange(from = .0, to = 1.0, toInclusive = false)
        var progress: Float = 0f
            set(value) {
                val scale = computeIntermediateScale(value)
                val xRadius = scale * dBounds().width() / 2f
                val yRadius = scale * dBounds().height() / 2f

                rect.left = dBounds().exactCenterX() - xRadius
                rect.top = dBounds().exactCenterY() - yRadius
                rect.right = dBounds().exactCenterX() + xRadius
                rect.bottom = dBounds().exactCenterY() + yRadius

                paint.color = computeIntermediateColor(value)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = width
            }

        fun draw(canvas: Canvas) = canvas.drawCircle(
            rect.centerX(),
            rect.centerY(),
            rect.height() / 2,
            paint
        )//.drawCircle(rect, paint)

        private fun computeIntermediateScale(fraction: Float): Float {
            val step = 1f / (scales.size - 1)
            val scaleIndex = (fraction * (scales.size - 1)).toInt()
            val relativeFraction = (fraction - step * scaleIndex) / step
            return relativeFraction * (scales[scaleIndex + 1] - scales[scaleIndex]) + scales[scaleIndex]
        }

        private fun computeIntermediateColor(fraction: Float): Int {
            val step = 1f / (alphas.size - 1)
            val alphasIndex = (fraction * (alphas.size - 1)).toInt()
            val relativeFraction = (fraction - step * alphasIndex) / step
            val alpha =
                (relativeFraction * (alphas[alphasIndex + 1] - alphas[alphasIndex]) + alphas[alphasIndex]).roundToInt()
            return tint.applyAlpha(alpha)
        }

        private fun Int.applyAlpha(alpha: Int) =
            Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
    }

    private companion object {
        const val SONAR_DURATION = 3000L
        const val OPAQUE_ALPHA = 0xFF
        const val CIRCLE_COUNT = 4
    }
}