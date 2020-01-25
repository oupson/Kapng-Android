package oupson.apngcreator


import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import oupson.apng.ExperimentalApngDecoder
import java.net.URL

class ApngDecoderFragment : Fragment() {
    companion object {
        private const val TAG = "ApngDecoderFragment"
        @JvmStatic
        fun newInstance() =
            ApngDecoderFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_apng_decoder, container, false)

        val imageView : ImageView = view.findViewById(R.id.apngDecoderImageView) ?: return  view

        if (context != null) {
            ExperimentalApngDecoder.decodeApngAsyncInto(
                this.context!!,
                URL("http://littlesvr.ca/apng/images/GenevaDrive.png"),
                imageView,
                callback = object : ExperimentalApngDecoder.Callback {
                    override fun onSuccess(drawable: Drawable) {
                        if (BuildConfig.DEBUG)
                            Log.i(TAG, "onSuccess()")
                    }

                    override fun onError(error: Exception) {
                        if (BuildConfig.DEBUG)
                            Log.e(TAG, "onError", error)
                    }
                })
        }

        return view
    }


}
