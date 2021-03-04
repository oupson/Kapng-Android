package oupson.apng

import android.graphics.Bitmap
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import oupson.apng.Utils.Companion.areBitmapSimilar
import oupson.apng.Utils.Companion.getFrame
import oupson.apng.decoder.ApngDecoder
import oupson.apng.drawable.ApngDrawable

class ApngDecoderInstrumentedTest {
    @Test
    fun testBtmConfigDecoding() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val input = context.assets.open("sushi.png")
        val anim = runBlocking {
            ApngDecoder.decodeApng(
                context,
                input,
                ApngDecoder.Config(bitmapConfig = Bitmap.Config.RGB_565)
            ) as AnimationDrawable
        }

        for (i in 0 until anim.numberOfFrames) {
            TestCase.assertTrue((anim.getFrame(i) as BitmapDrawable).bitmap.config == Bitmap.Config.RGB_565)
        }
    }

    @Test
    fun testBigBuckBunny() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val list = context.assets.list("bunny")?.map { getFrame(context, "bunny/$it") }!!

        val input = context.assets.open("bugbuckbunny.png")
        val anim = runBlocking {
            ApngDecoder.decodeApng(
                context,
                input,
                ApngDecoder.Config(bitmapConfig = Bitmap.Config.ARGB_8888, decodeCoverFrame = true)
            ) as ApngDrawable
        }

        TestCase.assertTrue(areBitmapSimilar(list[0], anim.coverFrame!!))
        for (i in 0 until anim.numberOfFrames) {
            TestCase.assertEquals(100, anim.getDuration(i))
            TestCase.assertTrue(
                areBitmapSimilar(
                    (anim.getFrame(i) as BitmapDrawable).bitmap,
                    list[i + 1]
                )
            )
        }
    }
}