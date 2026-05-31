package com.mohammed.pdfreader.ui.bookmarks

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mohammed.pdfreader.R

class BookmarksFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = TextView(context).apply {
        text = "🔖 الإشارات المرجعية\nقريباً..."
        textSize = 24f
        setTextColor(resources.getColor(R.color.text_primary, null))
        gravity = Gravity.CENTER
    }
}
