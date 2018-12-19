package oupson.apngcreator

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_creator.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.customView
import org.jetbrains.anko.imageView
import org.jetbrains.anko.sdk27.coroutines.onClick
import oupson.apng.APNGDisassembler
import oupson.apng.Apng
import oupson.apng.ApngAnimator
import oupson.apng.ImageUtils.PngEncoder
import oupson.apngcreator.adapter.frameListViewAdapter
import java.io.File


class CreatorActivity : AppCompatActivity() {
    var items : ArrayList<Bitmap> = ArrayList()
    var bitmapAdapter : frameListViewAdapter? = null
    val PICK_IMAGE = 999

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creator)
        fab_add_frame.onClick {
            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
            getIntent.type = "image/*"

            val pickIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickIntent.type = "image/*"

            val chooserIntent = Intent.createChooser(getIntent, "Select Image")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))

            startActivityForResult(chooserIntent, PICK_IMAGE)
        }
        fab_create.onClick {
            var apngCreated = Apng()

            items.forEach {
                apngCreated.addFrames(it)
            }


            Log.e("tag", apngCreated.frames.size.toString())
            apngCreated = APNGDisassembler.disassemble(apngCreated.toByteArray())
            apngCreated.optimiseFrame()
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "vtm").writeBytes(apngCreated.toByteArray())
            val a = ApngAnimator(applicationContext)


            a.load(apngCreated.toByteArray())
            a.onLoaded {
                    alert {
                        customView {
                            imageView {
                                Log.e("tag", "${it.anim?.numberOfFrames.toString()} : ${items.size}")
                                it.anim?.let {
                                    for (i in 0 until it.numberOfFrames) {
                                        val vt = Bitmap.createBitmap(it.getFrame(i).intrinsicWidth, it.getFrame(i).intrinsicHeight, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(vt)
                                        it.getFrame(i).draw(canvas)
                                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "frame$i.png").writeBytes(PngEncoder.encode(vt))
                                    }
                                }
                                this.setImageDrawable(it.anim)
                            }
                        }
                    }.show()
                }
            }
        bitmapAdapter = frameListViewAdapter(this, items)
        dragView.adapter = bitmapAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            PICK_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    contentResolver.openInputStream(data?.data).readBytes().apply {
                        items.add(BitmapFactory.decodeByteArray(this, 0, this.size))
                        bitmapAdapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }
}
