package oupson.apngcreator.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import oupson.apngcreator.R

class ImageAdapter(private val context : Context, private val list : List<Uri>) : RecyclerView.Adapter<ImageAdapter.ImageHolder>() {
    val delay : ArrayList<String> = arrayListOf()
    val listeners : ArrayList<Listener> = arrayListOf()
    inner class Listener : TextWatcher {
        var position : Int = -1
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (position > -1)
                delay[position] = s?.toString() ?: ""
        }
    }

    inner class ImageHolder(view : View) : RecyclerView.ViewHolder(view) {
        val imageView = view.findViewById<ImageView?>(R.id.listImageView)
        val textDelay = view.findViewById<TextInputEditText?>(R.id.textDelay)
        val listener = Listener()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ImageHolder(inflater.inflate(R.layout.list_image, parent, false))
    }

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        if (delay.size <= position)
            delay.add("1000")
        holder.textDelay?.addTextChangedListener(holder.listener.also{
            it.position = position
            listeners.add(position, it)
        })
        GlobalScope.launch(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(list[position])
            val btm = BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            })
            inputStream?.close()
            withContext(Dispatchers.Main) {
                holder.imageView?.setImageBitmap(btm)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ImageHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.textDelay?.removeTextChangedListener(holder.listener)
        listeners.remove(holder.listener)
    }

    override fun getItemCount(): Int = list.count()

}