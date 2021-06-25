package oupson.apngcreator.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.http.HttpResponseCache
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomappbar.BottomAppBarTopEdgeTreatment
import com.google.android.material.shape.CutCornerTreatment
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.ShapePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import oupson.apngcreator.BuildConfig
import oupson.apngcreator.R
import oupson.apngcreator.databinding.ActivityMainBinding
import oupson.apngcreator.fragments.ApngDecoderFragment
import oupson.apngcreator.fragments.JavaFragment
import oupson.apngcreator.fragments.KotlinFragment
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()

                    .build()
            )
        }
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG)
            Log.v(
                TAG,
                "supportFragmentManager.fragments.size : ${supportFragmentManager.fragments.size}"
            )

        binding = ActivityMainBinding.inflate(layoutInflater)


        setContentView(binding?.root)

        setSupportActionBar(binding?.bottomAppBar)

        setUpBottomAppBarShapeAppearance()

        val httpCacheSize = 10 * 1024 * 1024.toLong() // 10 MiB

        lifecycleScope.launch(Dispatchers.IO) {
            val httpCacheDir = File(cacheDir, "http")
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
        }

        binding?.fabCreate?.setOnClickListener {
            startActivity(Intent(this, CreatorActivity::class.java))
        }

        val drawerToggle = ActionBarDrawerToggle(
            this, binding?.drawerLayout, binding?.bottomAppBar,
            R.string.open,
            R.string.close
        )
        binding?.drawerLayout?.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        var selected = 0

        binding?.navigationView?.setNavigationItemSelectedListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
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

            binding?.drawerLayout?.closeDrawer(GravityCompat.START)

            return@setNavigationItemSelectedListener true
        }

        if (intent.hasExtra("fragment") && supportFragmentManager.fragments.size == 0) {
            when (intent.getStringExtra("fragment")) {
                "kotlin" -> {
                    supportFragmentManager.beginTransaction().apply {
                        add(
                            R.id.fragment_container,
                            KotlinFragment.newInstance(), "KotlinFragment"
                        )
                    }.commit()
                    binding?.navigationView?.setCheckedItem(R.id.menu_kotlin_fragment)
                    selected = 0
                }
                "java" -> {
                    supportFragmentManager.beginTransaction().apply {
                        add(
                            R.id.fragment_container,
                            JavaFragment()
                        )
                    }.commit()
                    binding?.navigationView?.setCheckedItem(R.id.menu_java_fragment)
                    selected = 1
                }
                "apng_decoder" -> {
                    supportFragmentManager.beginTransaction().apply {
                        add(
                            R.id.fragment_container,
                            ApngDecoderFragment.newInstance()
                        )
                    }.commit()
                    binding?.navigationView?.setCheckedItem(R.id.menu_apng_decoder_fragment)
                    selected = 2
                }
            }
        } else if (supportFragmentManager.fragments.size == 0) {
            supportFragmentManager.beginTransaction().apply {
                add(
                    R.id.fragment_container,
                    KotlinFragment.newInstance(), "KotlinFragment"
                )
            }.commit()
            binding?.navigationView?.setCheckedItem(R.id.menu_kotlin_fragment)
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch(Dispatchers.IO) {
            HttpResponseCache.getInstalled()?.flush()
        }
    }

    private fun setUpBottomAppBarShapeAppearance() {
        if (binding != null) {
            val fabShapeAppearanceModel: ShapeAppearanceModel =
                binding!!.fabCreate.shapeAppearanceModel
            val cutCornersFab =
                (fabShapeAppearanceModel.bottomLeftCorner is CutCornerTreatment
                        && fabShapeAppearanceModel.bottomRightCorner is CutCornerTreatment)
            val topEdge =
                if (cutCornersFab) BottomAppBarCutCornersTopEdge(
                    binding!!.bottomAppBar.fabCradleMargin,
                    binding!!.bottomAppBar.fabCradleRoundedCornerRadius,
                    binding!!.bottomAppBar.cradleVerticalOffset
                ) else BottomAppBarTopEdgeTreatment(
                    binding!!.bottomAppBar.fabCradleMargin,
                    binding!!.bottomAppBar.fabCradleRoundedCornerRadius,
                    binding!!.bottomAppBar.cradleVerticalOffset
                )
            val babBackground = binding!!.bottomAppBar.background as MaterialShapeDrawable
            babBackground.shapeAppearanceModel =
                babBackground.shapeAppearanceModel.toBuilder().setTopEdge(topEdge).build()
        }
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
