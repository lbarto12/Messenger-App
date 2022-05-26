package com.example.messenger.UTIL

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.example.messenger.ChatActivity
import com.example.messenger.MainActivity
import com.example.messenger.R
import com.example.messenger.Tools.BitmapTools
import kotlinx.android.synthetic.main.activity_messenger.view.*
import kotlinx.android.synthetic.main.message_cell.view.*
import java.util.*
import kotlin.collections.ArrayList

class MessageAdapter(
    var chats: MutableList<Chat>
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>(), Filterable {

    class MessageViewHolder(view: View): RecyclerView.ViewHolder(view)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.message_cell,
                parent,
                false
            )
        )
    }

    fun addChat(chat: Chat){

        chats.add(0, chat)
        notifyItemInserted(0)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val current = chats[position]

        holder.itemView.apply {
            // CLICK LISTENERS
            // TEXT
            // IMAGES?

            message_contact_name.text = current.contact.name
            if (current.messages.size > 0)
                recent_message.text = current.messages[current.messages.size - 1].content
            else recent_message.text = "No recent messages"
            if (current.contact.photo != null){
                contactProfilePicture.setImageBitmap(
                    BitmapTools.getCroppedBitmap(current.contact.photo!!)
                )
            }
            else contactProfilePicture.setImageResource(R.drawable.blank_contact)

            setOnClickListener{
                MainActivity.currentContact = current.contact
                MainActivity.currentChat = current
                current.hasUnread = false
                val intent = Intent(MainActivity.self, ChatActivity::class.java)
                MainActivity.self?.startActivity(intent)

            }
        }

    }

    fun setIndexToTop(position: Int){
        Collections.swap(chats, position, 0)
        notifyItemMoved(position, 0)
    }

    override fun getItemCount(): Int {
        return chats.size
    }

    val filteredChats = chats

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(p0: CharSequence?): FilterResults {
                val results = FilterResults()
                if (p0 == null || p0.isEmpty()){
                    results.count = filteredChats.size
                    results.values = filteredChats
                }
                else {
                    val searchResult = p0.toString().lowercase()
                    val chatItems = ArrayList<Chat>()
                    for (chat in filteredChats){
                        if (searchResult in chat.contact.name.lowercase()){
                            chatItems.add(chat)
                        }
                    }
                    results.count = chatItems.size
                    results.values = chatItems
                }
                return results
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                chats = p1!!.values as MutableList<Chat>
                notifyDataSetChanged()
            }

        }
    }

}