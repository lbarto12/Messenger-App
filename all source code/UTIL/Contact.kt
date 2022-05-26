package com.example.messenger.UTIL

import android.graphics.Bitmap

data class Contact(
    val name: String,
    val number: String,
    val id: String,
    var photo: Bitmap? = null,
    val email: String? = null,
    val friendcode: String? = null
)
