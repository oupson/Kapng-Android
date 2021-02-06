package oupson.apngcreator.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import oupson.apngcreator.R

class ImageAdapter(private val context : Context, private val list : List<Triple<Uri, Int, Long>>) : RecyclerView.Adapter<ImageAdapter.ImageHolder>() {
    inner class ImageHolder(view : View) : RecyclerView.ViewHolder(view) {
        val imageView : ImageView? = view.findViewById(R.id.listImageView)
        val textDelay : TextView? = view.findViewById(R.id.textDelay)
        val positionTextView : TextView? = view.findViewById(R.id.position_textView)
        val nameTextView : TextView? = view.findViewById(R.id.name_textView)
    }

    var clickListener : ((position : Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ImageHolder(inflater.inflate(R.layout.list_image, parent, false))
    }

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        holder.itemView.setOnClickListener { clickListener?.invoke(position) }
        holder.textDelay?.text = String.format("%dms", list[position].second)
        holder.positionTextView?.text = String.format("# %03d", holder.adapterPosition + 1)
        holder.nameTextView?.text = list[position].first.path?.substringAfterLast("/")
        GlobalScope.launch(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(list[position].first)
            val btm =
                BitmapFactory.decodeStream(inputStream, null, BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                })
            inputStream?.close()
            withContext(Dispatchers.Main) {
                holder.imageView?.setImageBitmap(btm)
            }
        }
    }

    override fun getItemCount(): Int = list.count()

    override fun getItemId(position: Int): Long = list[position].third
}