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
    private val TAG = javaClass.simpleName //use class name as tag
    private val PREFS_NAME = "SmsRingerPrefs"
    private val KEY_PHONE_NUMBER = "phoneNumber"
    private val KEY_PASSPHRASE = "passphrase"

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

                Log.d(TAG, "Saved settings: Phone=$savedPhoneNumber, Passphrase=$savedPassphrase")

                if (!savedPhoneNumber.isNullOrEmpty() && !savedPassphrase.isNullOrEmpty() &&
                    senderPhoneNumber != null && senderPhoneNumber.contains(savedPhoneNumber) &&
                   smsBody.contains(savedPassphrase ?: "")) { // Use safe call and Elvis operator for passphrase

                    Log.d(TAG, "Matching SMS received! Changing ringer mode.")
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

                    if (audioManager != null) {
                        try {
                            setLoudAndDiableDND(context)
                        }

                        catch (e: Exception) {
                            Log.e(TAG, "Error changing ringer mode: ${e.message}")
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

    private fun playAudio(context: Context) {
        // Wait some and request playback service
        Thread.sleep(5000)
        context.let {
            val serviceIntent = Intent(it, MusicPlaybackService::class.java).apply {
                action = MusicPlaybackService.ACTION_PLAY_MUSIC
            }
            // For Android 8.0 (API 26) and higher, use startForegroundService
            // The service then must call startForeground() within 5 seconds.
            ContextCompat.startForegroundService(it, serviceIntent)
        }
    }

    private fun setLoudAndDiableDND(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // 1. Set Ringer Mode to Normal (Loud)
        setRingerToLoud(audioManager)

        //2. If failed to disable DND, then play music
        if(!tryDisableDnd(context)) {
            playAudio(context)
        }
    }

    private fun setRingerToLoud(audioManager: AudioManager) {
        try {
            if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                Log.d(TAG, "Ringer mode set to NORMAL.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException changing ringer mode: ${e.message}")
            // Handle cases where you might not have permission (though less common for ringer mode)
        }
    }

    private fun tryDisableDnd(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Check for Notification Policy Access Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Log.w(TAG, "Notification Policy Access not granted. Cannot disable DND.")
                return false
            }
        }

        // Disable DND if it's active
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val currentFilter = notificationManager.currentInterruptionFilter
                if (currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                    //Disable DND
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)

                    //check if still DND is disabled
                    if (currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                        Log.d(TAG, "DND is still disabled.")
                        return false
                    }
                    else {
                        Log.d(TAG, "DND is successfully enabled.")
                        return true
                    }

                } else {
                    Log.d(TAG, "DND is already disabled or not active.")
                    return true
                }
            } else {
                // For versions older than M, DND management was different and less standardized.
                // RINGER_MODE_NORMAL usually implies DND is off.
                Log.d(TAG, "Pre-M: Ringer mode normal should disable DND equivalent.")
                return true
            }
        } catch (e: SecurityException) {
            // This might happen if the permission was revoked after the check.
            Log.e(TAG, "SecurityException disabling DND: ${e.message}")
            return false
        }
    }
}