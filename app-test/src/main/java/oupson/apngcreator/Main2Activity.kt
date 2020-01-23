package oupson.apngcreator

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.imageView
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.verticalLayout
import oupson.apng.CustomAnimationDrawable
import oupson.apng.ExperimentalApngDecoder

class Main2Activity : AppCompatActivity() {
    private lateinit var imageView : ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            imageView = imageView().lparams {
                width = matchParent
                height = matchParent
            }
            backgroundColor = Color.parseColor("#323232")
        }
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    2)
        } else {
            load()
        }
    }

    private fun load() {
        val uri = intent.data ?: return
        //val animator = imageView.loadApng(uri, null)
        val drawable = ExperimentalApngDecoder.decodeApng(this, uri)
        imageView.setImageDrawable(drawable)
        if (drawable is CustomAnimationDrawable)
            drawable.start()
        /**
        imageView.onClick {
            try {
                if (animator.isApng) {
                    if (animator.isPlaying) {
                        animator.pause()
                    } else {
                        animator.play()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        */
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            2 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    load()
                }
                return
            }
        }
    }
}
