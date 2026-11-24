package com.stiki.marcelloilham.guppymarketidn.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.stiki.marcelloilham.guppymarketidn.R

class Community : Fragment() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_community, container, false)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshHub)

        // Pengaturan SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(R.color.guppy_primary)
        swipeRefreshLayout.setOnRefreshListener {
            val content = swipeRefreshLayout.getChildAt(0)
            content?.animate()?.alpha(0.6f)?.setDuration(200)?.start()

            Handler(Looper.getMainLooper()).postDelayed({
                if (isAdded) {
                    swipeRefreshLayout.isRefreshing = false
                    content?.translationY = 30f
                    content?.animate()
                        ?.alpha(1.0f)
                        ?.translationY(0f)
                }
            }, 1000)
        }

        return view
    }
}
