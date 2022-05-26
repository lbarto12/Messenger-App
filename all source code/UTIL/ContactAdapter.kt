package com.example.messenger.UTIL

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.example.messenger.ChatActivity
import com.example.messenger.MainActivity
import com.example.messenger.MessagesUtil.NewConvoActivity
import com.example.messenger.R
import com.example.messenger.Tools.FirestoreTools
import com.example.messenger.ui.dashboard.ContactFragment
import com.example.messenger.ui.home.HomeFragment
import kotlinx.android.synthetic.main.contact_cell.view.*

class ContactAdapter (
    var contacts: MutableList<Contact>
):  RecyclerView.Adapter<ContactAdapter.ContactViewHolder>(), Filterable  {

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactAdapter.ContactViewHolder {
        return ContactAdapter.ContactViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.contact_cell,
                parent,
                false
            )
        )
    }

    fun addContact(c: Contact){
        contacts.add(c)
        notifyItemInserted(contacts.size - 1)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val current = contacts[position]

        holder.itemView.apply {
            // CLICK LISTENERS
            // TEXT
            // IMAGES?

            contact_name.text = current.name
            if (current.photo != null) contact_picture.setImageBitmap(current.photo)
            else contact_picture.setImageResource(R.drawable.blank_contact)
            number.text = current.friendcode

            setOnClickListener{
                FirestoreTools.withvalue("AllUsers", current.id, "friendcode"){ code ->
                    if (code == null) return@withvalue
                    NewConvoActivity.createNewConvo(
                        MainActivity.self!!,
                        code.toString()){ chat ->
                        ChatActivity.open(chat.contact, chat)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    val filteredContacts = contacts

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(p0: CharSequence?): FilterResults {
                val results = FilterResults()
                if (p0 == null || p0.isEmpty()){
                    results.count = filteredContacts.size
                    results.values = filteredContacts
                }
                else {
                    val searchResult = p0.toString().lowercase()
                    val contactItems = ArrayList<Contact>()
                    for (contact in filteredContacts){
                        if (searchResult in contact.name.lowercase()){
                            contactItems.add(contact)
                        }
                    }
                    results.count = contactItems.size
                    results.values = contactItems
                }
                return results
            }

            override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
                contacts = p1!!.values as MutableList<Contact>
                notifyDataSetChanged()
            }

        }
    }


}