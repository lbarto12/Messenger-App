package com.example.messenger.UTIL

import android.graphics.Bitmap
import com.google.firebase.Timestamp

data class Text(
    val content: String,
    val sentByMe: Boolean,
    val time: Timestamp,
    var photo: Bitmap? = null,
    val reference: String? = null
)
