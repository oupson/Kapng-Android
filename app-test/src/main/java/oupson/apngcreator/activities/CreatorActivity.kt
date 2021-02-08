package oupson.apngcreator.activities

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_creator.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import oupson.apng.encoder.ApngEncoder
import oupson.apng.utils.Utils
import oupson.apngcreator.BuildConfig
import oupson.apngcreator.R
import oupson.apngcreator.adapter.ImageAdapter
import oupson.apngcreator.dialogs.DelayInputDialog
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList

class CreatorActivity : AppCompatActivity() {
    companion object {
        private const val PICK_IMAGE = 1
        private const val WRITE_REQUEST_CODE = 2
        private const val TAG = "CreatorActivity"
    }

    private var items: ArrayList<Triple<Uri, Int, Long>> = ArrayList()
    private var adapter: ImageAdapter? = null
    private var firstFrameInAnim = true
    private var optimise = true

    private var nextImageId : Long= 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_creator)

        fabAddImage.setOnClickListener {
            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
            getIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            getIntent.type = "image/*"

            startActivityForResult(
                getIntent,
                PICK_IMAGE
            )
        }

        adapter = ImageAdapter(this, items)
        adapter?.setHasStableIds(true)

        imageRecyclerView.layoutManager = LinearLayoutManager(this)
        imageRecyclerView.setHasFixedSize(true)
        imageRecyclerView.itemAnimator = object : DefaultItemAnimator() {
            override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                return true
            }
        }
        imageRecyclerView.setItemViewCacheSize(20)
        if (adapter != null)
            ItemTouchHelper(SwipeToDeleteCallback(adapter!!)).attachToRecyclerView(imageRecyclerView)

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

        setSupportActionBar(creatorBottomAppBar)
        imageRecyclerView.adapter = adapter
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.creator_menu, menu)
        menu?.findItem(R.id.menu_first_frame_in_anim)?.isChecked = true
        menu?.findItem(R.id.menu_optimise)?.isChecked = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_create_apng -> {
                if (items.size > 0) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val randomFileName = UUID.randomUUID().toString()
                        val f = File(filesDir, "images/$randomFileName.png").apply {
                            if (!exists()) {
                                parentFile?.mkdirs()
                                createNewFile()
                            }
                        }
                        val out = FileOutputStream(f)
                        saveToOutputStream(out)
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
                    GlobalScope.launch(Dispatchers.IO) {
                        val randomFileName = UUID.randomUUID().toString()
                        val f = File(filesDir, "images/$randomFileName.png").apply {
                            if (!exists()) {
                                parentFile?.mkdirs()
                                println(createNewFile())
                            }
                        }
                        val out = FileOutputStream(f)
                        saveToOutputStream(out)
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
                    startActivityForResult(intent, WRITE_REQUEST_CODE)
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
            else -> if (item != null) super.onOptionsItemSelected(item) else true
        }
    }

    private fun saveToOutputStream(outputStream: OutputStream) {
        var maxWidth = 0
        var maxHeight = 0
        items.forEach {
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

        encoder.writeEnd()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
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
            WRITE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.data != null) {
                        if (BuildConfig.DEBUG)
                            Log.i(TAG, "Intent data : ${data.data}")

                        GlobalScope.launch(Dispatchers.IO) {
                            val out = contentResolver.openOutputStream(data.data!!) ?: return@launch
                            saveToOutputStream(out)
                            out.close()

                            withContext(Dispatchers.Main) {
                                Snackbar.make(
                                    imageRecyclerView,
                                    R.string.done,
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

        val deleteResult = File(filesDir, "images").deleteRecursively()
        if (BuildConfig.DEBUG)
            Log.v(TAG, "Deleted images dir : $deleteResult")
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