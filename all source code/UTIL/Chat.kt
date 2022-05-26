package com.example.messenger.UTIL

import android.graphics.Bitmap

data class Chat(
    val contact: Contact,
    val messages: ArrayList<Text>,
    var hasUnread: Boolean = false
)
