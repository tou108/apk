package com.mh4g.simulator

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.mh4g.simulator.data.AppData
import com.mh4g.simulator.ui.fragments.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var loadingBar: ProgressBar
    private lateinit var loadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        loadingBar = findViewById(R.id.loadingBar)
        loadingText = findViewById(R.id.loadingText)

        // データ非同期ロード
        loadingBar.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        viewPager.visibility = View.GONE
        tabLayout.visibility = View.GONE

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppData.initialize(applicationContext)
            }
            loadingBar.visibility = View.GONE
            loadingText.visibility = View.GONE
            viewPager.visibility = View.VISIBLE
            tabLayout.visibility = View.VISIBLE
            setupTabs()
        }
    }

    private fun setupTabs() {
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = adapter.itemCount

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = adapter.getTitle(position)
        }.attach()
    }
}

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val tabs = listOf(
        "検索" to { SearchFragment() as Fragment },
        "頭除外" to { EquipExcludeFragment.newInstance(com.mh4g.simulator.data.SLOT_HEAD) as Fragment },
        "胴除外" to { EquipExcludeFragment.newInstance(com.mh4g.simulator.data.SLOT_BODY) as Fragment },
        "腕除外" to { EquipExcludeFragment.newInstance(com.mh4g.simulator.data.SLOT_ARM)  as Fragment },
        "腰除外" to { EquipExcludeFragment.newInstance(com.mh4g.simulator.data.SLOT_WST)  as Fragment },
        "脚除外" to { EquipExcludeFragment.newInstance(com.mh4g.simulator.data.SLOT_LEG)  as Fragment },
        "装飾品" to { DecoExcludeFragment() as Fragment },
        "お守り" to { CharmFragment() as Fragment },
        "マイセット" to { MySetFragment() as Fragment }
    )

    override fun getItemCount() = tabs.size
    override fun createFragment(position: Int) = tabs[position].second()
    fun getTitle(position: Int) = tabs[position].first
}
