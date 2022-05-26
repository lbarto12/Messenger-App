package com.example.messenger.NOTIFY

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.messenger.MainActivity
import com.example.messenger.R
import com.example.messenger.Tools.FirestoreTools
import com.example.messenger.UTIL.MessageAdapter

class MessageService : Service() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        binder = MainBinder()
        MessageService.self = this

        createNotificationChannel()
        initGetThread()
    }


    // BINDER
    inner class MainBinder : Binder() {
        fun getMessageService(): MessageService {
            return this@MessageService
        }
    }

    override fun onBind(p0: Intent?): IBinder {
        return binder;
    }
    // !BINDER


    // GET NEW NOTIFICATIONS

    fun initGetThread(){
        Thread{
            while (running){

                FirestoreTools.scanAndAddToRespectiveChat()
                Log.d("SCANNING", "DONE")
                Thread.sleep(500)
            }
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(){
        val channel = NotificationChannel(
            RECEIVE_TEXT_CHANNEL,
            RECEIVE_TEXT_CHANNEL,
            NotificationManager.IMPORTANCE_HIGH)

        notificationManager = MainActivity.self!!.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d("MESSAGESERVICE", "CHANNEL CREATED")
    }

    fun sendChannel1(content: String, title: String){
        Log.d("MESSAGESERVICE", "NOTIFICATION SENDING")

        val intent = Intent(MainActivity.self, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT)


        val notification = androidx.core.app.NotificationCompat.Builder(this, RECEIVE_TEXT_CHANNEL)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            this.notify(nextNotificiationId, notification)
        }
        nextNotificiationId++
        //notificationManager.notify(1, notification)
        //startForeground(1, notification)
    }
    // !GET NEW NOTIFICATIONS


    override fun onDestroy() {
        super.onDestroy()
        running = false
    }

    // VARS
    private lateinit var binder: MainBinder

    companion object {
        lateinit var self: MessageService
        var running = true

        lateinit var notificationManager: NotificationManager
        val RECEIVE_TEXT_CHANNEL = "messengerChannelReceiveText"
        var nextNotificiationId = 0
    }
    // !VARS

}