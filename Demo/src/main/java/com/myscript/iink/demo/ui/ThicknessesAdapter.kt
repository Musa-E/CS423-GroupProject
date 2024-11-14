//AI STATEMENT: AI was not used in this. This file was imported from https://github.com/MyScript/interactive-ink-examples-android
//into this Android SDK and then edited by our team

package com.myscript.iink.demo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.myscript.iink.demo.R

private fun ThicknessState.toScale(): Float = when (this.thickness) {
    Thickness.THIN -> .25f
    Thickness.MEDIUM -> .5f
    Thickness.LARGE -> 1f
}
