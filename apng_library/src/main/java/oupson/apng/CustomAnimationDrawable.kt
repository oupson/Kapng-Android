package oupson.apng

import android.graphics.drawable.AnimationDrawable

/**
 * Extension of the [AnimationDrawable] that provides an [AnimationListener]. This will allow
 * for the caller to listen for specific animation related events.
 */
internal class CustomAnimationDrawable : AnimationDrawable() {
    private var onAnimationLoop : () -> Unit = {}

    fun setOnAnimationLoopListener( f : () -> Unit) {
        onAnimationLoop = f
    }

    override fun selectDrawable(index: Int): Boolean {
        val drawableChanged = super.selectDrawable(index)

        if (index != 0 && index == numberOfFrames - 1) {
            onAnimationLoop()
        }

        return drawableChanged
    }
}