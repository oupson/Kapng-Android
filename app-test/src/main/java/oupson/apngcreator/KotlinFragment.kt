package oupson.apngcreator


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import oupson.apng.ApngAnimator
import oupson.apng.loadApng


class KotlinFragment : Fragment() {
    private var apngImageView : ImageView? = null
    private var normalImageView : ImageView? = null

    private var pauseButton : Button? = null
    private var playButton : Button? = null

    private var speedSeekBar : SeekBar? = null

    private var animator : ApngAnimator? = null

    private val imageUrls = arrayListOf(
        "http://oupson.oupsman.fr/apng/bigApng.png",
        "https://metagif.files.wordpress.com/2015/01/bugbuckbunny.png",
        "https://upload.wikimedia.org/wikipedia/commons/3/3f/JPEG_example_flower.jpg",
        "http://orig06.deviantart.net/7812/f/2012/233/7/5/twilight_rapidash_shaded_and_animated_by_tamalesyatole-d5bz7hd.png",
        "https://raw.githubusercontent.com/tinify/iMessage-Panda-sticker/master/StickerPackExtension/Stickers.xcstickers/Sticker%20Pack.stickerpack/panda.sticker/panda.png",
        "file:///android_asset/image.png"
    )
    private val selected = 4

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (BuildConfig.DEBUG)
            Log.i(TAG, "onCreateView()")
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_kotlin, container, false)

        apngImageView = view.findViewById(R.id.ApngImageView)
        normalImageView = view.findViewById(R.id.NormalImageView)

        pauseButton = view.findViewById(R.id.PauseButton)
        playButton = view.findViewById(R.id.PlayButton)

        speedSeekBar = view.findViewById(R.id.SpeedSeekBar)

        return view
    }

    override fun onResume() {
        super.onResume()
        if (BuildConfig.DEBUG)
            Log.i(TAG, "onResume()")

        playButton?.setOnClickListener {
            animator?.play()
        }

        pauseButton?.setOnClickListener {
            animator?.pause()
        }

        speedSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null)
                    animator?.speed = seekBar.progress.toFloat() / 100f
            }
        })

        if (animator == null) {
            animator = apngImageView?.loadApng(imageUrls[selected])?.apply {
                onLoaded {
                    setOnFrameChangeLister {
                        // Log.e("app-test", "onLoop")
                    }
                }
            }
        }

        Picasso.get().load(imageUrls[selected]).into(normalImageView)
    }

    override fun onPause() {
        super.onPause()
        if (BuildConfig.DEBUG)
            Log.i(TAG, "onPause()")

        // animator = null
        normalImageView?.setImageDrawable(null)
        // apngImageView?.setImageDrawable(null)

        playButton?.setOnClickListener(null)
        pauseButton?.setOnClickListener(null)
        speedSeekBar?.setOnSeekBarChangeListener(null)
    }

    companion object {
        private const val TAG = "KotlinFragment"
        @JvmStatic
        fun newInstance() =
            KotlinFragment()
    }
}