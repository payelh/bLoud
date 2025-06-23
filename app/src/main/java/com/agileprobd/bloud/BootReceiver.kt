package com.agileprobd.bloud

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.content.ComponentName
import android.content.pm.PackageManager // Import PackageManager
import android.widget.Toast
class BootReceiver : BroadcastReceiver() {

    private val TAG = javaClass.simpleName //use class name as tag

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
                Log.d(TAG, "Device has booted, re-enabling SmsReceiver and starting service.")
                // It's good practice to ensure the main SMS receiver is enabled
                val smsReceiverComponent = ComponentName(it, SmsReceiver::class.java)
                it.packageManager.setComponentEnabledSetting(
                    smsReceiverComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                // Optional: If your MusicPlaybackService needs to start immediately after boot
                // without waiting for an SMS (e.g., if it also plays background music proactively),
                // you could start it here. However, for an SMS Ringer app, the SMS_RECEIVED
                // broadcast will usually trigger the service when needed.
                // For this app's primary function, the SMS_RECEIVED receiver is the main entry point.

                // Example of starting the music service after boot (only if relevant to your app's core design)
                // val musicServiceIntent = Intent(it, MusicPlaybackService::class.java)
                // ContextCompat.startForegroundService(it, musicServiceIntent)
                // Log.d(TAG, "MusicPlaybackService started after boot (if required).")

                Toast.makeText(it, "SmsRingerControl ready after boot.", Toast.LENGTH_LONG).show() // For testing
            }
        }
    }
}