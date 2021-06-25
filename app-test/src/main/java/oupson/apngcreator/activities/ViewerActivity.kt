package oupson.apngcreator.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import oupson.apng.decoder.ApngDecoder
import oupson.apng.decoder.ApngLoader
import oupson.apngcreator.databinding.ActivityViewerBinding

class ViewerActivity : AppCompatActivity() {
    private var apngLoader: ApngLoader? = null
    private var binding: ActivityViewerBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)

        setContentView(binding?.root)

        this.apngLoader = ApngLoader()

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onResume() {
        super.onResume()


        if (intent.data != null) {
            if (intent.data!!.scheme == "file" && ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    2
                )
            } else {
                load()
            }
        }
    }

    private fun load() {
        val uri = intent.data ?: return

        if (binding != null)
            apngLoader?.decodeApngAsyncInto(
                this,
                uri,
                binding!!.viewerImageView,
                callback = object : ApngLoader.Callback {
                    override fun onSuccess(drawable: Drawable) {}
                    override fun onError(error: Throwable) {
                        Log.e("ViewerActivity", "Error when loading file", error)
                    }
                },
                ApngDecoder.Config(decodeCoverFrame = false)
            )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
