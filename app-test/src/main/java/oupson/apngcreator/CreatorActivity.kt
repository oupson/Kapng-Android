package oupson.apngcreator

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_creator.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.customView
import org.jetbrains.anko.imageView
import org.jetbrains.anko.sdk27.coroutines.onClick
import oupson.apng.APNGDisassembler
import oupson.apng.Apng
import oupson.apng.ApngAnimator
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

            items.forEach { bitmap ->
                apngCreated.addFrames(bitmap)
            }

            apngCreated = APNGDisassembler.disassemble(apngCreated.toByteArray()).apply {
                optimiseFrame()
            }
            val a = ApngAnimator(applicationContext)
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "apn.png").writeBytes(apngCreated.toByteArray())
            a.load(apngCreated.toByteArray())
            a.onLoaded { anim ->
                alert {
                        customView {
                            imageView {
                                /**anim.anim?.let {cu ->
                                    for (i in 0 until cu.numberOfFrames) {
                                        val vt = Bitmap.createBitmap(cu.getFrame(i).intrinsicWidth, cu.getFrame(i).intrinsicHeight, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(vt)
                                        cu.getFrame(i).draw(canvas)
                                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "frameCreated$i.png").writeBytes(PngEncoder.encode(vt))
                                    }
                                }
                                this.setImageDrawable(anim.anim)
                                */
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
