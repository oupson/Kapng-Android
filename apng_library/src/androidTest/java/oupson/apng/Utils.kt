package oupson.apng

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import oupson.apng.utils.Utils

class Utils {
    companion object {
        fun getFrame(context: Context, name: String, btmConfig : Bitmap.Config = Bitmap.Config.ARGB_8888) : Bitmap {
            val inputStream = context.assets.open(name)

            val bitmap = BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
                inPreferredConfig = btmConfig
            })

            inputStream.close()
            return bitmap!!
        }

        fun isSimilar(buffer : Bitmap, frame : Bitmap, diff : Utils.Companion.DiffResult) : Boolean {
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

        fun areBitmapSimilar(btm1 : Bitmap, btm2 : Bitmap) : Boolean {
            for (y in 0 until btm1.height) {
                for (x in 0 until btm1.width) {
                    if (btm1.getPixel(x, y) != btm2.getPixel(x, y)) {
                        return false
                    }
                }
            }
            return true
        }
    }
}