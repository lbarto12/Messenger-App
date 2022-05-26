package com.example.messenger.Tools

import android.annotation.SuppressLint
import android.content.ContentUris
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import com.example.messenger.ChatActivity
import com.example.messenger.MainActivity
import com.example.messenger.UTIL.Chat
import com.example.messenger.UTIL.Contact
import com.example.messenger.UTIL.Text
import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException
import java.lang.Exception

class FirestoreTools {
    companion object {

        const val TAG = "FIRESTORE"

        // gets contacts from db and phone

        var totalContacts = 0
        var runningContacts = 0
        var contactsDone = false

        fun getAllContacts() {

            val contacts = ArrayList<Contact>()
            MainActivity.contactList = retrieveContacts()
            withcollection("AllUsers"){ allContacts ->
                totalContacts = allContacts.size()
                allContacts.forEach { contact ->
                    withphoto(contact.get("id").toString()){ photo ->
                        Log.d("CADD", "ENTERED" + contact.get("id"))
                        var newphoto: Bitmap? = null
                        if (photo != null){
                            newphoto = BitmapTools.getCroppedBitmap(photo)
                        }
                        MainActivity.contactList.add(
                            Contact(
                                contact.get("name").toString(),
                                contact.get("number").toString(),
                                contact.get("id").toString(),
                                newphoto,
                                null,
                                contact.get("friendcode").toString()
                            )
                        )
                        runningContacts++
                        explicitAddContacts()
                        Log.d("CADD", "EXIT" + contact.get("id"))
                    }
                }
            }
        }
        private fun explicitAddContacts(){
            if (totalContacts != runningContacts) return
            MainActivity.self?.addContacts()
            FirestoreTools.start()
        }

        //gets contacts from phone
        @SuppressLint("Range")
        fun retrieveContacts(): ArrayList<Contact> {
            val contacts = ArrayList<Contact>()
            var cursor: Cursor? = null


            try {
                cursor = MainActivity.self?.contentResolver?.query(
                    ContactsContract.Contacts.CONTENT_URI, null, null, null, null
                )
            }
            catch (e: Exception){
                Log.e("ERROR ADDING CONTACT", e.message!!)
            }

            if (cursor != null && cursor.count > 0){
                while (cursor.moveToNext()){
                    val contact_id = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    )
                    val contact_name = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    )

                    val temp = Array<String>(1){
                        contact_id
                    }

                    var phoneNumber: String? = null
                    if (cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)).toInt() > 0){
                        val numCursor: Cursor? = MainActivity.self?.contentResolver?.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            temp as Array<out String>?,
                            null
                        )

