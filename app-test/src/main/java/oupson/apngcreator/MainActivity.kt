package oupson.apngcreator

import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.graphics.drawable.Drawable
import android.graphics.drawable.AnimationDrawable
import android.os.Environment
import android.os.Environment.DIRECTORY_PICTURES
import android.system.Os
import oupson.apng.APNGDisassembler
import java.io.File
import android.os.Environment.getExternalStorageDirectory
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import oupson.apng.ApngAnimator
import java.nio.file.Files.exists
import android.system.Os.mkdir
import android.util.Log
import android.widget.Toast
import com.squareup.picasso.Picasso
import oupson.apng.Apng
import oupson.apng.Utils
import java.io.ByteArrayOutputStream
import java.lang.reflect.Field
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL


class MainActivity : AppCompatActivity() {
    lateinit var animator : ApngAnimator

    val imageUrl = "https://metagif.files.wordpress.com/2015/01/bugbuckbunny.png"

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
