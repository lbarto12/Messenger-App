package com.example.messenger

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messenger.UTIL.Text
import com.example.messenger.UTIL.TextAdapter
import kotlinx.android.synthetic.main.activity_messenger.*
import com.example.messenger.Tools.BitmapTools

import android.provider.MediaStore
import com.example.messenger.Tools.FirestoreTools
import com.example.messenger.UTIL.Chat
import com.example.messenger.UTIL.Contact
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage


class ChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger)

        val contact = intent.extras?.get("contact")
        MainActivity.currentTextAdapter = textAdapter
        self = this
        main()
    }



    private fun main(){
        chat_recycler.adapter = textAdapter
        chat_recycler.layoutManager = LinearLayoutManager(this)
        chat_recycler.layoutManager

        initDisplay()
        initChatBar()
        setUpChat()
    }

    private fun initDisplay(){
        val contact = MainActivity.currentContact
        if (contact != null){

            if (contact.photo != null)
                contact_image_button_messenger.setImageBitmap(
                    BitmapTools.getCroppedBitmap(contact.photo!!)
                )

            contact_name_message_view.text = contact.name
        }

        back_button_messenger.setOnClickListener { finish() }
    }

    fun scrollRecyclerViewToBottom() {
        if (textAdapter.itemCount > 0) {
            chat_recycler.scrollToPosition(textAdapter.itemCount - 1)
        }
    }

    private fun initChatBar(){

        chat_recycler.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            chat_recycler.getWindowVisibleDisplayFrame(r)
            val screenheight = chat_recycler.rootView.height
            val keypadheight = screenheight - r.bottom
            if (keypadheight > screenheight * 0.15){
                scrollRecyclerViewToBottom()
            }
        }
        scrollRecyclerViewToBottom()

        send_button.setOnClickListener {
            if (MainActivity.currentContact != null){
                if (chat_type_bar.text.toString().isEmpty()) return@setOnClickListener
                val content = chat_type_bar.text.toString()
                val text = Text(
                    content,
                    true,
                    Timestamp.now()
                )
                textAdapter.addText(text)
                if (MainActivity.me != null){
                    FirestoreTools.uploadText(MainActivity.me!!, MainActivity.currentContact!!, content)
                    chat_type_bar.text.clear()
                }

                if (MainActivity.currentChat != null){
                    MainActivity.currentChat!!.messages.add(Text(
                        content,
                        true,
                        Timestamp.now()
                        ))
                }
            }



        }

        send_photo_button.setOnClickListener {
            val intent2 = Intent()
            intent2.action = Intent.ACTION_GET_CONTENT
            intent2.type = "image/*"
            startActivityForResult(Intent.createChooser(intent2, "Select Image"), SEND_PHOTO_CODE)
        }
    }

    private fun setUpChat(){
        for (i in MainActivity.currentChat!!.messages){
            textAdapter.addText(i)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SEND_PHOTO_CODE &&
            resultCode == RESULT_OK &&
            data != null &&
            data.data != null){
            val fileUri = data.data
            val storage = FirebaseStorage.getInstance().reference.child("Image Files")


            FirestoreTools.withTextStoreId{ currentTextId ->
                val filepath = storage.child("$currentTextId.jpg")

                val uploadTask = fileUri?.let { filepath.putFile(it) }
                uploadTask!!.continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception!!

                    return@continueWithTask filepath.downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        val downloadUri = (task.result as Uri).toString()
                        val resultingText = Text(
                            "image",
                            true,
                            Timestamp.now(),
                            MediaStore.Images.Media.getBitmap(contentResolver, fileUri),
                            "$currentTextId.jpg"
                        )
                        textAdapter.addText(resultingText)
                        FirestoreTools.uploadText(MainActivity.me!!,
                            MainActivity.currentContact!!,
                            resultingText)


                    }
                }
            }


        }
    }


    override fun onDestroy() {
        super.onDestroy()
        MainActivity.currentTextAdapter = null
        MainActivity.currentChat = null
    }


    companion object{
        var self: ChatActivity? = null
        var shouldScrollToBottom = false

        fun open(contact: Contact, chat: Chat){
            MainActivity.currentContact = contact
            MainActivity.currentChat = chat
            val intent = Intent(MainActivity.self, ChatActivity::class.java)
            MainActivity.self?.startActivity(intent)
        }
    }

    private var textAdapter = TextAdapter(mutableListOf())
    private val SEND_PHOTO_CODE = 40
}

