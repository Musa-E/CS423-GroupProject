//AI STATEMENT: AI was not used in this. This file was imported from https://github.com/MyScript/interactive-ink-examples-android
//into this Android SDK and then edited by our team

package com.myscript.iink.demo.util

import android.view.View
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat

fun View.setTooltipText(@StringRes textRes: Int) {
    TooltipCompat.setTooltipText(this, context.getString(textRes))
}

fun ImageView.setContentDescription(@StringRes textRes: Int) {
    contentDescription = context.getString(textRes)
}