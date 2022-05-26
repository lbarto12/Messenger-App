package com.example.messenger

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.messenger.NOTIFY.MessageService
import com.example.messenger.databinding.ActivityMainBinding
import com.example.messenger.ui.dashboard.ContactFragment
import com.example.messenger.ui.home.HomeFragment
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.collections.ArrayList
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.messenger.Tools.FirestoreTools
import com.example.messenger.UTIL.*
import com.example.messenger.auth.SignInActivity
import com.example.messenger.ui.notifications.ProfileFragment
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.Timestamp
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    val TAG = "FIRESTORE"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        MainActivity.self = this
        val serviceIntent = Intent(this, MessageService::class.java)
        applicationContext.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        // and started?
        //startService(serviceIntent)

        request()
//        main()



        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)

    }




    private fun main(){

        Log.println(Log.DEBUG, "MAIN ACTIVITY", "main()")

        HomeFragment.self?.main()
        ContactFragment.self?.main()
        ProfileFragment.self?.main()

        FirestoreTools.withcollection("Texts") { texts ->
            texts.forEach { text ->
                Log.d("CALLBACK", text.get("content").toString())
            }
        }

    }


    // Stupid async methods for adding after db query
    fun addContacts(){
        for (i in contactList)
            contactAdapter.addContact(i)
    }

    fun addChats() {

        for (i in chatList){
            val comparator = kotlin.Comparator { t: Text, t2: Text ->
                return@Comparator t.time.compareTo(t2.time)
            }
            i.messages.sortWith(comparator)
        }

        for (i in chatList)
            messageAdapter.addChat(i)
    }

    // TODO: This does not work
    fun swapTo(f: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.nav_host_fragment_activity_main, f)
        transaction.commit()
    }

    // REQUEST PERMISSION

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                main()
            }

        }

    @RequiresApi(Build.VERSION_CODES.M)
    fun request(){
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                main()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.

            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.READ_CONTACTS)
            }
        }
    }
    // !REQUEST PERMISSION



    // SERVICE CONNECTION

    class SConnect : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            Log.println(Log.DEBUG, "CONNECTION", "BOUND!!!")
            val b = p1 as MessageService.MainBinder
            messageService = b.getMessageService()
            isBound = true
            //MainActivity.self?.main()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isBound = false
        }

    }

    // !SERVICE CONNECTION


    override fun onResume() {
        super.onResume()

        messageAdapter.notifyDataSetChanged()

    }

    // VARS
    companion object{
        public var self: MainActivity? = null
        var messageAdapter = MessageAdapter(mutableListOf())
        var contactAdapter = ContactAdapter(mutableListOf())


        lateinit var messageService: MessageService
        var isBound = false
        var serviceConnection = SConnect()

        var currentContact: Contact? = null
        var currentChat: Chat? = null
        var chatList = ArrayList<Chat>()

        var contactList = ArrayList<Contact>()
        var currentTextAdapter: TextAdapter? = null

        var signingIn = true

        const val HAS_NO_FRIEND_CODE = "-1"
        var myFriendCode = "-1"

        var me: Contact? = null
        var currentUser: GoogleSignInAccount? = null
    }
    // !VARS
}