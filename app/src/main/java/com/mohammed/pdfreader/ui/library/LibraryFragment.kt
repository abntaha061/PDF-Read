package com.mohammed.pdfreader.ui.library

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mohammed.pdfreader.R

class LibraryFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = TextView(context).apply {
        text = "📚 المكتبة\nقريباً..."
        textSize = 24f
        setTextColor(resources.getColor(R.color.text_primary, null))
        gravity = Gravity.CENTER
    }
}
