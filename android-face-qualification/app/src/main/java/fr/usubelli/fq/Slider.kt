package fr.usubelli.fq

import android.media.Image
import android.provider.MediaStore
import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.os.Parcelable
import android.content.Context


class ImageModel {

    var image_drawable: Int = 0
}

class Slider(private val context: Context, private val imageModelArrayList: ArrayList<ImageModel>) : PagerAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        return imageModelArrayList.size
    }

    override fun instantiateItem(view: ViewGroup, position: Int): Any {
        val imageLayout = inflater.inflate(R.layout.slider, view, false)!!

        val imageView = imageLayout.findViewById<ImageView>(R.id.image)

        imageView.setImageResource(imageModelArrayList[position].image_drawable)

        view.addView(imageLayout, 0)

        return imageLayout
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {}

    override fun saveState(): Parcelable? {
        return null
    }

}