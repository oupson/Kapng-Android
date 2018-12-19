package oupson.apngcreator.adapter

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import oupson.apngcreator.R

class frameListViewAdapter (context: Context, val bitmaps: List<Bitmap>) : ArrayAdapter<Bitmap>(context, 0, bitmaps) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =  LayoutInflater.from(context).inflate(R.layout.framelistviewadapterlayout, parent, false)

        val imageView = view.findViewById<ImageView>(R.id.frameAdapterImageView)
        imageView.setImageBitmap(bitmaps[position])
        return view
    }
}