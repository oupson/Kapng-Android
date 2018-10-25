package oupson.apngcreator

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main2.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import oupson.apng.ApngAnimator

class Main2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

    }

    override fun onResume() {
        super.onResume()
        val animator = ApngAnimator(this).loadInto(imageView2)
        val uri = intent.data
        if (uri.toString().contains("file:///")) {
            animator.load(uri.path)
        }
        Log.e("TAG", intent.data.toString())
        animator.load(getImageRealPath(contentResolver, uri, null))

        imageView3.onClick {
            if (animator.isPlaying) {
                animator.pause()
            } else {
                animator.play()
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
}
