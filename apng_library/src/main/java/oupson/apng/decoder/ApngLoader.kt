package oupson.apng.decoder

import android.content.Context
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RawRes
import kotlinx.coroutines.*
import oupson.apng.drawable.ApngDrawable
import oupson.apng.utils.Loader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.net.URL

class ApngLoader(parent: Job? = null) {
    interface Callback {
        /**
         * Function called when the file was successfully decoded.
         * @param drawable Can be an [ApngDrawable] if successful and an [AnimatedImageDrawable] if the image decoded is not an APNG but a gif. If it is not an animated image, it is a [Drawable].
         */
        fun onSuccess(drawable: Drawable)

        /**
         * Function called when something gone wrong.
         * @param error The problem.
         */
        fun onError(error: Exception)
    }

    private val job = SupervisorJob(parent)
    private val coroutineScope: CoroutineScope = CoroutineScope(job)

    fun cancelAll() {
        coroutineScope.cancel(CancellationException("Loading was canceled"))
    }


    /**
     * Load Apng into an imageView.
     * @param context Context needed for animation drawable.
     * @param file File to decode.
     * @param imageView Image View.
     * @param config Decoder configuration
     */
    suspend fun decodeApngInto(
        context: Context,
        file: File,
        imageView: ImageView,
        config: ApngDecoder.Config = ApngDecoder.Config()
    ): Drawable {
        val drawable =
            ApngDecoder.decodeApng(
                context,
                withContext(Dispatchers.IO) {
                    FileInputStream(file)
                },
                config
            )
        withContext(Dispatchers.Main) {
            imageView.setImageDrawable(drawable)
            (drawable as? AnimationDrawable)?.start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (drawable as? AnimatedImageDrawable)?.start()
            }
        }
        return drawable
    }

    /**
     * Load Apng into an imageView.
     * @param context Context needed for animation drawable and content resolver.
     * @param uri Uri to load.
     * @param imageView Image View.
     * @param config Decoder configuration
     */
    suspend fun decodeApngInto(
        context: Context,
        uri: Uri,
        imageView: ImageView,
        config: ApngDecoder.Config = ApngDecoder.Config()
    ): Drawable {
        val inputStream =
            withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri) }
                ?: throw FileNotFoundException("Failed to load $uri") // TODO Better err ?
        val drawable =
            ApngDecoder.decodeApng(
                context,
                inputStream,
                config
            )
        withContext(Dispatchers.Main) {
            imageView.setImageDrawable(drawable)
            (drawable as? AnimationDrawable)?.start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (drawable as? AnimatedImageDrawable)?.start()
            }
        }

        return drawable
    }

    /**
     * Load Apng into an imageView.
     * @param context Context needed to decode the resource and for the animation drawable.
     * @param res Raw resource to load.
     * @param imageView Image View.
     * @param config Decoder configuration
     */
    suspend fun decodeApngInto(
        context: Context, @RawRes res: Int,
        imageView: ImageView,
        config: ApngDecoder.Config = ApngDecoder.Config()
    ): Drawable {
        val drawable =
            ApngDecoder.decodeApng(
                context,
                context.resources.openRawResource(res),
                config
            )
        withContext(Dispatchers.Main) {
            imageView.setImageDrawable(drawable)
            (drawable as? AnimationDrawable)?.start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (drawable as? AnimatedImageDrawable)?.start()
            }
        }
        return drawable
    }

    /**
     * Load Apng into an imageView, asynchronously.
     * @param context Context needed for the animation drawable.
     * @param url URL to load.
     * @param imageView Image View.
     * @param config Decoder configuration
     */
    suspend fun decodeApngInto(
        context: Context,
        url: URL,
        imageView: ImageView,
        config: ApngDecoder.Config = ApngDecoder.Config()
    ): Drawable {

        val drawable = ApngDecoder.decodeApng(
            context,
            ByteArrayInputStream(
                Loader.load(
                    url
                )
            ),
            config
        )
        withContext(Dispatchers.Main) {
            imageView.setImageDrawable(drawable)
            (drawable as? AnimationDrawable)?.start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (drawable as? AnimatedImageDrawable)?.start()
            }
        }

        return drawable
    }

    /**
     * Load Apng into an imageView, asynchronously.
     * @param context Context needed for decoding the image and creating the animation drawable.
     * @param string URL to load
     * @param imageView Image View.
     * @param config Decoder configuration
     */
    @Suppress("unused")
    suspend fun decodeApngInto(
        context: Context,
        string: String,
        imageView: ImageView,
        config: ApngDecoder.Config = ApngDecoder.Config()
    ): Drawable {
        return if (string.startsWith("http://") || string.startsWith("https://")) {
            decodeApngInto(
                context,
                URL(string),
                imageView,
                config
            )
        } else if (File(string).exists()) {
            var pathToLoad =
                if (string.startsWith("content://")) string else "file://$string"
            pathToLoad = pathToLoad.replace("%", "%25").replace("#", "%23")
            decodeApngInto(
                context,
                Uri.parse(pathToLoad),
                imageView,
                config
            )
        } else if (string.startsWith("file://android_asset/")) {
            val drawable =
                ApngDecoder.decodeApng(
                    context,
                    context.assets.open(string.replace("file:///android_asset/", "")),

                    config
                )
            withContext(Dispatchers.Main) {
                imageView.setImageDrawable(drawable)
                (drawable as? AnimationDrawable)?.start()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    (drawable as? AnimatedImageDrawable)?.start()
                }
            }
            drawable
        } else {
            throw Exception("Cannot open string")
        }
    }


