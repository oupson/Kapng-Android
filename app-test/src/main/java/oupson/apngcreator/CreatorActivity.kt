package oupson.apngcreator

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.view.View
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.jetbrains.anko.*
import org.jetbrains.anko.design.appBarLayout
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
            val apngCreated = Apng()

            items.forEach { bitmap ->
                apngCreated.addFrames(bitmap)
            }

            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "apn0.png").writeBytes(apngCreated.toByteArray())

            apngCreated.apply {
                //optimiseFrame()
            }
            val a = ApngAnimator(applicationContext)
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "apn.png").writeBytes(apngCreated.toByteArray())
            a.load(apngCreated.toByteArray())
            a.onLoaded { anim ->
                alert {
                    customView {
                        ctx.setTheme(R.style.AppTheme_DarkDialog)
                        imageView {
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
            backgroundColor = Color.BLACK
            var bar = verticalLayout {
                id = View.generateViewId()
                backgroundColor = Color.DKGRAY
                appBarLayout {
                    backgroundColor = Color.BLACK
                    toolbar {
                        id = View.generateViewId()
                        title = Html.fromHtml("<font color='#ffffff'>Create APNG</font>")
                    }.lparams {
                        width = matchParent
                        height = wrapContent
                    }
                }.lparams {
                    width = matchParent
                    height = wrapContent
                    bottomMargin = 1
                }
            }.lparams {
                width = matchParent
                height = wrapContent
            }

            listView = listView {
                id = View.generateViewId()
            }.lparams {
                width = matchParent
                height = matchParent
                below(bar)
            }
            addFrameButton = floatingActionButton {
                imageResource = R.drawable.ic_add_black_24dp
                backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                isClickable = true
            }.lparams {
                width = wrapContent
                height = wrapContent
                margin = dip(5)
                alignParentBottom()
                alignParentEnd()
            }
            createButton = floatingActionButton {
                imageResource = R.drawable.ic_play_arrow_black_24dp
                backgroundTintList = ColorStateList.valueOf(Color.WHITE)
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
