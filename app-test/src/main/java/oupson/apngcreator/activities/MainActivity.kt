package oupson.apngcreator.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.http.HttpResponseCache
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.android.material.bottomappbar.BottomAppBarTopEdgeTreatment
import com.google.android.material.shape.CutCornerTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.ShapePath
import kotlinx.android.synthetic.main.activity_main.*
import oupson.apngcreator.BuildConfig
import oupson.apngcreator.R
import oupson.apngcreator.fragments.ApngDecoderFragment
import oupson.apngcreator.fragments.JavaFragment
import oupson.apngcreator.fragments.KotlinFragment
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG)
            Log.v(TAG, "supportFragmentManager.fragments.size : ${supportFragmentManager.fragments.size}")

        setContentView(R.layout.activity_main)

        setSupportActionBar(bottomAppBar)

        setUpBottomAppBarShapeAppearance()

        val httpCacheSize = 10 * 1024 * 1024.toLong() // 10 MiB

        val httpCacheDir = File(cacheDir, "http")
        HttpResponseCache.install(httpCacheDir, httpCacheSize)

        fabCreate.setOnClickListener {
            startActivity(Intent(this, CreatorActivity::class.java))
        }

        val drawerToggle = ActionBarDrawerToggle(this, drawer_layout, bottomAppBar,
            R.string.open,
            R.string.close
        )
        drawer_layout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        var selected = 0

        navigationView.setNavigationItemSelectedListener { menuItem : MenuItem ->
            when(menuItem.itemId) {
                R.id.menu_kotlin_fragment -> {
                    if (selected != 0) {
                        supportFragmentManager.beginTransaction().apply {
                            replace(
                                R.id.fragment_container,
                                KotlinFragment.newInstance()
                            )
                            addToBackStack(null)
                        }.commit()
                        selected = 0
                    }
                }
                R.id.menu_java_fragment -> {
                    if (selected != 1) {
                        supportFragmentManager.beginTransaction().apply {
                            replace(
                                R.id.fragment_container,
                                JavaFragment()
                            )
                            addToBackStack(null)
                        }.commit()
                        selected = 1
                    }
                }
                R.id.menu_apng_decoder_fragment -> {
                    if (selected != 2) {
                        supportFragmentManager.beginTransaction().apply {
                            replace(
                                R.id.fragment_container,
                                ApngDecoderFragment.newInstance()
                            )
                            addToBackStack(null)
                        }.commit()
                        selected = 2
                    }
                }
            }

            drawer_layout.closeDrawer(GravityCompat.START)

            return@setNavigationItemSelectedListener true
        }

        if (intent.hasExtra("fragment") && supportFragmentManager.fragments.size == 0) {
            when(intent.getStringExtra("fragment")) {
                "kotlin" -> {
                    supportFragmentManager.beginTransaction().apply {
                        add(
                            R.id.fragment_container,
                            KotlinFragment.newInstance(), "KotlinFragment")
                    }.commit()
                    navigationView.setCheckedItem(R.id.menu_kotlin_fragment)
                    selected = 0
                }
                "java" -> {
                    supportFragmentManager.beginTransaction().apply {
                        add(
                            R.id.fragment_container,
                            JavaFragment()
                        )
                    }.commit()
                    navigationView.setCheckedItem(R.id.menu_java_fragment)
                    selected = 1
                }
                "apng_decoder" -> {
                    supportFragmentManager.beginTransaction().apply {
                        add(
                            R.id.fragment_container,
                            ApngDecoderFragment.newInstance()
                        )
                    }.commit()
                    navigationView.setCheckedItem(R.id.menu_apng_decoder_fragment)
                    selected = 2
                }
            }
        } else if (supportFragmentManager.fragments.size == 0) {
            supportFragmentManager.beginTransaction().apply {
                add(
                    R.id.fragment_container,
                    KotlinFragment.newInstance(), "KotlinFragment")
            }.commit()
            navigationView.setCheckedItem(R.id.menu_kotlin_fragment)
        }
    }

    override fun onStop() {
        super.onStop()
        HttpResponseCache.getInstalled()?.flush()
    }

    private fun setUpBottomAppBarShapeAppearance() {
        val fabShapeAppearanceModel: ShapeAppearanceModel = fabCreate.shapeAppearanceModel
        val cutCornersFab =
            (fabShapeAppearanceModel.bottomLeftCorner is CutCornerTreatment
                    && fabShapeAppearanceModel.bottomRightCorner is CutCornerTreatment)
        val topEdge =
            if (cutCornersFab) BottomAppBarCutCornersTopEdge(
                bottomAppBar.fabCradleMargin,
                bottomAppBar.fabCradleRoundedCornerRadius,
                bottomAppBar.cradleVerticalOffset
            ) else BottomAppBarTopEdgeTreatment(
                bottomAppBar.fabCradleMargin,
                bottomAppBar.fabCradleRoundedCornerRadius,
                bottomAppBar.cradleVerticalOffset
            )
        val babBackground = bottomAppBar.background as MaterialShapeDrawable
        babBackground.shapeAppearanceModel =
            babBackground.shapeAppearanceModel.toBuilder().setTopEdge(topEdge).build()
    }


    inner class BottomAppBarCutCornersTopEdge(
        private val fabMargin: Float,
        roundedCornerRadius: Float,
        private val cradleVerticalOffset: Float
    ) :
        BottomAppBarTopEdgeTreatment(fabMargin, roundedCornerRadius, cradleVerticalOffset) {
        @SuppressLint("RestrictedApi")
        override fun getEdgePath(
            length: Float,
            center: Float,
            interpolation: Float,
            shapePath: ShapePath
        ) {
            val fabDiameter = fabDiameter
            if (fabDiameter == 0f) {
                shapePath.lineTo(length, 0f)
                return
            }
            val diamondSize = fabDiameter / 2f
            val middle = center + horizontalOffset
            val verticalOffsetRatio = cradleVerticalOffset / diamondSize
            if (verticalOffsetRatio >= 1.0f) {
                shapePath.lineTo(length, 0f)
                return
            }
            shapePath.lineTo(middle - (fabMargin + diamondSize - cradleVerticalOffset), 0f)
            shapePath.lineTo(
                middle,
                (diamondSize - cradleVerticalOffset + fabMargin) * interpolation
            )
            shapePath.lineTo(middle + (fabMargin + diamondSize - cradleVerticalOffset), 0f)
            shapePath.lineTo(length, 0f)
        }

    }
}
