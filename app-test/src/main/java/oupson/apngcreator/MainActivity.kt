package oupson.apngcreator

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.squareup.picasso.Picasso
import org.jetbrains.anko.*
import org.jetbrains.anko.constraint.layout.ConstraintSetBuilder
import org.jetbrains.anko.constraint.layout.applyConstraintSet
import org.jetbrains.anko.constraint.layout.constraintLayout
import org.jetbrains.anko.constraint.layout.matchConstraint
import org.jetbrains.anko.design.appBarLayout
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onMenuItemClick
import org.jetbrains.anko.sdk27.coroutines.onSeekBarChangeListener
import oupson.apng.ApngAnimator
import oupson.apng.loadApng

class MainActivity : AppCompatActivity() {
    private lateinit var animator: ApngAnimator
    private lateinit var tool : Toolbar
    // val imageUrl = "http://oupson.oupsman.fr/apng/bigApng.png"
    private val imageUrl = "https://metagif.files.wordpress.com/2015/01/bugbuckbunny.png"
    // val imageUrl = "http://orig06.deviantart.net/7812/f/2012/233/7/5/twilight_rapidash_shaded_and_animated_by_tamalesyatole-d5bz7hd.png"
    // val imageUrl = "https://raw.githubusercontent.com/tinify/iMessage-Panda-sticker/master/StickerPackExtension/Stickers.xcstickers/Sticker%20Pack.stickerpack/panda.sticker/panda.png"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val buttonDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dip(5).toFloat()
            setStroke(2, Color.WHITE)
        }
        verticalLayout {
            backgroundColor = Color.BLACK
            verticalLayout {
                backgroundColor = Color.DKGRAY
                appBarLayout {
                    backgroundColor = Color.BLACK
                    tool = toolbar {
                        id = View.generateViewId()
                        title = Html.fromHtml("<font color='#ffffff'>MainActivity</font>", Html.FROM_HTML_MODE_LEGACY)
                        inflateMenu(R.menu.main_menu)
                        onMenuItemClick { item ->
                            when (item!!.itemId) {
                                R.id.action_open_create_activity -> {
                                    startActivity<CreatorActivity>()
                                    finish()
                                }
                            }
                        }
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
                val pauseButton = button("pause") {
                    id = View.generateViewId()
                    background = buttonDrawable
                    onClick {
                        animator.pause()
                    }
                }.lparams(
                        width = wrapContent,
                        height = wrapContent
                )
                val playButton = button("play") {
                    background = buttonDrawable
                    id = View.generateViewId()
                    onClick {
                        animator.play()
                    }
                }.lparams(
                        width = wrapContent,
                        height = wrapContent
                )
                val seekBar = themedSeekBar(R.style.AppTheme_SeekBar){
                    id = View.generateViewId()
                    max = 200
                    progress = 10
                    onSeekBarChangeListener {
                        onProgressChanged { _, _, _ -> }
                        onStartTrackingTouch { }
                        onStopTrackingTouch { seekBar ->
                            animator.speed = (seekBar?.progress?.toFloat() ?: 100f / 100f)
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
                    animator = this.loadApng("file:///android_asset/image.png").apply {
                        onLoaded {
                            setOnAnimationLoopListener {
                                // Log.e("app-test", "onLoop")
                            }
                        }
                    }
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
    }
}
