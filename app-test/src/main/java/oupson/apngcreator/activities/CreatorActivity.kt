package oupson.apngcreator.activities

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import oupson.apng.encoder.ApngEncoder
import oupson.apng.utils.Utils
import oupson.apngcreator.BuildConfig
import oupson.apngcreator.R
import oupson.apngcreator.adapter.ImageAdapter
import oupson.apngcreator.databinding.ActivityCreatorBinding
import oupson.apngcreator.dialogs.DelayInputDialog
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList

class CreatorActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CreatorActivity"

        private const val CREATION_CHANNEL_ID =
            "${BuildConfig.APPLICATION_ID}.notifications_channels.create"
    }

    private var items: ArrayList<Triple<Uri, Int, Long>> = ArrayList()
    private var adapter: ImageAdapter? = null
    private var firstFrameInAnim = true
    private var optimise = true

    private var nextImageId: Long = 0

    private var binding: ActivityCreatorBinding? = null

    private val pickLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                if (data?.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        items.add(Triple(data.clipData!!.getItemAt(i).uri, 1000, nextImageId++))
                    }
                    adapter?.notifyDataSetChanged()
                } else if (data?.data != null) {
                    items.add(Triple(data.data!!, 1000, nextImageId++))
                    adapter?.notifyDataSetChanged()
                }
            }
        }

    private val writeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data

                if (data?.data != null) {
                    if (BuildConfig.DEBUG)
                        Log.i(TAG, "Intent data : ${data.data}")

                    val builder = NotificationCompat.Builder(this, CREATION_CHANNEL_ID).apply {
                        setContentTitle(getString(R.string.create_notification_title))
                        setContentText(
                            this@CreatorActivity.resources.getQuantityString(
                                R.plurals.create_notification_description,
                                0,
                                0,
                                items.size
                            )
                        )
                        setSmallIcon(R.drawable.ic_create_white_24dp)
                        priority = NotificationCompat.PRIORITY_LOW
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        val out = contentResolver.openOutputStream(data.data!!) ?: return@launch
                        saveToOutputStream(
                            items.map { Pair(it.first, it.second) },
                            out,
                            builder = builder
                        )
                        out.close()

                        if (binding != null) {
                            withContext(Dispatchers.Main) {
                                Snackbar.make(
                                    binding!!.imageRecyclerView,
                                    R.string.done,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatorBinding.inflate(layoutInflater)

        setContentView(binding?.root)

        binding?.fabAddImage?.setOnClickListener {
            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
            getIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            getIntent.type = "image/*"

            pickLauncher.launch(getIntent)
        }

        adapter = ImageAdapter(this, items, lifecycleScope)
        adapter?.setHasStableIds(true)

        binding?.imageRecyclerView?.layoutManager = LinearLayoutManager(this)
        binding?.imageRecyclerView?.setHasFixedSize(true)
        binding?.imageRecyclerView?.itemAnimator = object : DefaultItemAnimator() {
            override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                return true
            }
        }
        binding?.imageRecyclerView?.setItemViewCacheSize(20)
        if (adapter != null)
            ItemTouchHelper(SwipeToDeleteCallback(adapter!!)).attachToRecyclerView(binding?.imageRecyclerView)

        adapter?.clickListener = { position ->
            DelayInputDialog(object : DelayInputDialog.InputSenderDialogListener {
                override fun onOK(number: Int?) {
                    if (number != null) {
                        items[position] = Triple(
                            items[position].first,
                            number,
                            items[position].third
                        )
                        adapter?.notifyDataSetChanged()
                    }
                }

                override fun onCancel(number: Int?) {}
            }, items[position].second).show(supportFragmentManager, null)
        }

        setSupportActionBar(binding?.creatorBottomAppBar)
        binding?.imageRecyclerView?.adapter = adapter
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = getString(R.string.create_notification_channel_name)
            val descriptionText = getString(R.string.create_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(CREATION_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.creator_menu, menu)
        menu?.findItem(R.id.menu_first_frame_in_anim)?.isChecked = true
        menu?.findItem(R.id.menu_optimise)?.isChecked = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_create_apng -> {
                if (items.size > 0) {
                    val builder = NotificationCompat.Builder(this, CREATION_CHANNEL_ID).apply {
                        setContentTitle(getString(R.string.create_notification_title))
                        setContentText(
                            this@CreatorActivity.resources.getQuantityString(
                                R.plurals.create_notification_description,
                                0,
                                0,
                                items.size
                            )
                        )
                        setSmallIcon(R.drawable.ic_create_white_24dp)
                        priority = NotificationCompat.PRIORITY_LOW
                    }

                    lifecycleScope.launch(Dispatchers.IO) {
                        val randomFileName = UUID.randomUUID().toString()
                        val f = File(filesDir, "images/$randomFileName.png").apply {
                            if (!exists()) {
                                parentFile?.mkdirs()
                                createNewFile()
                            }
                        }
                        val out = FileOutputStream(f)
                        saveToOutputStream(
                            items.map { Pair(it.first, it.second) },
                            out,
                            builder = builder
                        )
                        out.close()

                        if (BuildConfig.DEBUG)
                            Log.v(TAG, "File size is ${f.length() / 1000}kB")

                        withContext(Dispatchers.Main) {
                            Toast
                                .makeText(
                                    this@CreatorActivity, getString(
                                        R.string.file_size_kB,
                                        f.length() / 1000
                                    ), Toast.LENGTH_SHORT
                                )
                                .show()

                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = FileProvider.getUriForFile(
                                this@CreatorActivity,
                                "${BuildConfig.APPLICATION_ID}.provider",
                                f
                            )
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivity(intent)
                        }
                    }
                }
                true
            }
            R.id.menu_share_apng -> {
                if (items.size > 0) {
                    val builder = NotificationCompat.Builder(this, CREATION_CHANNEL_ID).apply {
                        setContentTitle(getString(R.string.create_notification_title))
                        setContentText(
                            this@CreatorActivity.resources.getQuantityString(
                                R.plurals.create_notification_description,
                                0,
                                0,
                                items.size
                            )
                        )
                        setSmallIcon(R.drawable.ic_create_white_24dp)
                        priority = NotificationCompat.PRIORITY_LOW
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        val randomFileName = UUID.randomUUID().toString()
                        val f = File(filesDir, "images/$randomFileName.png").apply {
                            if (!exists()) {
                                parentFile?.mkdirs()
                                println(createNewFile())
                            }
                        }
                        val out = FileOutputStream(f)
                        saveToOutputStream(
                            items.map { Pair(it.first, it.second) },
                            out,
                            builder = builder
                        )
                        out.close()

                        withContext(Dispatchers.Main) {
                            Toast
                                .makeText(
                                    this@CreatorActivity, getString(
                                        R.string.file_size_kB,
                                        f.length() / 1000
                                    ), Toast.LENGTH_SHORT
                                )
                                .show()

                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                val uri = FileProvider.getUriForFile(
                                    this@CreatorActivity,
                                    "${BuildConfig.APPLICATION_ID}.provider",
                                    f
                                )
                                putExtra(
                                    Intent.EXTRA_STREAM, uri
                                )

                                clipData = ClipData.newUri(
                                    contentResolver,
                                    getString(R.string.share),
                                    uri
                                )
                                type = "image/png"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(
                                Intent.createChooser(
                                    intent,
                                    resources.getText(R.string.share)
                                )
                            )
                        }
                    }
                }
                true
            }
            R.id.menu_save_apng -> {
                if (items.size > 0) {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        // Filter to only show results that can be "opened", such as
                        // a file (as opposed to a list of contacts or timezones).
                        addCategory(Intent.CATEGORY_OPENABLE)

                        // Create a file with the requested MIME type.
                        type = "image/png"
                        putExtra(Intent.EXTRA_TITLE, "${items[0].first.lastPathSegment}.png")
                    }

                    writeLauncher.launch(
                        intent
                    )
                }
                true
            }
            R.id.menu_set_all_duration -> {
                if (items.size > 0) {
                    DelayInputDialog(object : DelayInputDialog.InputSenderDialogListener {
                        override fun onCancel(number: Int?) {}

                        override fun onOK(number: Int?) {
                            if (number != null) {
                                for (i in 0 until items.size) {
                                    items[i] = Triple(items[i].first, number, items[i].third)
                                }
                                adapter?.notifyDataSetChanged()
                            }
                        }
                    }).show(supportFragmentManager, null)
                }
                true
            }
            R.id.menu_clear -> {
                items.clear()
                adapter?.notifyDataSetChanged()
                true
            }
            R.id.menu_first_frame_in_anim -> {
                item.isChecked = !item.isChecked
                firstFrameInAnim = item.isChecked
                true
            }
            R.id.menu_optimise -> {
                item.isChecked = !item.isChecked
                optimise = item.isChecked
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private suspend fun saveToOutputStream(
        files: Collection<Pair<Uri, Int>>,
        outputStream: OutputStream,
        builder: NotificationCompat.Builder? = null
    ) {
        var maxWidth = 0
        var maxHeight = 0
        var notificationManagerCompat: NotificationManagerCompat?
        if (builder != null) {
            withContext(Dispatchers.Main) {
                notificationManagerCompat = NotificationManagerCompat.from(this@CreatorActivity)
                builder.setProgress(files.size, 0, false)
                notificationManagerCompat?.notify(1, builder.build())
            }
        }
        files.forEach {
            val str = contentResolver.openInputStream(it.first)
            if (str == null) {
                Log.e(TAG, "Input Stream is null : ${it.first}")
                return@forEach
            }

            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true

            BitmapFactory.decodeStream(str, null, options)
            if (options.outWidth > maxWidth)
                maxWidth = options.outWidth
            if (options.outHeight > maxHeight)
                maxHeight = options.outHeight
            str.close()
        }

        if (BuildConfig.DEBUG)
            Log.d(TAG, "MaxWidth : $maxWidth; MaxHeight : $maxHeight")

        val encoder = ApngEncoder(
            outputStream,
            maxWidth,
            maxHeight,
            items.size
        ).setCompressionLevel(9)
            .setIsFirstFrameInAnim(firstFrameInAnim)
            .setOptimiseApng(optimise)

        items.forEachIndexed { i, uri ->
            if (builder != null) {
                withContext(Dispatchers.Main) {
                    notificationManagerCompat = NotificationManagerCompat.from(this@CreatorActivity)
                    builder.setProgress(files.size, i + 1, false)
                        .setContentText(
                            this@CreatorActivity.resources.getQuantityString(
                                R.plurals.create_notification_description,
                                i + 1,
                                i + 1,
                                files.size
                            )
                        )
                    notificationManagerCompat?.notify(1, builder.build())
                }
            }
            if (BuildConfig.DEBUG)
                Log.v(TAG, "Encoding frame $i")

            try {
                val str = contentResolver.openInputStream(uri.first)
                    ?: return@forEachIndexed
                if (i == 0) {
                    val btm =
                        BitmapFactory.decodeStream(str)
                    if (btm != null) {
                        encoder.writeFrame(
                            if (btm.width != maxWidth && btm.height != maxHeight)
                                Bitmap.createScaledBitmap(
                                    btm,
                                    maxWidth,
                                    maxHeight,
                                    false
                                )
                            else
                                btm,
                            delay = uri.second.toFloat(),
                            disposeOp = Utils.Companion.DisposeOp.APNG_DISPOSE_OP_NONE
                        )
                    }
                } else {
                    encoder.writeFrame(
                        str,
                        delay = uri.second.toFloat(),
                    )
                }
                str.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error when creating apng", e)
            }
        }

        withContext(Dispatchers.IO) {
            encoder.writeEnd()
        }


        if (builder != null) {
            withContext(Dispatchers.Main) {
                notificationManagerCompat = NotificationManagerCompat.from(this@CreatorActivity)
                builder.setProgress(0, 0, false)
                    .setContentText(getString(R.string.create_notification_description_end))
                notificationManagerCompat?.notify(1, builder.build())
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        lifecycleScope.launch(Dispatchers.IO) {
            val deleteResult = File(filesDir, "images").deleteRecursively()
            if (BuildConfig.DEBUG)
                Log.v(TAG, "Deleted images dir : $deleteResult")
        }
    }

    inner class SwipeToDeleteCallback(private val adapter: ImageAdapter) :
        ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPos = viewHolder.adapterPosition
            val toPos = target.adapterPosition

            Collections.swap(items, fromPos, toPos)

            adapter.notifyItemMoved(fromPos, toPos)
            adapter.notifyItemChanged(fromPos)
            adapter.notifyItemChanged(toPos)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            items.removeAt(position)
            adapter.notifyDataSetChanged()
        }

        override fun isItemViewSwipeEnabled() = true
    }
}