package oupson.apng

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertTrue
import org.junit.Test
import oupson.apng.encoder.ApngEncoder


class ApngEncoderInstrumentedTest {
    @Test
    fun testDiff() { // TODO BLEND / DISPOSE OP
        val context = InstrumentationRegistry.getInstrumentation().context

        val frame1 = getFrame(context, "bunny/frame_apngframe01.png")
        val frame2 = getFrame(context, "bunny/frame_apngframe02.png")

        val diff = ApngEncoder.getDiffBitmap(frame1, frame2)

        assertTrue(isSimilar(frame1, frame2, diff))
    }

    private fun isSimilar(buffer : Bitmap, frame : Bitmap, diff : Triple<Bitmap, Int, Int>) : Boolean {
        val btm = buffer.copy(Bitmap.Config.ARGB_8888, true)

        for (y in 0 until diff.first.height) {
            for (x in 0 until diff.first.width) {
                val p = diff.first.getPixel(x, y)
                if (p != Color.TRANSPARENT)
                    btm.setPixel(diff.second + x, diff.third + y, p)
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