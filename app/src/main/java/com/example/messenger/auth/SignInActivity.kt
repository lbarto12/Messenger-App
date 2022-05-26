package com.example.messenger.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.messenger.Tools.FirestoreTools
import com.example.messenger.UTIL.Contact
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_sign_in.*

import com.example.messenger.MainActivity
import com.example.messenger.R
import com.example.messenger.auth.email.EmailSignInActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_new_convo.*


class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // WARNING: is this right?
        auth = FirebaseAuth.getInstance()

        //if (auth.currentUser != null) finish()

        signinwithgoogle.setOnClickListener {
            signInWithGoogle()
        }

        signinwithemail.setOnClickListener {
            MainActivity.self?.startActivity(Intent(MainActivity.self, EmailSignInActivity::class.java))
        }


        Thread{
            while (loggingin){
                Thread.sleep(500)
            }
            finish()

        }.start()
    }


    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    private val RC_SIGN_IN = 1

    private fun signInWithGoogle(){
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("770030920990-aq89g0ljsfvt0a7lndu12sr7cldmosqh.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val signInintent = this.googleSignInClient.signInIntent
        startActivityForResult(signInintent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("SIGN IN", "firesbaseAuthWithGoogle" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
                MainActivity.currentUser = account
            } catch (e: ApiException){
                Log.w("SING IN", "GOOGLE SIGN IN FAILED")
                Log.e("SIGN IN", e.toString())
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String){
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful){
                    Log.d("SIGNIN", "signInWithCredential:success")
                    val user = auth.currentUser
                    //Log.d("SIGNIN", MainActivity.currentUser?.displayName?: "none found")
                    //Log.d("SIGNIN", MainActivity.currentUser?.email?: "none found")
                    addGetDataBaseProfile()
                    val usr = MainActivity.currentUser ?: return@addOnCompleteListener
                    MainActivity.me = Contact(
                        usr.displayName?: "NO USER",
                        "NONE",
                        usr.id?: "NONE",
                        null,
                        usr.email?: "NONE"
                    )
                    FirestoreTools.withphoto(MainActivity.me!!.id){ image ->
                        MainActivity.me!!.photo = image
                    }

                    if (usr.id != null){
                        val db = FirebaseFirestore.getInstance()
                        db.collection("AllUsers").document(usr.id!!.toString()).get()
                            .addOnSuccessListener { r ->
                                MainActivity.myFriendCode = r.get("friendcode").toString()
                            }
                    }



                    FirestoreTools.getAllContacts()
                    loggingin = false
                }
                else {
                    // If sign in fails, display a message to the user.
                    Log.w("SIGNIN", "signInWithCredential:failure", task.exception)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
    }

    private fun addGetDataBaseProfile(){
        if (MainActivity.currentUser != null){
            val usr = MainActivity.currentUser
            val db = FirebaseFirestore.getInstance()
            FirestoreTools.withcollection("AllUsers"){ users ->
                users.forEach { doc ->
                    if (doc.get("id") == usr?.id.toString())
                        return@withcollection
                }
                FirestoreTools.withNextFriendCode { nextFriendCode ->
                    val nfc = nextFriendCode.toInt() + 1
                    FirestoreTools.put(
                        "AllUsers",
                        MainActivity.currentUser?.id?:
                        "NOIDFOUND",
                        hashMapOf(
                            "name" to usr!!.displayName!!,
                            "id" to usr.id!!,
                            "friendcode" to nextFriendCode.toString()
                        )
                    )
                    FirestoreTools.setNextFriendCode(nfc.toString())
                }
            }
        }
    }




    companion object{
        var loggingin = true
    }
    //
}