// region with callback
    /**
     * Load Apng into an imageView, asynchronously.
     * @param context Context needed for animation drawable.
     * @param file File to decode.
     * @param imageView Image View.
     * @param callback [ApngLoader.Callback] to handle success and error.
     * @param config Decoder configuration
     */
    @Suppress("unused", "BlockingMethodInNonBlockingContext")
    @JvmOverloads
    fun decodeApngAsyncInto(
        context: Context,
        file: File,
        imageView: ImageView,
        callback: Callback? = null,
        config: ApngDecoder.Config = ApngDecoder.Config()
    ) =
        coroutineScope.launch(Dispatchers.Default) {
            try {
                val drawable = decodeApngInto(context, file, imageView, config)
                callback?.onSuccess(drawable)
            } catch (e: Exception) {
                callback?.onError(e)
            }
        }


    /**
     * Load Apng into an imageView, asynchronously.
     * @param context Context needed for animation drawable and content resolver.
     * @param uri Uri to load.
     * @param imageView Image View.
     * @param callback [ApngLoader.Callback] to handle success and error.
     * @param config Decoder configuration
     */
    @Suppress("unused")
    @JvmOverloads
    fun decodeApngAsyncInto(
        context: Context,
        uri: Uri,
        imageView: ImageView,
        callback: Callback? = null,
        config: ApngDecoder.Config = ApngDecoder.Config()
    ) = coroutineScope.launch(Dispatchers.Default) {
        try {
            val drawable = decodeApngInto(context, uri, imageView, config)
            callback?.onSuccess(drawable)
        } catch (e: Exception) {
            callback?.onError(e)
        }
    }

    /**
     * Load Apng into an imageView, asynchronously.
     * @param context Context needed to decode the resource and for the animation drawable.
     * @param res Raw resource to load.
     * @param imageView Image View.
     * @param callback [ApngLoader.Callback] to handle success and error.
     * @param config Decoder configuration
     */
    @Suppress("unused")
    @JvmOverloads
    fun decodeApngAsyncInto(
        context: Context, @RawRes res: Int,
        imageView: ImageView,
        callback: Callback? = null,
        config: ApngDecoder.Config = ApngDecoder.Config()
    ) = coroutineScope.launch(Dispatchers.Default) {
        try {
            val drawable = decodeApngInto(context, res, imageView, config)
            callback?.onSuccess(drawable)
        } catch (e: Exception) {
            callback?.onError(e)
        }
    }

    /**
     * Load Apng into an imageView, asynchronously.
     * @param context Context needed for the animation drawable.
     * @param url URL to load.
     * @param imageView Image View.
     * @param callback [ApngLoader.Callback] to handle success and error.
     * @param config Decoder configuration
     */
    @Suppress("unused", "BlockingMethodInNonBlockingContext")
    @JvmOverloads
    fun decodeApngAsyncInto(
        context: Context,
        url: URL,
        imageView: ImageView,
        callback: Callback? = null,
        config: ApngDecoder.Config = ApngDecoder.Config()
    ) = coroutineScope.launch(Dispatchers.Default) {
        try {
            val drawable = decodeApngInto(context, url, imageView, config)
            callback?.onSuccess(drawable)
        } catch (e: Exception) {
            callback?.onError(e)
        }
    }

    /**
     * Load Apng into an imageView, asynchronously.
     * @param context Context needed for decoding the image and creating the animation drawable.
     * @param string URL to load
     * @param imageView Image View.
     * @param callback [ApngLoader.Callback] to handle success and error.
     * @param config Decoder configuration
     */
    @Suppress("unused")
    @JvmOverloads
    fun decodeApngAsyncInto(
        context: Context,
        string: String,
        imageView: ImageView,
        callback: Callback? = null,
        config: ApngDecoder.Config = ApngDecoder.Config()
    ) =
        coroutineScope.launch(Dispatchers.Default) {
            try {
                val drawable = decodeApngInto(context, string, imageView, config)
                callback?.onSuccess(drawable)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onError(e)
                }
            }
        }

// endregion with callback
}