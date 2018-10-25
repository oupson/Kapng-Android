package oupson.apngcreator

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import oupson.apng.ApngAnimator


class MainActivity : AppCompatActivity() {
    lateinit var animator : ApngAnimator

    val imageUrl = "https://metagif.files.wordpress.com/2015/01/bugbuckbunny.png"
    //val imageUrl = "https://raw.githubusercontent.com/tinify/iMessage-Panda-sticker/master/StickerPackExtension/Stickers.xcstickers/Sticker%20Pack.stickerpack/panda.sticker/panda.png"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        animator = ApngAnimator(this).loadInto(imageView)
        animator.load(imageUrl)

        Picasso.get().load(imageUrl).into(imageView2)

        play.setOnClickListener {
            animator.play()
        }

        pause.setOnClickListener {
            animator.pause()
        }
    }
}
