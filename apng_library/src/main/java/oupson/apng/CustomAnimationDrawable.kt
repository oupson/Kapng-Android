package oupson.apng

import android.graphics.drawable.AnimationDrawable

/**
 * Extension of the [AnimationDrawable] that provides an animationListener This will allow
 * for the caller to listen for specific animation related events.
 */
@Deprecated("Deprecated, Use ApngEncoder and ApngDecoder instead", level = DeprecationLevel.WARNING)
class CustomAnimationDrawable : AnimationDrawable() {
    private var onFrameChangeListener : (index : Int) -> Unit? = {}

    fun setOnFrameChangeListener( f : (index : Int) -> Unit?) {
        onFrameChangeListener = f
    }

    override fun selectDrawable(index: Int): Boolean {
        val drawableChanged = super.selectDrawable(index)
        onFrameChangeListener(index)
        return drawableChanged
    }
}