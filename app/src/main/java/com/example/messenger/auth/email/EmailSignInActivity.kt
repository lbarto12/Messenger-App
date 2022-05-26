package com.example.messenger.auth.email

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.messenger.MainActivity
import com.example.messenger.R
import com.example.messenger.Tools.FirestoreTools
import com.example.messenger.UTIL.Contact
import com.example.messenger.auth.SignInActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_email_sign_in.*

class EmailSignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_sign_in)


        auth = FirebaseAuth.getInstance()


        // MAKE FIELDS + COMPLETE BUTTON

        emailEditTextLogin.hint = "Email"
        passwordEditTextLogin.hint = "Password"

        signUpConfirmButton.setOnClickListener {
            val email = emailEditTextLogin.text.toString()
            val pass = passwordEditTextLogin.text.toString()
            createAccount(email, pass)
            signInWithEmail(email, pass)
        }
    }

    fun createAccount(email: String, password: String){
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("CREATE ACCOUNT", "createUserWithEmail:success")
                    val user = auth.currentUser
                    val usrname = user?.displayName?: user?.email?: "NONE"
                    addEmailUser(user, email, password)
                }
            }

    }

    fun addEmailUser(usr: FirebaseUser?, email: String, pass:String){
        if (usr == null) return
        if (FirestoreTools.subTagValInCollection(
                "AllUsers",
                "id",
                usr.email.toString())
        ) return
        FirestoreTools.withcollection("AllUsers"){ users ->
            users.forEach { doc ->
                if (doc.get("id") == usr.email)
                    return@withcollection
            }
            FirestoreTools.withNextFriendCode { nextFriendCode ->
                val nfc = nextFriendCode.toInt() + 1
                FirestoreTools.put(
                    "AllUsers",
                    usr.email?: "NOIDFOUND",
                    hashMapOf(
                        "name" to usr.email!!,
                        "id" to usr.email!!,
                        "friendcode" to nfc.toString()
                    )
                )
                FirestoreTools.setNextFriendCode(nextFriendCode.toString())
            }

        }

    }

    fun signInWithEmail(email: String, password: String){
        Log.d("EMJ", "$email $password")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener{ task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val id = user?.email?:"NULL"

                        FirestoreTools.withdocument("AllUsers", id){ doc ->
                            if (doc.get("id").toString() == id){
                                Log.d("DATABASE", "FOUND ${doc.get("id").toString()}")
                                FirestoreTools.withphoto(id){ image ->
                                    MainActivity.me = Contact(
                                        doc.get("name").toString(),
                                        "NONE",
                                        doc.get("id").toString(),
                                        image
                                    )
                                    MainActivity.myFriendCode = doc.get("friendcode").toString()

                                    FirestoreTools.getAllContacts()
                                    SignInActivity.loggingin = false
                                    Log.d("SIGNINID", MainActivity.me?.name?: "CANNOT BE FOUND")

                                    finish()
                                }
                            }
                        }

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("SIGN IN", "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            this, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
    }

    private lateinit var auth: FirebaseAuth
}