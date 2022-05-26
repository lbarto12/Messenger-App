package com.example.messenger.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messenger.MainActivity
import com.example.messenger.MessagesUtil.NewConvoActivity
import com.example.messenger.R
import com.example.messenger.Tools.FirestoreTools
import kotlinx.android.synthetic.main.fragment_messages.*

class HomeFragment : Fragment(R.layout.fragment_messages) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.println(Log.DEBUG, "HOME FRAGMENT", "onCreate()")

        HomeFragment.self = this
        if (MainActivity.self != null) main()
    }


    fun main(){
        messagesRecycler.adapter = MainActivity.messageAdapter
        messagesRecycler.layoutManager = LinearLayoutManager(MainActivity.self)

        messagesSearchbar.queryHint = "Search Messages"
        messagesSearchbar.clearFocus()

        messagesSearchbar.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                MainActivity.messageAdapter.filter.filter(p0)
                return true
            }

        })

        messagesRecycler.addItemDecoration(DividerItemDecoration(messagesRecycler.context, DividerItemDecoration.VERTICAL))

        newmessage.setOnClickListener {
            val intent = Intent(MainActivity.self, NewConvoActivity::class.java)
            startActivity(intent)
        }

        //FirestoreTools.getAllChats()
    }


    companion object {
        var self: HomeFragment? = null
    }



    override fun onDestroyView() {
        super.onDestroyView()
    }
}