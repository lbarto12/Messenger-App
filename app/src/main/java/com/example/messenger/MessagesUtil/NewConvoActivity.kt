package com.example.messenger.MessagesUtil

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.messenger.ChatActivity
import com.example.messenger.MainActivity
import com.example.messenger.R
import com.example.messenger.UTIL.Chat
import com.example.messenger.UTIL.Contact
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_new_convo.*

class NewConvoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_convo)

        friendCodeNewConvo.hint = "Friend Code"

        returnFromNewConvo.setOnClickListener { finish() }

        submitNewConvo.setOnClickListener {
            createNewConvo(this, friendCodeNewConvo.text.toString()){ chat ->
                finish()
                ChatActivity.open(chat.contact, chat)
            }
        }
    }

    companion object{
        fun createNewConvo(context: Context, friendCode: String, then: ((result: Chat) -> Unit)? = null){

            val db = FirebaseFirestore.getInstance()
            db.collection("AllUsers").get()
                .addOnSuccessListener { usrs ->
                    usrs.forEach { usr ->
                        if (usr.get("friendcode").toString() == friendCode){
                            if (usr.get("id") == MainActivity.me?.id){
                                Toast.makeText(context, "You Are This User", Toast.LENGTH_SHORT).show()
                                return@addOnSuccessListener
                            }
                            for (i in MainActivity.chatList){
                                if (i.contact.id == usr.get("id")){
                                    Toast.makeText(context, "Chat Already Exists", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }
                            }
                            val chat = Chat(
                                Contact(
                                    usr.get("name").toString(),
                                    "none",
                                    usr.get("id").toString(),
                                ),
                                ArrayList()
                            )
                            MainActivity.chatList.add(chat)
                            MainActivity.messageAdapter.addChat(chat)
                            then?.invoke(chat)
                            return@addOnSuccessListener
                        }
                    }
                    Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                }
        }
    }

}