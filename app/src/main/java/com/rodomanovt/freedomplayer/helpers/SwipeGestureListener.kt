package com.rodomanovt.freedomplayer.helpers

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.abs

open class SwipeGestureListener(context: Context) : View.OnTouchListener {

    private val gestureDetector = GestureDetector(context, GestureListener())
    private var touchedView: View? = null

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        touchedView = v
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return this@SwipeGestureListener.onSingleTapUp(e)
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            var result = false
            try {
                val view = touchedView ?: return false
                val diffY = e2.y - (e1?.y ?: 0f)
                val diffX = e2.x - (e1?.x ?: 0f)
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            animateSwipe(view, diffX = SWIPE_ANIMATION_DISTANCE) { onSwipeRight() }
                        } else {
                            animateSwipe(view, diffX = -SWIPE_ANIMATION_DISTANCE) { onSwipeLeft() }
                        }
                        result = true
                    }
                } else if (abs(diffY) > SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        animateSwipe(view, diffY = SWIPE_ANIMATION_DISTANCE) { onSwipeDown() }
                    } else {
                        animateSwipe(view, diffY = -SWIPE_ANIMATION_DISTANCE) { onSwipeUp() }
                    }
                    result = true
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return result
        }
    }

    private fun animateSwipe(view: View, diffX: Float = 0f, diffY: Float = 0f, action: () -> Unit) {
        view.animate().cancel()
        view.animate()
            .translationX(view.translationX + diffX)
            .translationY(view.translationY + diffY)
            .setDuration(SWIPE_ANIMATION_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                action()
                view.animate()
                    .translationX(0f)
                    .translationY(0f)
                    .setDuration(SWIPE_ANIMATION_RETURN_DURATION)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
    }

    /** Called for a regular tap when this listener consumes touch events. */
    open fun onSingleTapUp(e: MotionEvent): Boolean = false

    open fun onSwipeUp() {}
    open fun onSwipeDown() {}
    open fun onSwipeLeft() {}
    open fun onSwipeRight() {}

    companion object {
        private const val SWIPE_ANIMATION_DISTANCE = 148f
        private const val SWIPE_ANIMATION_DURATION = 140L
        private const val SWIPE_ANIMATION_RETURN_DURATION = 180L
    }
}
