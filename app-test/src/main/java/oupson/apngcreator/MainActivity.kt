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

    val imageUrl = "https://raw.githubusercontent.com/tinify/iMessage-Panda-sticker/master/Source/panda-original.png"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val apng = Apng()
        val file1 = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES), "hopital.jpg")
        val file2 = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES), "test.jpg")
        apng.addFrames(BitmapFactory.decodeByteArray(file2.readBytes(), 0, file2.readBytes().size), 2000f, Utils.Companion.blend_op.APNG_BLEND_OP_OVER, Utils.Companion.dispose_op.APNG_DISPOSE_OP_NONE)

        apng.addFrames(BitmapFactory.decodeByteArray(file1.readBytes(), 0, file1.readBytes().size), 1000f, Utils.Companion.blend_op.APNG_BLEND_OP_OVER, Utils.Companion.dispose_op.APNG_DISPOSE_OP_NONE)

        animator = ApngAnimator(imageView)
        animator.load(apng.generateAPNGByteArray())

        Picasso.get().load(imageUrl).into(imageView2);
        val out = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES), "out.png")
        out.createNewFile()
        out.writeBytes(apng.generateAPNGByteArray())

        play.setOnClickListener {
            animator.play()
        }

        pause.setOnClickListener {
            animator.pause()
        }
    }
}
