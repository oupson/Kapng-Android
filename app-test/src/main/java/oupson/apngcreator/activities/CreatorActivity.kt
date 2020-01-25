package oupson.apngcreator.activities

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_creator.*
import oupson.apng.Apng
import oupson.apngcreator.BuildConfig
import oupson.apngcreator.R
import oupson.apngcreator.adapter.ImageAdapter
import java.io.File

class CreatorActivity : AppCompatActivity() {
    companion object {
        private const val PICK_IMAGE = 999
    }
    private var items : ArrayList<Uri> = ArrayList()
    private var adapter : ImageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_creator)

        fabAddImage.setOnClickListener {
            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
            getIntent.type = "image/*"

            val pickIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickIntent.type = "image/*"

            val chooserIntent = Intent.createChooser(getIntent, "Select Image")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))

            startActivityForResult(chooserIntent,
                PICK_IMAGE
            )
        }

        /*        frameListViewAdapter(this, items) */
        adapter = ImageAdapter(this, items)

        imageRecyclerView.layoutManager = LinearLayoutManager(this)
        imageRecyclerView.setHasFixedSize(true)
        imageRecyclerView.setItemViewCacheSize(20)
        if (adapter != null)
            ItemTouchHelper(SwipeToDeleteCallback(adapter!!)).attachToRecyclerView(imageRecyclerView)

        setSupportActionBar(creatorBottomAppBar)
        imageRecyclerView.adapter = adapter
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.creator_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_create_apng -> {
                // TODO
                val apngCreated = Apng()

                items.forEachIndexed { i, uri ->
                    println("delay : ${adapter?.delay?.get(i)?.toFloat() ?: 1000f}ms")
                    val str = contentResolver.openInputStream(uri)
                    apngCreated.addFrames(BitmapFactory.decodeStream(str), delay = adapter?.delay?.get(i)?.toFloat() ?: 1000f)
                    str?.close()
                }

                File(cacheDir, "apn0.png").writeBytes(apngCreated.toByteArray())

                apngCreated.apply {
                    // TODO
                    //if (view.optimiseCheckBox.isChecked)
                    //    apngCreated.optimiseFrame()
                }

                // TODO Open
                val f = File(filesDir, "images/apng.png").apply {
                    if (!exists()) {
                        parentFile.mkdirs()
                        println(createNewFile())
                    }
                    writeBytes(apngCreated.toByteArray())
                }


                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", f)
                startActivity(intent)

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            PICK_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.data != null) {
                        items.add(data.data!!)
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    inner class SwipeToDeleteCallback(private val adapter: ImageAdapter) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ) : Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            adapter.delay.removeAt(position)
            items.removeAt(position)
            adapter.notifyDataSetChanged()
            adapter.listeners.forEachIndexed { index, listener ->
                if (index >= position)
                    listener.position = index
            }
        }

        override fun isItemViewSwipeEnabled() = true
    }
}

