package com.example.messenger.UTIL

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.messenger.ChatActivity
import com.example.messenger.R
import kotlinx.android.synthetic.main.image_cell_from_other.view.*
import kotlinx.android.synthetic.main.image_cell_from_user.view.*
import kotlinx.android.synthetic.main.text_cell_from_user.view.*
import kotlinx.android.synthetic.main.text_cell_from_other.view.*

class TextAdapter(
    private var texts: MutableList<Text>
):  RecyclerView.Adapter<TextAdapter.TextViewHolder>() {


    class TextViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private var currentindexforviewholder = 0

    fun addText(text: Text){
        Log.println(Log.DEBUG, "TEXT ADAPTER", "ADDED")
        texts.add(text)
        notifyItemInserted(texts.size - 1)
        ChatActivity.shouldScrollToBottom = true
    }

    override fun getItemViewType(position: Int): Int {
        var by = if (texts[position].sentByMe) 0 else 1
        if (texts[position].photo != null){
            by += 2
        }
        return by
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextViewHolder {
        Log.d("VIEWHOLDER", "$viewType")
        val layout = when (viewType) {
            0 -> R.layout.text_cell_from_user
            1 -> R.layout.text_cell_from_other
            2 -> R.layout.image_cell_from_user
            3 -> R.layout.image_cell_from_other
            else -> null
        }

        Log.println(Log.DEBUG, "TEXT ADAPTER", "CREATED")
        return TextAdapter.TextViewHolder(
            LayoutInflater.from(parent.context).inflate(
                layout!!,
                parent,
                false
            ),
        )
    }

    override fun onBindViewHolder(holder: TextViewHolder, position: Int) {
        Log.println(Log.DEBUG, "TEXT ADAPTER", "BOUND")

        val current = texts[position]

        holder.itemView.apply {
            if (current.sentByMe){
                if (current.photo != null)
                    image_message_holder_user.setImageBitmap(current.photo)
                else
                    text_message_holder_user.text = current.content
            }
            else{
                if (current.photo != null)
                    image_message_holder_other.setImageBitmap(current.photo)
                else
                    text_message_holder_other.text = current.content
            }
        }
        if (ChatActivity.shouldScrollToBottom){
            ChatActivity.self?.scrollRecyclerViewToBottom()
            ChatActivity.shouldScrollToBottom = false
        }
    }

    override fun getItemCount(): Int {
        return texts.size
    }


}