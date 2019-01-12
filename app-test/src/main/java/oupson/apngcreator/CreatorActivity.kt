package oupson.apngcreator

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import org.jetbrains.anko.*
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.sdk27.coroutines.onClick
import oupson.apng.Apng
import oupson.apng.ApngAnimator
import oupson.apngcreator.adapter.AnkoAdapter
import java.io.File

class CreatorActivity : AppCompatActivity() {
    var items : ArrayList<Bitmap> = ArrayList()
    var bitmapAdapter : AnkoAdapter<Bitmap>? = null
    val PICK_IMAGE = 999
    var view = CreatorActivityLayout()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view.setContentView(this)
        view.addFrameButton.onClick {
            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
            getIntent.type = "image/*"

            val pickIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickIntent.type = "image/*"

            val chooserIntent = Intent.createChooser(getIntent, "Select Image")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))

            startActivityForResult(chooserIntent, PICK_IMAGE)
        }
        view.createButton.onClick {
            var apngCreated = Apng()

            items.forEach { bitmap ->
                apngCreated.addFrames(bitmap)
            }

            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "apn0.png").writeBytes(apngCreated.toByteArray())

            apngCreated.apply {
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
                                */
                                this.setImageDrawable(anim.anim)
                            }
                        }
                    }.show()
                }
            }
        bitmapAdapter = AnkoAdapter({items}) {index, items, view ->
            with(items[index]) {
                verticalLayout {
                    lparams {
                        width = matchParent
                        height = matchParent
                    }
                    imageView {
                        setImageBitmap(this@with)
                    }.lparams {
                        width = matchParent
                        height = matchParent
                    }
                }
            }
        }
        /*        frameListViewAdapter(this, items) */
        view.listView.adapter = bitmapAdapter
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

class CreatorActivityLayout : AnkoComponent<CreatorActivity> {
    lateinit var listView : ListView
    lateinit var addFrameButton : FloatingActionButton
    lateinit var createButton : FloatingActionButton

    override fun createView(ui: AnkoContext<CreatorActivity>) = with(ui) {
        relativeLayout {
            listView = listView {
            }.lparams {
                width = matchParent
                height = matchParent
            }
            addFrameButton = floatingActionButton {
                imageResource = R.drawable.ic_add_white_24dp
                isClickable = true
            }.lparams {
                width = wrapContent
                height = wrapContent
                margin = dip(5)
                alignParentBottom()
                alignParentEnd()
            }
            createButton = floatingActionButton {
                imageResource = R.drawable.ic_play_arrow_white_24dp
                isClickable = true
            }.lparams {
                width = wrapContent
                height = wrapContent
                margin = dip(5)
                alignParentBottom()
                alignParentStart()
            }
        }
    }
}
