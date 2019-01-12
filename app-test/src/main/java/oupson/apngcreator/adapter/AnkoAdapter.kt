package oupson.apngcreator.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

class AnkoAdapter<T>(itemFactory: () -> List<T>, viewFactory: Context.(index: Int, items: List<T>, view: View?) -> View): BaseAdapter() {
    val viewFactory = viewFactory
    val items: List<T> by lazy { itemFactory() }

    override fun getView(index: Int, view: View?, viewGroup: ViewGroup?): View {
        return viewGroup!!.context.viewFactory(index, items, view)
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(index: Int): T {
        return items.get(index)
    }

    override fun getItemId(index: Int): Long {

        return (items.get(index) as Any).hashCode().toLong() + (index.toLong() * Int.MAX_VALUE)
    }
}