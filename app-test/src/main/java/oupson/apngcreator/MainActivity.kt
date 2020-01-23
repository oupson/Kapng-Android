package oupson.apngcreator

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.constraint.layout.ConstraintSetBuilder
import org.jetbrains.anko.constraint.layout.applyConstraintSet
import org.jetbrains.anko.constraint.layout.constraintLayout
import org.jetbrains.anko.constraint.layout.matchConstraint
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.design.appBarLayout
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onSeekBarChangeListener
import oupson.apng.ApngAnimator
import oupson.apng.CustomAnimationDrawable
import oupson.apng.ExperimentalApngDecoder
import java.net.URL

// TODO REMOVE ANKO
fun ViewManager.xToolbar(init : androidx.appcompat.widget.Toolbar.() -> Unit) = ankoView({androidx.appcompat.widget.Toolbar(it)}, 0, init)
class MainActivity : AppCompatActivity() {
    private lateinit var animator: ApngAnimator
    private lateinit var tool : androidx.appcompat.widget.Toolbar
    // val imageUrl = "http://oupson.oupsman.fr/apng/bigApng.png"
    private val imageUrl = "https://metagif.files.wordpress.com/2015/01/bugbuckbunny.png"
    //private val imageUrl = "https://upload.wikimedia.org/wikipedia/commons/3/3f/JPEG_example_flower.jpg"
    // val imageUrl = "http://orig06.deviantart.net/7812/f/2012/233/7/5/twilight_rapidash_shaded_and_animated_by_tamalesyatole-d5bz7hd.png"
    // val imageUrl = "https://raw.githubusercontent.com/tinify/iMessage-Panda-sticker/master/StickerPackExtension/Stickers.xcstickers/Sticker%20Pack.stickerpack/panda.sticker/panda.png"
    // val imageUrl = "file:///android_asset/image.png"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verticalLayout {
            verticalLayout {
                appBarLayout {
                    tool = xToolbar {
                        id = View.generateViewId()
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

            constraintLayout {
                backgroundColor = Color.WHITE
                val pauseButton = button("pause") {
                    backgroundColor = Color.WHITE
                    id = View.generateViewId()
                    onClick {
                        animator.pause()
                    }
                }.lparams(
                        width = wrapContent,
                        height = wrapContent
                )
                val playButton = button("play") {
                    backgroundColor = Color.WHITE
                    id = View.generateViewId()
                    onClick {
                        animator.play()
                    }
                }.lparams(
                        width = wrapContent,
                        height = wrapContent
                )
                val seekBar = seekBar {
                    id = View.generateViewId()
                    max = 200
                    progress = 100
                    onSeekBarChangeListener {
                        onProgressChanged { _, _, _ -> }
                        onStartTrackingTouch { }
                        onStopTrackingTouch { seekBar ->
                            animator.speed = (seekBar?.progress?.toFloat() ?: 100f) / 100f
                        }
                    }
                }.lparams(
                        width = matchConstraint,
                        height = wrapContent
                )
                val imageView2 = imageView {
                    id = View.generateViewId()
                    Picasso.get().load(imageUrl).into(this)
                }.lparams(
                        width = matchConstraint,
                        height = matchConstraint
                )
                val imageView = imageView {
                    id = View.generateViewId()
                    GlobalScope.launch(Dispatchers.IO) {
                        val drawable = ExperimentalApngDecoder.decodeApng(this@MainActivity, URL(imageUrl))
                        GlobalScope.launch(Dispatchers.Main) {
                            this@imageView.setImageDrawable(drawable)
                            if (drawable is CustomAnimationDrawable)
                                drawable.start()
                        }
                    }


                    /**
                    animator = this.loadApng(imageUrl).apply {
                        onLoaded {
                            setOnFrameChangeLister {
                                // Log.e("app-test", "onLoop")
                            }
                        }
                    }
                     */
                }.lparams(
                        width = matchConstraint,
                        height = matchConstraint
                )
                applyConstraintSet {
                    pauseButton {
                        connect(
                                ConstraintSetBuilder.Side.BOTTOM to ConstraintSetBuilder.Side.TOP of seekBar margin dip(8),
                                ConstraintSetBuilder.Side.END to ConstraintSetBuilder.Side.END of ConstraintLayout.LayoutParams.PARENT_ID margin dip(8)
                        )
                    }
                    playButton {
                        connect(
                                ConstraintSetBuilder.Side.BOTTOM to ConstraintSetBuilder.Side.TOP of seekBar margin dip(8),
                                ConstraintSetBuilder.Side.START to ConstraintSetBuilder.Side.START of ConstraintLayout.LayoutParams.PARENT_ID margin dip(8)
                        )
                    }
                    seekBar {
                        connect(
                                ConstraintSetBuilder.Side.BOTTOM to ConstraintSetBuilder.Side.BOTTOM of ConstraintLayout.LayoutParams.PARENT_ID margin dip(8),
                                ConstraintSetBuilder.Side.END to ConstraintSetBuilder.Side.END of ConstraintLayout.LayoutParams.PARENT_ID margin dip(8),
                                ConstraintSetBuilder.Side.START to ConstraintSetBuilder.Side.START of ConstraintLayout.LayoutParams.PARENT_ID margin dip(8)
                        )
                    }
                    imageView2 {
                        connect(
                                ConstraintSetBuilder.Side.BOTTOM to ConstraintSetBuilder.Side.TOP of playButton margin dip(8),
                                ConstraintSetBuilder.Side.END to ConstraintSetBuilder.Side.END of ConstraintLayout.LayoutParams.PARENT_ID margin dip(8),
                                ConstraintSetBuilder.Side.START to ConstraintSetBuilder.Side.START of ConstraintLayout.LayoutParams.PARENT_ID margin dip(8),
                                ConstraintSetBuilder.Side.TOP to ConstraintSetBuilder.Side.BOTTOM of imageView
                        )
                    }
                    imageView {
                        connect(
                                ConstraintSetBuilder.Side.BOTTOM to ConstraintSetBuilder.Side.TOP of imageView2 margin dip(8),
                                ConstraintSetBuilder.Side.END to ConstraintSetBuilder.Side.END of ConstraintLayout.LayoutParams.PARENT_ID margin dip(8),
                                ConstraintSetBuilder.Side.START to ConstraintSetBuilder.Side.START of ConstraintLayout.LayoutParams.PARENT_ID margin dip(8),
                                ConstraintSetBuilder.Side.TOP to ConstraintSetBuilder.Side.TOP of ConstraintLayout.LayoutParams.PARENT_ID margin dip(8)
                        )
                    }
                }
            }.lparams {
                width = matchParent
                height = matchParent
            }
        }
        setSupportActionBar(tool)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_open_create_activity -> startActivity<CreatorActivity>()
            R.id.action_open_java_activity -> startActivity<JavaActivity>()
        }
        return super.onOptionsItemSelected(item)
    }
}
