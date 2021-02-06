package oupson.apng

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import oupson.apng.utils.Utils


class ApngEncoderInstrumentedTest {
    // TODO TEST IF OPTIMISED ANIMATIONS AND NON-OPTIMISED ANIMATIONS ARE THE SAME

    @Test
    fun testDiffBunny() {
        val context = InstrumentationRegistry.getInstrumentation().context

        val bunnyFrame1 = getFrame(context, "bunny/frame_apngframe01.png")
        val bunnyFrame2 = getFrame(context, "bunny/frame_apngframe02.png")

        val diffBunny1to2 = Utils.getDiffBitmap(bunnyFrame1, bunnyFrame2)

        assertTrue(isSimilar(bunnyFrame1, bunnyFrame2, diffBunny1to2))
    }

    @Test
    fun testDiffBall() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val ballFrame1 = getFrame(context, "ball/apngframe01.png")
        val ballFrame2 = getFrame(context, "ball/apngframe02.png")

        val diffBall1to2 = Utils.getDiffBitmap(ballFrame1, ballFrame2)

        assertTrue(isSimilar(ballFrame1, ballFrame2, diffBall1to2))
    }

    @Test
    fun containTransparency() {
        val context = InstrumentationRegistry.getInstrumentation().context

        val bunnyFrame1 = getFrame(context, "bunny/frame_apngframe01.png")
        assertFalse(Utils.containTransparency(bunnyFrame1))

        val ballFrame1 = getFrame(context, "ball/apngframe01.png")
        assertTrue(Utils.containTransparency(ballFrame1))
    }

    private fun isSimilar(buffer : Bitmap, frame : Bitmap, diff : Utils.Companion.DiffResult) : Boolean {
        val btm = buffer.copy(Bitmap.Config.ARGB_8888, true)

        for (y in 0 until diff.bitmap.height) {
            for (x in 0 until diff.bitmap.width) {
                val p = diff.bitmap.getPixel(x, y)
                if (p != Color.TRANSPARENT || p == Color.TRANSPARENT && diff.blendOp == Utils.Companion.BlendOp.APNG_BLEND_OP_SOURCE)
                    btm.setPixel(diff.offsetX + x, diff.offsetY + y, p)
            }
        }

        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                if (frame.getPixel(x, y) != btm.getPixel(x, y)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getFrame(context: Context, name: String) : Bitmap {
        val inputStream = context.assets.open(name)

        val bitmap = BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })

        inputStream.close()
        return bitmap!!
    }
}