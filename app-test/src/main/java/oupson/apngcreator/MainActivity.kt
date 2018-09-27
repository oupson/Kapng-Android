package oupson.apngcreator

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.graphics.drawable.Drawable
import android.graphics.drawable.AnimationDrawable
import android.os.Environment
import android.system.Os
import oupson.apng.APNGDisassembler
import java.io.File
import android.os.Environment.getExternalStorageDirectory
import kotlinx.android.synthetic.main.activity_main.*
import oupson.apng.ApngAnimator


class MainActivity : AppCompatActivity() {
    lateinit var animator : ApngAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        animator = ApngAnimator(imageView)
        animator.load(File(File(Environment.getExternalStorageDirectory(), "documents"), "image_3.png"))

        play.setOnClickListener {
            animator.play()
        }

        pause.setOnClickListener {
            animator.pause()
        }
    }
}
