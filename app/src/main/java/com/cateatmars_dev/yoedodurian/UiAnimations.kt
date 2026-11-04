package com.cateatmars_dev.yoedodurian

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar

object UiAnimations {

    /**
     * Adds a responsive, tactile scale-down & springy release animation on touch
     * without interfering with normal click / accessibility events.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun applyPressEffect(vararg views: View?) {
        for (target in views) {
            target?.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate()
                            .scaleX(0.965f)
                            .scaleY(0.965f)
                            .setDuration(90L)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(200L)
                            .setInterpolator(OvershootInterpolator(1.6f))
                            .start()
                    }
                }
                false
            }
        }
    }

    /**
     * Plays a smooth staggered slide-up + fade-in entrance for direct visible children of a container.
     */
    fun playStaggeredEntrance(container: ViewGroup?, startDelayMs: Long = 40L, stepMs: Long = 55L) {
        if (container == null) return
        val density = container.resources.displayMetrics.density
        val offsetPx = 22f * density
        var visibleIndex = 0

        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child.visibility != View.VISIBLE) continue

            child.alpha = 0f
            child.translationY = offsetPx
            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(startDelayMs + visibleIndex * stepMs)
                .setDuration(360L)
                .setInterpolator(DecelerateInterpolator(1.5f))
                .start()
            visibleIndex++
        }
    }

    /**
     * Gentle breathing glow animation for live AI status dots and hero scan rings.
     */
    fun startBreathingPulse(view: View?, minAlpha: Float = 0.45f, maxScale: Float = 1.15f): ObjectAnimator? {
        if (view == null) return null
        val pvhAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, minAlpha)
        val pvhScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, maxScale)
        val pvhScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, maxScale)
        return ObjectAnimator.ofPropertyValuesHolder(view, pvhAlpha, pvhScaleX, pvhScaleY).apply {
            duration = 1150L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
    }

    /**
     * Smoothly animates a horizontal ProgressBar from 0 to targetProgress.
     */
    fun animateProgress(progressBar: ProgressBar?, targetProgress: Int, durationMs: Long = 750L) {
        if (progressBar == null) return
        progressBar.progress = 0
        ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress.coerceIn(0, progressBar.max)).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator(1.6f)
            start()
        }
    }

    /**
     * Quick pop feedback when a live counter increments.
     */
    fun playCounterPop(view: View?) {
        if (view == null) return
        view.animate()
            .scaleX(1.12f)
            .scaleY(1.12f)
            .setDuration(90L)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(140L)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }
            .start()
    }
}

