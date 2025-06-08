package com.agileprobd.bloud

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.content.ContextCompat

class SmsReceiver : BroadcastReceiver()
{
    private val TAG = "SmsReceiver"
    private val PREFS_NAME = "SmsRingerPrefs"
    private val KEY_PHONE_NUMBER = "phoneNumber"
    private val KEY_PASSPHRASE = "passphrase"
    private val KEY_ACTION = "actionToDo" // "Loud" or "Play Sound"

    override fun onReceive(context: Context, intent: Intent) {
        // Check if the intent action is for SMS received
        if ("android.provider.Telephony.SMS_RECEIVED" == intent.action) {
            Log.d(TAG, "SMS_RECEIVED broadcast received.")

            // Retrieve the SMS message data
            val bundle: Bundle? = intent.extras
            if (bundle != null) {
                val pdus = bundle.get("pdus") as? Array<*>
                if (pdus == null || pdus.isEmpty()) {
                    Log.e(TAG, "PDUs are null or empty.")
                    return
                }

                val smsBodyBuilder = StringBuilder()
                var senderPhoneNumber: String? = null

                for (pdu in pdus) {
                    val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                    if (smsMessage != null) {
                        senderPhoneNumber = smsMessage.originatingAddress // Get sender's phone number
                        smsBodyBuilder.append(smsMessage.messageBody) // Get message body
                    }
                }

                val smsBody = smsBodyBuilder.toString()
                Log.d(TAG, "Incoming SMS from: $senderPhoneNumber, Body: $smsBody")

                // Load saved settings
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedPhoneNumber = prefs.getString(KEY_PHONE_NUMBER, "")
                val savedPassphrase = prefs.getString(KEY_PASSPHRASE, "")
                val savedAction = prefs.getString(KEY_ACTION, "loud")

                Log.d(TAG, "Saved settings: Phone=$savedPhoneNumber, Passphrase=$savedPassphrase, Action=$savedAction")

//                if (!savedPhoneNumber.isNullOrEmpty() && !savedPassphrase.isNullOrEmpty() &&
//                    senderPhoneNumber != null && senderPhoneNumber.contains(savedPhoneNumber) &&
//                    smsBody.contains(savedPassphrase ?: "")) { // Use safe call and Elvis operator for passphrase
                if(true) {
                    Log.d(TAG, "Matching SMS received! Changing ringer mode.")
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

                    if (audioManager != null) {
                        try {
                            when (savedAction) {

                                "Loud" -> {

                                    setLoudModeAndDisableDnd(context)

                                }
                                "Play Sound" -> {

                                    // --- NEW: Start music playback service here ---
                                    context.let {
                                        val serviceIntent = Intent(it, MusicPlaybackService::class.java).apply {
                                            action = MusicPlaybackService.ACTION_PLAY_MUSIC
                                        }
                                        // For Android 8.0 (API 26) and higher, use startForegroundService
                                        // The service then must call startForeground() within 5 seconds.
                                        ContextCompat.startForegroundService(it, serviceIntent)
                                    }
                                }
                                else -> Log.w(TAG, "Unknown action: $savedAction")
                            }
                        }

                        catch (e: Exception) {
                            Log.e(TAG, "Error changing ringer mode: ${e.message}")
                            // Catch any other general exception
                            println("An unknown error occurred: ${e.message}")
                            e.printStackTrace()
                        }

                    } else {
                        Log.e(TAG, "AudioManager is null. Cannot change ringer mode.")
                    }
                } else {
                    Log.d(TAG, "Incoming SMS did not match criteria.")
                }
            }
        }
    }

    private fun setLoudModeAndDisableDnd(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Set Ringer Mode to Normal (Loud)
        try {
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                Log.d(TAG, "Ringer mode set to NORMAL.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException changing ringer mode: ${e.message}")
            // Handle cases where you might not have permission (though less common for ringer mode)
        }


        // 2. Check for Notification Policy Access Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Log.w(TAG, "Notification Policy Access not granted. Cannot disable DND.")
                return
            }
        }

        // 3. Disable DND if it's active
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val currentFilter = notificationManager.currentInterruptionFilter
                if (currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    Log.d(TAG, "DND disabled (interruption filter set to ALL).")
                } else {
                    Log.d(TAG, "DND is already disabled or not active.")
                }
            } else {
                // For versions older than M, DND management was different and less standardized.
                // RINGER_MODE_NORMAL usually implies DND is off.
                Log.d(TAG, "Pre-M: Ringer mode normal should disable DND equivalent.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException disabling DND: ${e.message}")
            // This might happen if the permission was revoked after the check.
        }
    }
}