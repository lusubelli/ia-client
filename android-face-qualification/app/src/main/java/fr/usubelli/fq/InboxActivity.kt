package fr.usubelli.fq

import android.os.Bundle
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View


class InboxActivity: AppCompatActivity() {

    private var imageModelArrayList: List<ImageModel> = mutableListOf()

    private val myImageList = intArrayOf(
        R.drawable.cn1,
        R.drawable.cn2,
        R.drawable.cn3,
        R.drawable.cn4,
        R.drawable.cn5
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContentView(R.layout.inbox)

        imageModelArrayList = ArrayList()
        imageModelArrayList = populateList()

        init()

    }

    private fun populateList(): List<ImageModel> {

        val list = mutableListOf<ImageModel>()

        for (i in 0 until myImageList.size) {
            val imageModel = ImageModel()
            imageModel.image_drawable = myImageList[i]
            list.add(imageModel)
        }

        return list
    }

    private fun init() {
        val mPager: ViewPager = findViewById(R.id.pager)
        mPager.adapter = Slider(this@InboxActivity, ArrayList(imageModelArrayList))
    }
    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}