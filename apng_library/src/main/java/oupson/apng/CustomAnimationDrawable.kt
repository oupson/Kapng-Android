package oupson.apng

import android.graphics.drawable.AnimationDrawable
import oupson.apng.CustomAnimationDrawable.AnimationListener

/**
 * Extension of the [AnimationDrawable] that provides an [AnimationListener]. This will allow
 * for the caller to listen for specific animation related events.
 */
internal class CustomAnimationDrawable : AnimationDrawable() {

    /**
     * Interface that exposes callbacks for events during the animation.
     */
    interface AnimationListener {

        /**
         * The animation has performed a loop.
         */
        fun onAnimationLooped()
    }

    private var animationListener: AnimationListener? = null

    fun setAnimationListener(animationListener: AnimationListener) {
        this.animationListener = animationListener
    }

    override fun selectDrawable(index: Int): Boolean {
        val drawableChanged = super.selectDrawable(index)

        if (index != 0 && index == numberOfFrames - 1) {
            animationListener?.onAnimationLooped()
        }

        return drawableChanged
    }
}