package com.example.messenger.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messenger.MainActivity
import com.example.messenger.R
import kotlinx.android.synthetic.main.fragment_contacts.*

class ContactFragment : Fragment(R.layout.fragment_contacts) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.println(Log.DEBUG, "CONTACT FRAGMENT", "onCreate()")
        ContactFragment.self = this
        if (MainActivity.self != null) main()
    }


    fun main(){
        contactsRecycler.adapter = MainActivity.contactAdapter
        contactsRecycler.layoutManager = LinearLayoutManager(MainActivity.self)

        contactsSearchbar.queryHint = "Search Contacts"
        contactsSearchbar.clearFocus()

        contactsSearchbar.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                MainActivity.contactAdapter.filter.filter(p0)
                return true
            }

        })

        contactsRecycler.addItemDecoration(DividerItemDecoration(contactsRecycler.context, DividerItemDecoration.VERTICAL))
    }

    companion object {
        var self: ContactFragment? = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}