                        if (numCursor != null){
                            while (numCursor.moveToNext()){
                                phoneNumber = numCursor.getString(numCursor.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER))
                            }
                            numCursor.close()
                        }


                    }

                    var image: Bitmap? = null
                    try {
                        val istream = ContactsContract.Contacts.openContactPhotoInputStream(
                            MainActivity.self?.contentResolver,
                            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact_id.toLong())
                        )

                        if (istream != null){
                            image = BitmapFactory.decodeStream(istream)
                        }
                    }
                    catch (ex: IOException){
                        ex.printStackTrace()
                    }


                    contacts.add(
                        Contact(
                            contact_name,
                            phoneNumber?: "0",
                            contact_id,
                            image
                        )
                    )
                }
            }
            cursor?.close()
            return contacts
        }

        // Set a firebase value
        fun <K, V> put(collection: String, doc: String, hashMap: HashMap<K, V>) {
            val start = System.currentTimeMillis()
            val db = FirebaseFirestore.getInstance()
            db.collection(collection).document(doc)
                .set(hashMap)
                .addOnSuccessListener {
                    val end = System.currentTimeMillis()
                    val totaltime = end - start;
                    Toast.makeText(MainActivity.self, "Data uploaded in $totaltime ms", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { Log.d("FIREBASE", "DATA NOT WRITTEN") }
        }

        // I don't think this works
        fun subTagValInCollection(collection: String, tag: String, value: String): Boolean{
            val db = FirebaseFirestore.getInstance()

            var isin = false

            db.collection(collection).get()
                .addOnSuccessListener { col ->
                    col.forEach { doc ->
                        if (doc.get(tag) == value.toString())
                            isin = true
                    }
                }

            return isin
        }

        // Upload a string from a text to the db
        fun uploadText(c1: Contact, c2: Contact, content: String){
            withTextStoreId { currentTextCode ->
                val newtextcode = createNextTextId(currentTextCode)

                put("Texts", MainActivity.me!!.name + newtextcode.toString(),
                    hashMapOf(
                        "from" to c1.id,
                        "to" to c2.id,
                        "content" to content,
                        "time" to Timestamp.now()
                    )
                )

                setNextTextCode(newtextcode)
                sendPending(c2, MainActivity.me!!.name + newtextcode.toString())
            }
        }

        // Upload a text object to the db
        fun uploadText(c1: Contact, c2: Contact, text: Text){
            withTextStoreId { currentTextCode ->
                val newtextcode = createNextTextId(currentTextCode)
                val map = hashMapOf(
                    "from" to c1.id,
                    "to" to c2.id,
                    "content" to text.content,
                    "time" to text.time,
                )
                if (text.reference != null) map.put(
                    "photourl", text.reference
                )

                put("Texts", newtextcode, map)

                setNextTextCode(newtextcode)
                sendPending(c2, newtextcode)

                if (MainActivity.currentChat != null){
                    MainActivity.currentChat!!.messages.add(text)
                }

            }
        }

        // create the next textcode
        fun createNextTextId(currentTextCode: String): String {
            val trailingChar = currentTextCode[currentTextCode.length - 1].code + 1

            var newtextcode = currentTextCode.substring(0, currentTextCode.length - 1) + trailingChar.toChar()

            // handle end of codes of current length
            if (trailingChar.toChar() == 'z'){
                val rev = newtextcode.reversed().toCollection(ArrayList())
                for ((iter, char) in rev.withIndex()){
                    if (char == 'z'){
                        rev[iter] = 'a'
                        if (iter + 1 < rev.size){
                            val code = rev[iter + 1].code
                            rev[iter + 1] = (code + 1).toChar()
                        }
                        else{
                            rev.add('a')
                        }
                    }
                }
                newtextcode = rev.reversed().joinToString("")
            }
            return newtextcode
        }

        // find a contact in this users contacts
        fun getContactById(id: String): Contact{
            for (contact in MainActivity.contactList){
                if (contact.id == id) return contact
            }
            return Contact("NONE", "", "")
        }


        var gettingsize = -1
        var runningsize = 0
        val alltexts = ArrayList<Pair<Text, String>>()
        val allchats = ArrayList<Chat>()
        var done = false

        // Literally just a wrapper for a chain of events that starts the whole thing,
        // since start is more meaningful than deletePending... and things need to
        // happen only after it happens
        private fun start(){
            deletePendingOnStartup()
        }

        // Loads all the chats into the inital state of the app
        private fun getAllChats() {
            val usr = MainActivity.me?: return

            withcollection("Texts"){ texts ->
                gettingsize = texts.size()
                texts.forEach { text ->
                    if (isEither(usr.id, text.get("from").toString(), text.get("to").toString())){
                        val content = text.get("content").toString()
                        val newtext = Text(
                            text.get("content").toString(),
                            text.get("from") == usr.id,
                            text.get("time") as Timestamp
                        )
                        val cont = if (text.get("from") == usr.id) text.get("to").toString() else text.get("from").toString()

                        if (text.contains("photourl")){
                            withphoto(text.get("photourl")!!){ image ->
                                if (image == null) return@withphoto
                                newtext.photo = image
                                alltexts.add( Pair(newtext, cont))
                                runningsize += 1
                                explicitCreateChats(alltexts, allchats)
                            }
                        }
                        else{
                            alltexts.add(Pair(newtext, cont))
                            runningsize += 1
                            explicitCreateChats(alltexts, allchats)
                        }
                    }
                    else {
                        runningsize += 1
                        explicitCreateChats(alltexts, allchats)
                    }
                }
            }

        }

        // this is here to remove duplicate code in 'getAllChats' due to asynchronicity
        private fun explicitCreateChats(alltexts: ArrayList<Pair<Text, String>>, allchats: ArrayList<Chat>){
            if (runningsize != gettingsize) return
            for (i in alltexts) {
                var alreadyin = false
                for (j in allchats){
                    if (i.second == j.contact.id){
                        alreadyin = true
                    }
                }
                if (!alreadyin){
                    val x = getContactById(i.second)
                    allchats.add(
                        Chat(
                            getContactById(i.second),
                            ArrayList()
                        )
                    )
                }

                for (k in allchats){
                    if (i.second == k.contact.id){
                        k.messages.add(i.first)
                    }
                }

            }

            MainActivity.chatList = allchats
            MainActivity.self?.addChats()
        }

        // helper function for seeing if a text applies to you / other contact
        fun isEither(target: String, first: String, second: String): Boolean{
            return target == first || target == second
        }

        // returns true if the id is the same as the user's id
        fun isMe(id: String): Boolean {
            return id == MainActivity.me!!.id
        }

        // Sends a pending text reference to the 'Pending' collection, used for realtime
        // updates
        fun sendPending(to: Contact, textid: String) {
            val docname = to.id + MainActivity.me!!.id + textid
            put("Pending", docname, hashMapOf(
                "from" to  MainActivity.me!!.id,
                "to" to to.id,
                "index" to textid,
                "time" to Timestamp.now()
            ) )
        }

        // Deletes pending texts waiting for user to prevent duplicates, since all texts
        // will be checked on startup
        fun deletePendingOnStartup(){
            withcollection("Pending"){ collection ->
                collection.forEach { pending ->
                    if (MainActivity.me!!.id == pending.get("to").toString()){
                        delDoc("Pending", pending.id)
                    }
                }
                getAllChats()
            }
        }

        // This runs in a thread elsewhere to constantly scan for new texts pertaining to
        // the current user, and adds them to the respective chat
        fun scanAndAddToRespectiveChat(){
            if (runningsize != gettingsize) return

            val db = FirebaseFirestore.getInstance()
            db.collection("Pending").get()
                .addOnSuccessListener { collection ->
                    if (MainActivity.self == null || MainActivity.me == null) return@addOnSuccessListener
                    collection.forEach { pending ->
                        if (MainActivity.me!!.id == pending.get("to").toString()) {
                            val textCode = pending.get("index").toString()
                            db.collection("Texts").document(textCode).get()
                                .addOnSuccessListener { text ->
                                    val content = text.get("content").toString()

                                    var storageRef: StorageReference? = null
                                    var storedImage: Bitmap? = null

                                    delDoc("Pending", pending.id)

                                    if (text.contains("photourl")){
                                        storageRef = FirebaseStorage
                                            .getInstance()
                                            .reference
                                            .child("Image Files/${text.get("photourl")}")
                                        storageRef.getBytes(1024 * 1024)
                                            .addOnCompleteListener { barr ->
                                                storedImage = BitmapFactory
                                                    .decodeByteArray(barr.result, 0, barr.result.size)

                                                explicitAddText(text, pending.id, storedImage)
                                            }
                                    }
                                    else{
                                        explicitAddText(text, pending.id)
                                    }

                                }
                        }
                    }
                }
        }

        // Same thing as 'explicitCreateChats' but just for adding in the above thread function
        // Accounting for async
        private fun explicitAddText(text: DocumentSnapshot, docid: String, image: Bitmap? = null){
            val content = text.get("content").toString()

            val addtext = Text(
                content,
                isMe(text.get("from").toString()),
                text.get("time") as Timestamp,
                image,
                text.get("reference").toString()
            )

            withvalue("AllUsers", text.get("from").toString(), "name") { name ->
                withvalue("AllUsers", text.get("from").toString(), "id") { id ->
                    if (ChatActivity.self == null ||
                        MainActivity.currentChat?.contact?.id != id.toString()){
                        MainActivity.messageService.sendChannel1(content, name.toString() )
                    }
                }


            }

            var found = false
            for (i in MainActivity.messageAdapter.chats){
                if (i.contact.id == text.get("from").toString()){
                    found = true
                    i.hasUnread = true
                    i.messages.add(addtext)
                    if (MainActivity.currentContact != null &&
                        i.contact.id == MainActivity.currentContact!!.id &&
                        MainActivity.currentTextAdapter != null){
                        MainActivity.currentTextAdapter!!.addText(addtext)
                    }

                    delDoc("Pending", docid)
                }
            }

            if (!found){
                withcollection("AllUsers") { users ->
                    users.forEach { user ->
                        if (user.get("id").toString() == text.get("from").toString()){
                            val contact = Contact(
                                user.get("name").toString(),
                                "null",
                                user.get("id").toString()
                            )
                            val chat = Chat(contact, ArrayList())
                            chat.messages.add(addtext)
                            MainActivity.messageAdapter.addChat(chat)
                            delDoc("Pending", docid)
                        }
                    }
                }

            }
        }

        // helper function to delect a document from firebase
        fun delDoc(collection: String, docid: String){
            val db = FirebaseFirestore.getInstance()
            db.collection(collection).document(docid)
                .delete()
        }

        // Callback -> returns specified collection
        fun withcollection(collection: String, callback: (result: QuerySnapshot) -> Unit) {
            FirebaseFirestore.getInstance().collection(collection).get()
                .addOnSuccessListener(callback)
        }

        // Callback -> returns specified document
        fun withdocument(collection: String, document: String, callback: (result: DocumentSnapshot) -> Unit) {
            FirebaseFirestore.getInstance()
                .collection(collection)
                .document(document)
                .get()
                .addOnSuccessListener(callback)
        }

        // Callback -> returns specified value
        fun withvalue(collection: String, document: String, field: String, callback: (result: Any?) -> Unit) {
            FirebaseFirestore.getInstance()
                .collection(collection)
                .document(document)
                .get()
                .addOnSuccessListener { doc ->
                    callback.invoke(doc.get(field))
                }
        }

        // Callback -> returns textstoreid
        fun withTextStoreId(callback: (result: String) -> Unit) {
            FirebaseFirestore.getInstance()
                .collection("Config")
                .document("constants")
                .get()
                .addOnSuccessListener { callback.invoke(it.get("textstoreid").toString()) }
        }

        // Callback -> returns nextfriendcode
        fun withNextFriendCode(callback: (result: String) -> Unit) {
            FirebaseFirestore.getInstance()
                .collection("Config")
                .document("consttwo")
                .get()
                .addOnSuccessListener{callback.invoke(it.get("nextfriendcode").toString())}
        }

        // Callback -> returns photo from storage
        fun withphoto(url: Any, callback: (result: Bitmap?) -> Unit){
            val storageRef = FirebaseStorage
                .getInstance()
                .reference
                .child("Image Files/${url}")
            storageRef.getBytes((1024 * 1024).toLong())
                .addOnCompleteListener{ ba ->
                    try{
                        val final = BitmapFactory
                            .decodeByteArray(ba.result, 0, ba.result.size)
                        callback.invoke(final)
                    } catch(e: Exception){
                        callback.invoke(null)
                    }

                }
        }

        // Sets textstoreid
        fun setNextTextCode(value: String) {
            val db = FirebaseFirestore.getInstance()
            db.collection("Config").document("constants").set(
                hashMapOf("textstoreid" to value)
            )
        }

        // Sets nextfriendcode
        fun setNextFriendCode(value: String){
            val db = FirebaseFirestore.getInstance()
            db.collection("Config").document("consttwo").set(
                hashMapOf("nextfriendcode" to value)
            )
        }
    }





}