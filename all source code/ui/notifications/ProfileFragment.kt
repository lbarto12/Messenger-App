package com.example.messenger.ui.notifications

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.messenger.MainActivity
import com.example.messenger.R
import com.example.messenger.Tools.BitmapTools
import com.example.messenger.Tools.FirestoreTools
import com.example.messenger.databinding.FragmentNotificationsBinding
import com.example.messenger.ui.home.HomeFragment
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_notifications.*
import kotlinx.android.synthetic.main.text_cell_from_user.*

class ProfileFragment : Fragment(R.layout.fragment_notifications) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.println(Log.DEBUG, "HOME FRAGMENT", "onCreate()")

        ProfileFragment.self = this
        if (MainActivity.self != null) main()
    }

    fun main(){
        my_friend_code.text = MainActivity.myFriendCode
        my_name.text = MainActivity.me!!.name
        if (MainActivity.me?.photo != null){
            my_profile_pic.setImageBitmap(
                BitmapTools.getCroppedBitmap(
                    MainActivity.me!!.photo!!
                )
            )
        }
        change_photo_tv.setOnClickListener {
            val intent2 = Intent()
            intent2.action = Intent.ACTION_GET_CONTENT
            intent2.type = "image/*"
            startActivityForResult(Intent.createChooser(intent2, "Select Image"), NEW_PHOTO_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NEW_PHOTO_CODE && resultCode == RESULT_OK && data != null && data.data != null){
            val fileUri = data.data
            val storage = FirebaseStorage.getInstance().reference.child("Image Files")
            val id = MainActivity.me!!.id

            val filepath = storage.child(id)
            val uploadTask = fileUri?.let { filepath.putFile(it) }
            uploadTask!!.continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!

                return@continueWithTask filepath.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful){
                    val downloadUri = (task.result as Uri).toString()
                    val image = MediaStore.Images.Media.getBitmap(MainActivity.self!!.contentResolver, fileUri)
                    MainActivity.me!!.photo = image
                    my_profile_pic.setImageBitmap(
                        BitmapTools.getCroppedBitmap(image)
                    )
                }
            }

        }

    }

    val NEW_PHOTO_CODE = 256

    companion object {
        var self: ProfileFragment? = null
    }
}