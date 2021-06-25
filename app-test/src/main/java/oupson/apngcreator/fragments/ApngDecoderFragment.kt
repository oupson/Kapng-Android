package oupson.apngcreator.fragments


import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import oupson.apng.decoder.ApngDecoder
import oupson.apng.decoder.ApngLoader
import oupson.apng.drawable.ApngDrawable
import oupson.apngcreator.BuildConfig
import oupson.apngcreator.R
import java.net.URL

class ApngDecoderFragment : Fragment() {
    companion object {
        private const val TAG = "ApngDecoderFragment"

        @JvmStatic
        fun newInstance() =
            ApngDecoderFragment()
    }

    private var apngLoader: ApngLoader? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_apng_decoder, container, false)

        val imageView: ImageView = view.findViewById(R.id.apngDecoderImageView) ?: return view

        apngLoader = ApngLoader()

        if (context != null) {
            apngLoader?.decodeApngAsyncInto(
                this.requireContext(),
                URL("https://metagif.files.wordpress.com/2015/01/bugbuckbunny.png"),
                imageView,
                config = ApngDecoder.Config(
                    bitmapConfig = Bitmap.Config.RGB_565,
                    decodeCoverFrame = true
                ),
                callback = object : ApngLoader.Callback {
                    override fun onSuccess(drawable: Drawable) {
                        if (BuildConfig.DEBUG)
                            Log.i(
                                TAG,
                                "onSuccess(), has cover frame : ${(drawable as? ApngDrawable)?.coverFrame != null}"
                            )
                    }

                    override fun onError(error: Throwable) {
                        Log.e(TAG, "onError : $error")
                    }
                })
        }

        return view
    }
}
