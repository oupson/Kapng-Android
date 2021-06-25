package oupson.apngcreator.fragments


import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import coil.load
import oupson.apng.decoder.ApngLoader
import oupson.apng.drawable.ApngDrawable
import oupson.apngcreator.BuildConfig
import oupson.apngcreator.R


class KotlinFragment : Fragment() {
    companion object {
        private const val TAG = "KotlinFragment"

        @JvmStatic
        fun newInstance() =
            KotlinFragment()
    }

    private var apngImageView: ImageView? = null
    private var normalImageView: ImageView? = null

    private var pauseButton: Button? = null
    private var playButton: Button? = null

    private var speedSeekBar: SeekBar? = null

    //private var animator : ApngAnimator? = null
    private var animation: ApngDrawable? = null
    private var durations: IntArray? = null

    private var frameIndex = 0

    private val imageUrls = arrayListOf(
        "http://oupson.oupsman.fr/apng/bigApng.png",
        "https://metagif.files.wordpress.com/2015/01/bugbuckbunny.png",
        "https://upload.wikimedia.org/wikipedia/commons/3/3f/JPEG_example_flower.jpg",
        "http://orig06.deviantart.net/7812/f/2012/233/7/5/twilight_rapidash_shaded_and_animated_by_tamalesyatole-d5bz7hd.png",
        "https://raw.githubusercontent.com/tinify/iMessage-Panda-sticker/master/StickerPackExtension/Stickers.xcstickers/Sticker%20Pack.stickerpack/panda.sticker/panda.png",
        "file:///android_asset/image.png"
    )
    private val selected = 4

    private var apngLoader: ApngLoader? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (BuildConfig.DEBUG)
            Log.v(TAG, "onCreateView()")

        val view = inflater.inflate(R.layout.fragment_kotlin, container, false)

        apngLoader = ApngLoader()

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
            Log.v(TAG, "onResume()")

        playButton?.setOnClickListener {
            animation?.start()
        }

        pauseButton?.setOnClickListener {
            animation = animation?.let { animation ->
                val res = ApngDrawable()
                animation.stop()
                res.coverFrame = animation.coverFrame
                val currentFrame = animation.current

                frameLoop@ for (i in 0 until animation.numberOfFrames) {
                    val checkFrame = animation.getFrame(i)
                    if (checkFrame === currentFrame) {
                        frameIndex = i
                        for (k in frameIndex until animation.numberOfFrames) {
                            val frame: Drawable = animation.getFrame(k)
                            res.addFrame(frame, animation.getDuration(i))
                        }
                        for (k in 0 until frameIndex) {
                            val frame: Drawable = animation.getFrame(k)
                            res.addFrame(frame, animation.getDuration(i))
                        }
                        apngImageView?.setImageDrawable(res)
                        animation.invalidateSelf()
                        break@frameLoop
                    }
                }
                res
            }
        }

        speedSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null && durations != null) {
                    val speed = seekBar.progress.toFloat() / 100f
                    animation = animation?.let { animation ->
                        val res = ApngDrawable()
                        animation.stop()
                        res.coverFrame = animation.coverFrame

                        for (i in 0 until animation.numberOfFrames) {
                            res.addFrame(
                                animation.getFrame(i),
                                (durations!![i].toFloat() / speed).toInt()
                            )
                        }

                        apngImageView?.setImageDrawable(res)
                        res.start()
                        res
                    }
                }
            }
        })

        if ((animation == null)) {
            apngLoader?.decodeApngAsyncInto(
                requireContext(),
                imageUrls[selected],
                apngImageView!!,
                callback = object : ApngLoader.Callback {
                    override fun onSuccess(drawable: Drawable) {
                        animation = (drawable as? ApngDrawable)
                        durations = IntArray(animation?.numberOfFrames ?: 0) { i ->
                            animation?.getDuration(i) ?: 0
                        }
                    }

                    override fun onError(error: Throwable) {
                        Log.e(TAG, "Error when decoding apng", error)
                    }
                })
        }

        normalImageView?.load(imageUrls[selected])
    }

    override fun onPause() {
        super.onPause()
        if (BuildConfig.DEBUG)
            Log.v(TAG, "onPause()")

        animation = null
        normalImageView?.setImageDrawable(null)
        apngImageView?.setImageDrawable(null)

        playButton?.setOnClickListener(null)
        pauseButton?.setOnClickListener(null)
        speedSeekBar?.setOnSeekBarChangeListener(null)
    }
}