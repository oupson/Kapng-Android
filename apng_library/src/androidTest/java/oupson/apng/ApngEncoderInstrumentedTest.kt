package oupson.apng

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import oupson.apng.decoder.ApngDecoder
import oupson.apng.encoder.ApngEncoder
import oupson.apng.utils.Utils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class ApngEncoderInstrumentedTest {
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
    fun testContainTransparency() {
        val context = InstrumentationRegistry.getInstrumentation().context

        val bunnyFrame1 = getFrame(context, "bunny/frame_apngframe01.png")
        assertFalse(Utils.containTransparency(bunnyFrame1))

        val ballFrame1 = getFrame(context, "ball/apngframe01.png")
        assertTrue(Utils.containTransparency(ballFrame1))
    }

    @Test
    fun testOptimiseBall() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val list = context.assets.list("ball")?.map { getFrame(context, "ball/$it") }!!

        val optimisedOutputStream = ByteArrayOutputStream()
        val optimisedEncoder = ApngEncoder(optimisedOutputStream, list[0].width, list[0].height, list.size)
            .setOptimiseApng(true)

        list.forEach {
            optimisedEncoder.writeFrame(it)
        }

        optimisedEncoder.writeEnd()
        optimisedOutputStream.close()

        val bytes = optimisedOutputStream.toByteArray()
        val optimisedInputStream = ByteArrayInputStream(bytes)

        val optimisedApng =
            ApngDecoder.decodeApng(context, optimisedInputStream) as AnimationDrawable

        optimisedInputStream.close()



        val nonOptimisedOutputStream = ByteArrayOutputStream()

        val nonOptimisedEncoder = ApngEncoder(nonOptimisedOutputStream, list[0].width, list[0].height, list.size)
            .setOptimiseApng(false)

        list.forEach {
            nonOptimisedEncoder.writeFrame(it)
        }

        nonOptimisedEncoder.writeEnd()

        nonOptimisedOutputStream.close()

        val nonOptimisedBytes = nonOptimisedOutputStream.toByteArray()
        val nonOptimisedInputStream = ByteArrayInputStream(nonOptimisedBytes)

        val nonOptimisedApng =
            ApngDecoder.decodeApng(context, nonOptimisedInputStream) as AnimationDrawable
        nonOptimisedInputStream.close()

        for (i in 0 until optimisedApng.numberOfFrames) {
            assertTrue(
                isBitmapSimilar(
                    (optimisedApng.getFrame(i) as BitmapDrawable).bitmap,
                    (nonOptimisedApng.getFrame(i) as BitmapDrawable).bitmap
                )
            )
        }
    }

    @Test
    fun testOptimiseBunny() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val list = context.assets.list("bunny")?.map { getFrame(context, "bunny/$it") }!!

        val optimisedOutputStream = ByteArrayOutputStream()
        val optimisedEncoder = ApngEncoder(optimisedOutputStream, list[0].width, list[0].height, list.size)
            .setOptimiseApng(true)

        list.forEach {
            optimisedEncoder.writeFrame(it)
        }

        optimisedEncoder.writeEnd()
        optimisedOutputStream.close()

        val bytes = optimisedOutputStream.toByteArray()
        val optimisedInputStream = ByteArrayInputStream(bytes)

        val optimisedApng =
            ApngDecoder.decodeApng(context, optimisedInputStream) as AnimationDrawable

        optimisedInputStream.close()



        val nonOptimisedOutputStream = ByteArrayOutputStream()

        val nonOptimisedEncoder = ApngEncoder(nonOptimisedOutputStream, list[0].width, list[0].height, list.size)
            .setOptimiseApng(false)

        list.forEach {
            nonOptimisedEncoder.writeFrame(it)
        }

        nonOptimisedEncoder.writeEnd()

        nonOptimisedOutputStream.close()

        val nonOptimisedBytes = nonOptimisedOutputStream.toByteArray()
        val nonOptimisedInputStream = ByteArrayInputStream(nonOptimisedBytes)

        val nonOptimisedApng =
            ApngDecoder.decodeApng(context, nonOptimisedInputStream) as AnimationDrawable
        nonOptimisedInputStream.close()

        for (i in 0 until optimisedApng.numberOfFrames) {
            assertTrue(
                isBitmapSimilar(
                    (optimisedApng.getFrame(i) as BitmapDrawable).bitmap,
                    (nonOptimisedApng.getFrame(i) as BitmapDrawable).bitmap
                )
            )
        }
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

    fun isBitmapSimilar(btm1 : Bitmap, btm2 : Bitmap) : Boolean {
        for (y in 0 until btm1.height) {
            for (x in 0 until btm1.width) {
                if (btm1.getPixel(x, y) != btm2.getPixel(x, y)) {
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