package oupson.apngcreator

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main2.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import oupson.apng.ApngAnimator
import oupson.apng.Utils.Companion.isApng
import oupson.apng.exceptions.NotApngException
import java.io.File


class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
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

    fun load() {
        val animator = ApngAnimator().loadInto(imageView3)
        val uri = intent.data
        if (uri.toString().contains("file:///")) {
            try {
                if (isApng(File(uri.path).readBytes())) {
                    animator.load(this, uri.path)
                } else {
                    imageView3.setImageBitmap(BitmapFactory.decodeFile(uri.path))
                    Snackbar.make(constraint, "Not an APNG, and verified !", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: NotApngException) {
                imageView3.setImageBitmap(BitmapFactory.decodeFile(uri.path))
                Snackbar.make(constraint, "Not an APNG", Snackbar.LENGTH_LONG).show()
            }
        } else {
            try {
                animator.load(this, getImageRealPath(contentResolver, uri, null))
            } catch (e: NotApngException) {
                imageView3.setImageBitmap(BitmapFactory.decodeFile(getImageRealPath(contentResolver, uri, null)))
                Snackbar.make(constraint, "Not an APNG", Snackbar.LENGTH_LONG).show()
            }
        }
        imageView3.onClick {
            try {
                if (animator.isPlaying) {
                    animator.pause()
                } else {
                    animator.play()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getImageRealPath(contentResolver: ContentResolver, uri: Uri, whereClause: String?): String {
        var ret = ""

        // Query the uri with condition.
        val cursor = contentResolver.query(uri, null, whereClause, null, null)

        if (cursor != null) {
            val moveToFirst = cursor.moveToFirst()
            if (moveToFirst) {
                // Get columns name by uri type.
                var columnName = MediaStore.Images.Media.DATA

                if (uri === MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                    columnName = MediaStore.Images.Media.DATA
                } else if (uri === MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) {
                    columnName = MediaStore.Audio.Media.DATA
                } else if (uri === MediaStore.Video.Media.EXTERNAL_CONTENT_URI) {
                    columnName = MediaStore.Video.Media.DATA
                }

                // Get column index.
                val imageColumnIndex = cursor.getColumnIndex(columnName)

                // Get column value which is the uri related file local path.
                ret = cursor.getString(imageColumnIndex)
            }
        }

        return ret
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            2 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    load()

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request.
    }
}
